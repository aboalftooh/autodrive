#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--output',type=Path); a=ap.parse_args(argv); root=a.root.resolve()
    A=(root/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt').read_text()
    I=(root/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt').read_text()
    F=(root/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt').read_text()
    N=(root/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt').read_text()
    bal=(root/'feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt').read_text()
    comp=(root/'feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt').read_text()
    status=(root/'feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt').read_text()
    chat=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt').read_text()
    msg=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt').read_text()
    img=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt').read_text()
    phone=(root/'feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt').read_text()
    code=(root/'feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt').read_text()
    cases=[]
    def case(name,ok,detail=''):
        cases.append({'name':name,'status':'PASS' if ok else 'FAIL','detail':detail})
    # Positive contract fixtures.
    case('positive destructive primary default-safe','tone: AutoDrivePrimaryButtonTone = AutoDrivePrimaryButtonTone.Primary' in A and 'AutoDrivePrimaryButtonTone.Destructive' in bal)
    case('positive compact multiline text field','AutoDriveTextFieldLayout.CompactMultiline' in chat and 'AutoDriveTextFieldLayout.CompactMultiline -> 56.dp' in I)
    case('positive isError without duplicate body','isError: Boolean = errorText != null' in I and 'isError = state is CodeState.Error' in code)
    case('positive bottom sheet skip partial', 'skipPartiallyExpanded = true' in comp and 'rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)' in F)
    case('positive back header rich title','titleContent = {' in msg and 'if (titleContent != null) titleContent()' in N)
    case('positive back header trailing source compatibility', re.search(r'titleContent: \(@Composable \(\) -> Unit\)\? = null,\s*trailingAction: \(@Composable \(\) -> Unit\)\? = null,',N,re.S) is not None)
    case('positive icon descriptions',all(x in chat+msg for x in ['إلغاء التسجيل الصوتي','إرسال التسجيل الصوتي','إيقاف الرسالة الصوتية مؤقتًا','تشغيل الرسالة الصوتية']))
    case('positive no caller sub48',re.search(r'\.size\(\s*(?:36|40|44)\.dp\s*\)',chat+msg+img) is None)
    case('positive status adapter', 'AutoDriveStatusChip' in status and 'Surface(' not in status)
    case('positive phone external error retained','if (state is PhoneAuthState.Error)' in phone and 'isError = state is PhoneAuthState.Error' in phone)
    case('positive code external error retained','if (state is CodeState.Error)' in code and 'isError = state is CodeState.Error' in code)
    case('positive recording destructive semantics','AutoDrivePrimaryButtonTone.Destructive' in chat and 'onClick = onStop' in chat)
    # Negative fixtures use in-memory mutations only.
    badA=A.replace('tone: AutoDrivePrimaryButtonTone = AutoDrivePrimaryButtonTone.Primary,','tone: AutoDrivePrimaryButtonTone = AutoDrivePrimaryButtonTone.Primary,\n    rawColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,')
    case('negative raw Color public parameter detected','rawColor:' in badA and 'Color' in badA)
    fifth=N+'\nfun AutoDriveBackHeaderUnsafe(rawDp: androidx.compose.ui.unit.Dp) = Unit\n'
    case('negative fifth API escape detected','rawDp:' in fifth)
    case('negative destructive meaning loss detected','AutoDrivePrimaryButtonTone.Destructive' not in bal.replace('AutoDrivePrimaryButtonTone.Destructive','AutoDrivePrimaryButtonTone.Primary'))
    case('negative compact 112 regression detected','AutoDriveTextFieldLayout.CompactMultiline -> 112.dp' in I.replace('AutoDriveTextFieldLayout.CompactMultiline -> 56.dp','AutoDriveTextFieldLayout.CompactMultiline -> 112.dp'))
    case('negative bottom sheet state loss detected','skipPartiallyExpanded = true' not in comp.replace('skipPartiallyExpanded = true','skipPartiallyExpanded = false'))
    case('negative typing indicator loss detected','يكتب...' not in msg.replace('يكتب...',''))
    case('negative empty description detected','contentDescription = ""' in chat.replace('contentDescription = "إرسال التسجيل الصوتي"','contentDescription = ""'))
    case('negative caller shrink detected',re.search(r'\.size\(\s*44\.dp\s*\)',chat+'\nModifier.size(44.dp)') is not None)
    case('negative raw primitive detected',re.search(r'(?<!AutoDrive)IconButton\s*\(',chat+'\nIconButton(onClick={}){}') is not None)
    case('negative status clone detected','Surface(' in status+'\nSurface() { }')
    failed=[x for x in cases if x['status']!='PASS']
    payload={'schemaVersion':64,'tool':'test_designsystem_verification_v64','explicitOutcomes':len(cases),'cases':cases,'verdict':'PASS' if not failed else 'FAIL','failed':[x['name'] for x in failed]}
    if a.output:
        out=a.output if a.output.is_absolute() else root/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f"V64 FIXTURES: {payload['verdict']} ({len(cases)-len(failed)}/{len(cases)})")
    for x in failed: print(' -',x['name'])
    return 0 if not failed else 1
if __name__=='__main__': raise SystemExit(main())
