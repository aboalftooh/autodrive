#!/usr/bin/env python3
from pathlib import Path
import re
import sys

from project_layout import ALL_SOURCE_ROOTS, MAIN_SOURCE_ROOTS, MODULES, ROOT

errors=[]
checks=0

def ok(name, condition, detail=''):
    global checks
    checks += 1
    if condition:
        print(f'PASS: {name}')
    else:
        errors.append(f'{name}: {detail}')
        print(f'FAIL: {name}: {detail}')

# physical package agreement for every declared module/source set
all_files=[]
for src in ALL_SOURCE_ROOTS:
    files=list(src.rglob('*.kt'))
    all_files.extend(files)
    mismatches=[]
    for f in files:
        text=f.read_text(errors='replace')
        m=re.search(r'^package\s+([\w.]+)', text, re.M)
        if not m:
            mismatches.append(f'{f.relative_to(ROOT)}: missing package')
            continue
        expected='.'.join(f.relative_to(src).with_suffix('').parts[:-1])
        if m.group(1)!=expected:
            mismatches.append(f'{f.relative_to(ROOT)}: {m.group(1)} != {expected}')
    ok(f'package paths {src.relative_to(ROOT)} ({len(files)} files)', not mismatches, '; '.join(mismatches[:8]))

app_root=MODULES['app']/'src/main/kotlin/com/autodrive/app'
legacy={'data','domain','ui','utils','notifications','observability'}
present=sorted(p.name for p in app_root.iterdir() if p.is_dir() and p.name in legacy)
ok('legacy layer roots removed from app', not present, str(present))
allowed={'feature','coordinator','navigation','di'}
roots=sorted(p.name for p in app_root.iterdir() if p.is_dir())
unexpected=[x for x in roots if x not in allowed]
ok('application package roots are composition-only', not unexpected, str(unexpected))

# Core source roots cannot import feature or app composition packages.
core_offenders=[]
for name,module in MODULES.items():
    if not name.startswith('core/'): continue
    src=module/'src/main/kotlin'
    if not src.exists(): continue
    for f in src.rglob('*.kt'):
        text=f.read_text(errors='replace')
        for target in ('feature','coordinator','navigation','di'):
            if f'import com.autodrive.app.{target}.' in text:
                core_offenders.append(str(f.relative_to(ROOT)))
ok('core has no outward application dependencies', not core_offenders, str(core_offenders[:10]))

# Cross-feature imports may target domain contracts/models only.
cross=[]
edges=set()
for name,module in MODULES.items():
    if not name.startswith('feature/'): continue
    owner=name.split('/',1)[1]
    src=module/'src/main/kotlin'
    if not src.exists(): continue
    for f in src.rglob('*.kt'):
        text=f.read_text(errors='replace')
        for m in re.finditer(r'import\s+com\.autodrive\.app\.feature\.([^.]+)\.([^\s]+)', text):
            target,suffix=m.group(1),m.group(2)
            if target==owner: continue
            edges.add((owner,target))
            if not suffix.startswith('domain.'):
                cross.append(f'{f.relative_to(ROOT)} -> {target}.{suffix}')
ok('cross-feature imports use domain contracts only', not cross, str(cross[:10]))

# No feature cycles.
graph={}
for a,b in edges: graph.setdefault(a,set()).add(b)
cycles=[]
def visit(node,path):
    if node in path:
        cycles.append(path[path.index(node):]+[node]); return
    for nxt in graph.get(node,()): visit(nxt,path+[node])
for node in graph: visit(node,[])
canon=set()
for c in cycles:
    body=c[:-1]
    if not body: continue
    rots=[tuple(body[i:]+body[:i]) for i in range(len(body))]
    canon.add(min(rots))
ok('feature graph is acyclic', not canon, str(sorted(canon)))

# Internal import resolution across all modules and test source sets.
packages=set(); decls=set()
for f in all_files:
    text=f.read_text(errors='replace')
    pm=re.search(r'^package\s+([\w.]+)',text,re.M)
    if not pm: continue
    pkg=pm.group(1); packages.add(pkg)
    for dm in re.finditer(r'^(?:(?:data|sealed|enum|annotation|value|open|abstract)\s+)*(?:class|interface|object|typealias|fun|val|var)\s+([A-Za-z_][A-Za-z0-9_]*)', text, re.M):
        decls.add(pkg+'.'+dm.group(1))

def generated_symbol(name: str) -> bool:
    return name.endswith('.R') or name.endswith('.BuildConfig') or '.R.' in name or '.BuildConfig.' in name

unresolved=[]
for f in all_files:
    text=f.read_text(errors='replace')
    for im in re.finditer(r'^import\s+(com\.autodrive\.app\.[\w.*]+)(?:\s+as\s+\w+)?',text,re.M):
        name=im.group(1)
        if generated_symbol(name): continue
        if name.endswith('.*'):
            if name[:-2] not in packages:
                unresolved.append(f'{f.relative_to(ROOT)}: {name}')
            continue
        probe=name
        found=False
        while probe.startswith('com.autodrive.app.'):
            if probe in decls or probe in packages:
                found=True; break
            probe=probe.rsplit('.',1)[0]
        if not found: unresolved.append(f'{f.relative_to(ROOT)}: {name}')
ok('project imports resolve across modules', not unresolved, '; '.join(unresolved[:15]))

print(f'package checks: {checks-len(errors)}/{checks} PASS')
if errors:
    sys.exit(1)
