#!/usr/bin/env python3
"""Static verification for V13 Gradle module extraction."""
from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

from project_layout import MODULES, ROOT

EXPECTED = [
    "core/model", "core/common", "core/database", "core/network",
    "core/observability", "core/session", "core/sync", "core/designsystem",
    "core/platform", "feature/auth", "feature/chat", "feature/notifications",
    "feature/commission", "feature/balance", "feature/profile",
]

checks: list[tuple[str, bool, str]] = []

def check(name: str, condition: bool, detail: str = "") -> None:
    checks.append((name, condition, detail))
    print(("PASS" if condition else "FAIL") + f": {name}" + (f" — {detail}" if detail and not condition else ""))


def project_dependencies(module: str) -> set[str]:
    script = MODULES[module] / "build.gradle.kts"
    if not script.is_file():
        return set()
    text = script.read_text(encoding="utf-8")
    return {name.lstrip(":").replace(":", "/") for name in re.findall(r'project\("(:[^"]+)"\)', text)}

# Every target module is declared and owns an isolated script/source root.
for module in EXPECTED:
    check(f"settings declares {module}", module in MODULES)
    check(f"{module} build script exists", (ROOT/module/"build.gradle.kts").is_file())
    check(f"{module} source root exists", (ROOT/module/"src/main/kotlin").is_dir())

all_declared = set(MODULES)
graph = {module: project_dependencies(module) for module in all_declared}
unknown = sorted((owner, dep) for owner,deps in graph.items() for dep in deps if dep not in all_declared)
check("all project dependencies reference declared modules", not unknown, str(unknown[:10]))

# Directed graph is acyclic.
cycles: list[list[str]] = []
def visit(node: str, path: list[str]) -> None:
    if node in path:
        cycles.append(path[path.index(node):] + [node]); return
    for nxt in graph.get(node, set()):
        visit(nxt, path + [node])
for node in graph:
    visit(node, [])
canon=set()
for cycle in cycles:
    body=cycle[:-1]
    if not body: continue
    rotations=[tuple(body[i:]+body[:i]) for i in range(len(body))]
    canon.add(min(rotations))
check("Gradle module graph is acyclic", not canon, str(sorted(canon)))

# Package declaration and source path alignment.
package_errors=[]
package_owner: dict[str, str] = {}
all_sources=[]
for module,path in MODULES.items():
    src=path/"src/main/kotlin"
    if not src.is_dir(): continue
    for file in src.rglob("*.kt"):
        all_sources.append((module,src,file))
        text=file.read_text(encoding="utf-8", errors="replace")
        match=re.search(r'^package\s+([\w.]+)', text, re.M)
        if not match:
            package_errors.append(f"{file.relative_to(ROOT)} missing package"); continue
        pkg=match.group(1)
        expected='.'.join(file.relative_to(src).with_suffix('').parts[:-1])
        if pkg != expected:
            package_errors.append(f"{file.relative_to(ROOT)}: {pkg} != {expected}")
        package_owner[pkg]=module
check(f"package paths agree ({len(all_sources)} production files)", not package_errors, '; '.join(package_errors[:10]))

# No same package/file path appears in multiple modules.
relative_owners=defaultdict(list)
for module,src,file in all_sources:
    relative_owners[file.relative_to(src).as_posix()].append(module)
duplicates={path: owners for path,owners in relative_owners.items() if len(owners)>1}
check("no duplicate project declarations across modules", not duplicates, str(list(duplicates.items())[:10]))

# Old roots are gone from app.
app_package=ROOT/"app/src/main/kotlin/com/autodrive/app"
legacy_present=[name for name in ("core", "feature/auth", "feature/chat", "feature/notifications", "feature/commission", "feature/balance", "feature/profile") if (app_package/name).exists()]
check("extracted source roots removed from app module", not legacy_present, str(legacy_present))

# Core and feature build dependencies obey directions.
core_to_feature=sorted((m,d) for m,deps in graph.items() if m.startswith("core/") for d in deps if d.startswith("feature/"))
check("core modules do not depend on features", not core_to_feature, str(core_to_feature))
module_to_app=sorted((m,d) for m,deps in graph.items() if m!="app" for d in deps if d=="app")
check("libraries never depend on app", not module_to_app, str(module_to_app))

# Import boundaries and direct dependency coverage.
MODULE_PACKAGE_PREFIXES = {
    module: ("com.autodrive.app" if module == "app" else "com.autodrive.app." + module.replace("/", "."))
    for module in MODULES
}

def target_owner(import_name: str) -> str | None:
    if not import_name.startswith("com.autodrive.app"):
        return None
    # Module namespaces also own generated R and BuildConfig symbols.
    candidates = [
        (prefix, module)
        for module, prefix in MODULE_PACKAGE_PREFIXES.items()
        if import_name == prefix or import_name.startswith(prefix + ".")
    ]
    if candidates:
        return max(candidates, key=lambda item: len(item[0]))[1]
    # Fall back to declared source packages.
    probe=import_name
    while probe.startswith("com.autodrive.app"):
        if probe in package_owner: return package_owner[probe]
        if '.' not in probe: break
        probe=probe.rsplit('.',1)[0]
    return None

uncovered=[]
app_imports=[]
cross_feature=[]
for module,src,file in all_sources:
    text=file.read_text(encoding="utf-8", errors="replace")
    for match in re.finditer(r'^import\s+(com\.autodrive\.app\.[\w.*]+)', text, re.M):
        imported=match.group(1).rstrip('.*')
        owner=target_owner(imported)
        if owner is None or owner==module: continue
        if owner=="app" and module!="app":
            app_imports.append(f"{file.relative_to(ROOT)} -> {imported}")
        if owner not in graph.get(module,set()):
            uncovered.append(f"{module}: {file.relative_to(ROOT)} -> {owner} ({imported})")
        if module.startswith("feature/") and owner.startswith("feature/"):
            own_feature=module.split('/',1)[1]
            target_feature=owner.split('/',1)[1]
            marker=f"com.autodrive.app.feature.{target_feature}."
            suffix=imported.split(marker,1)[1] if marker in imported else ""
            if own_feature!=target_feature and not suffix.startswith("domain."):
                cross_feature.append(f"{file.relative_to(ROOT)} -> {imported}")
check("extracted modules do not import app-owned packages", not app_imports, '; '.join(app_imports[:10]))
check("source imports are covered by direct Gradle dependencies", not uncovered, '; '.join(uncovered[:10]))
check("cross-feature imports use domain contracts only", not cross_feature, '; '.join(cross_feature[:10]))

# Resource ownership follows the Session 06 Design System architecture.
resource_owners=[]
for module,path in MODULES.items():
    res=path/"src/main/res"
    if res.is_dir() and any(p.is_file() for p in res.rglob('*')):
        resource_owners.append(module)
expected_resource_owners={"app", "core/designsystem", "feature/auth"}
check("Android resources follow Session 06 owners", set(resource_owners)==expected_resource_owners, str(resource_owners))
check("design system retains shared typography resources", (ROOT/"core/designsystem/src/main/res/font/tajawal_regular.ttf").is_file())
check("app owns launcher resources", (ROOT/"app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").is_file())
check("auth owns login hero", (ROOT/"feature/auth/src/main/res/drawable/login_hero.png").is_file())

# App R may only be referenced by the app module; extracted modules use their own generated R.
wrong_r=[]
for module,src,file in all_sources:
    text=file.read_text(encoding="utf-8", errors="replace")
    if module != "app" and 'import com.autodrive.app.R' in text:
        wrong_r.append(str(file.relative_to(ROOT)))
check("extracted modules do not import app R", not wrong_r, str(wrong_r[:10]))

# App composes every extracted module directly.
app_dependencies=graph.get("app",set())
missing_from_app=sorted(set(EXPECTED)-app_dependencies)
check("app composes every extracted module", not missing_from_app, str(missing_from_app))

# Pure JVM modules stay Android-free.
pure_offenders=[]
for module in ("core/model", "core/common"):
    src=MODULES[module]/"src/main/kotlin"
    for file in src.rglob('*.kt'):
        text=file.read_text(encoding='utf-8',errors='replace')
        if re.search(r'^import\s+android(?:x)?\.', text, re.M):
            pure_offenders.append(str(file.relative_to(ROOT)))
check("pure core modules remain Android-free", not pure_offenders, str(pure_offenders))

passed=sum(1 for _,ok,_ in checks if ok)
print(f"module checks: {passed}/{len(checks)} PASS")
if passed != len(checks):
    sys.exit(1)
