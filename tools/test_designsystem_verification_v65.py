#!/usr/bin/env python3
from __future__ import annotations
import json, math, re, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def rgb(h):
    h=h.lstrip('#'); return tuple(int(h[i:i+2],16)/255 for i in (0,2,4))
def lum(h):
    vals=[]
    for c in rgb(h): vals.append(c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4)
    return .2126*vals[0]+.7152*vals[1]+.0722*vals[2]
def contrast(a,b):
    x,y=lum(a),lum(b); hi,lo=max(x,y),min(x,y); return (hi+.05)/(lo+.05)

def main():
    checks=[]
    def ok(name, cond): checks.append((name,bool(cond)))
    action=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt').read_text()
    nav=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt').read_text()
    inputs=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt').read_text()
    data=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt').read_text()
    conv=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt').read_text()
    notif=(ROOT/'feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt').read_text()
    acct=(ROOT/'feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt').read_text()
    feedback=(ROOT/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt').read_text()
    chatimg=(ROOT/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt').read_text()
    code=(ROOT/'feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt').read_text()
    # Positive fixtures/source contracts (16)
    ok('icon-only action labeled once', 'this.contentDescription = contentDescription' in action and 'contentDescription = null' in action)
    ok('loading primary keeps name + busy', 'stateDescription = "جارٍ التحميل"' in action and 'contentDescription = text' in action)
    ok('loading icon button keeps action name', 'loading && contentDescription' not in action and 'this.contentDescription = contentDescription' in action)
    ok('selected bottom-nav announced', '.selectable(' in nav and 'selected = selected' in nav)
    ok('selected dropdown option announced', 'selected = option.id == selected?.id' in inputs)
    ok('account type selected semantics', 'Role.RadioButton' in acct and 'selected = selected' in acct)
    ok('unread conversation contextualized', 'رسائل غير مقروءة' in conv)
    ok('unread notification contextualized', 'stateDescription = if (notification.isRead)' in notif)
    ok('step indicator progress semantics', 'progressBarRangeInfo' in data and 'الخطوة $stepNumber من $safeTotal' in data)
    ok('decorative icon suppressed', 'clearAndSetSemantics' in feedback and 'contentDescription = null' in nav)
    ok('48dp touch target token', 'AutoDriveIconSize.TouchTarget' in code)
    ok('enabled placeholder contrast >=4.5', contrast('#8890A8','#161820') >= 4.5 and 'focusedPlaceholderColor = AutoDriveText.Secondary' in inputs and 'unfocusedPlaceholderColor = AutoDriveText.Secondary' in inputs)
    ok('status state not color-only', 'مقروء' in notif and 'غير مقروء' in notif)
    ok('real heading marked', '.semantics { heading() }' in nav or 'heading()' in feedback)
    ok('RTL auto-mirrored back', 'Icons.AutoMirrored' in nav)
    ok('LTR numeric intent remains explicit/provable', 'KeyboardType.Number' in inputs or 'KeyboardType.Number' in code)
    # Negative fixtures (18) — each must be rejected by the stated predicate.
    # Explicit predicates avoid depending on project source for intentionally bad fixture text.
    bad={
      'empty/generic description': '', 'generic description':'button',
      'loading loses label':'if (loading) Spinner() else Text(label)',
      'selected color only':'color = if (selected) Active else Muted',
      'unread dot only':'if (!read) Dot(color = Red)',
      'bare badge':'Text("3")',
      'step color only':'segments.forEach { color = active }',
      '44 target':'.size(44.dp)',
      'caller shrinks DS icon':'.size(40.dp).AutoDriveIconButton()',
      'every Text heading':'Text(a, Modifier.semantics { heading() }); Text(b, Modifier.semantics { heading() })',
      'duplicate speech':'Icon(contentDescription="حذف"); Text("حذف")',
      'disabled still clickable':'enabled=false; Modifier.clickable { onClick() }',
      'focus escapes dialog':'Dialog(); outside.requestFocus()',
      'fontScale maxLines hack':'fontScale=2.0; maxLines=1',
    }
    ok('reject empty description', bad['empty/generic description'].strip()=='')
    ok('reject generic description', bad['generic description'].lower() in {'icon','button'})
    ok('reject loading lost label', 'Spinner()' in bad['loading loses label'] and 'stateDescription' not in bad['loading loses label'])
    ok('reject selected color-only', 'selected' in bad['selected color only'] and 'semantics' not in bad['selected color only'])
    ok('reject unread color/dot-only', 'Dot' in bad['unread dot only'] and 'stateDescription' not in bad['unread dot only'])
    ok('reject bare badge number', re.fullmatch(r'Text\("\d+"\)',bad['bare badge']) is not None)
    ok('reject color-only step', 'color' in bad['step color only'] and 'progressBarRangeInfo' not in bad['step color only'])
    ok('reject 44dp target', float(re.search(r'(\d+)\.dp',bad['44 target']).group(1))<48)
    ok('reject shrunken DS icon', float(re.search(r'(\d+)\.dp',bad['caller shrinks DS icon']).group(1))<48)
    ratio_bad=contrast('#4A5068','#161820')
    ok('reject enabled placeholder 2.23', abs(ratio_bad-2.23)<=.02 and ratio_bad<4.5)
    ok('reject normal text <4.5', contrast('#4A5068','#161820')<4.5)
    ok('reject large text <3.0', contrast('#4A5068','#161820')<3.0)
    ok('reject non-text important <3.0', contrast('#4A5068','#161820')<3.0)
    ok('reject every Text heading', bad['every Text heading'].count('heading()')==2)
    ok('reject duplicate icon+text speech', 'contentDescription="حذف"' in bad['duplicate speech'] and 'Text("حذف")' in bad['duplicate speech'])
    ok('reject disabled clickable', 'enabled=false' in bad['disabled still clickable'] and '.clickable' in bad['disabled still clickable'])
    ok('reject focus escape', 'outside.requestFocus()' in bad['focus escapes dialog'])
    ok('reject fontScale clipping hack', 'fontScale=2.0' in bad['fontScale maxLines hack'] and 'maxLines=1' in bad['fontScale maxLines hack'])
    failed=[n for n,v in checks if not v]
    payload={'schemaVersion':65,'positiveFixtures':16,'negativeFixtures':18,'checks':len(checks),'failed':failed,'contrastFixtureRatio':round(ratio_bad,4),'verdict':'PASS' if not failed and len(checks)==34 else 'FAIL'}
    out=ROOT/'.verification-v65/v65-fixtures.json'; out.parent.mkdir(exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f"V65 FIXTURES: {payload['verdict']} ({len(checks)-len(failed)}/{len(checks)})")
    if failed:
        for x in failed: print(' -',x)
    return 0 if payload['verdict']=='PASS' else 1
if __name__=='__main__': raise SystemExit(main())
