# COMPONENT_ADOPTION_WAVES_v64

Input: `AutoDrive-v63-settings-static-runtime-blocked.zip`  
SHA-256: `b6c8f4c65c2a462cd2b5deedcc788a3478c50b3a097d413fa56e04a20b432c25`

## Wave checkpoints

| Checkpoint | Confirmed | Material | A11Y-001 | Candidates |
|---|---:|---:|---:|---:|
| PRE | 46 | 43 | 3 | 18 |
| Wave 0 | 46 | 43 | 3 | 18 |
| Wave A | 36 | 33 | 3 | 14 |
| Wave B | 19 | 16 | 3 | 14 |
| Wave C | 0 | 0 | 0 | 6 |

## Wave 0 — DS providers

| File | Pre SHA-256 | Post SHA-256 | Result |
|---|---|---|---|
| `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` | `dc28cd3efe428f3c01b82b81fcc2256d651562909daf95cd4dd43c1e81f2598a` | `8ffd0ca42e94015e6379f9cfff4ba3cfd6fd6d5b2b3a56873014225d05be06ca` | CHANGED |
| `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` | `3848eea77a1d518baab3931d2cb125fc5f078b5d16159ac3365189e794932498` | `a8470cec63305e0dc952bd00b7d4a8ca5108b1ef711d46deeb865fa8a24c1e17` | CHANGED |
| `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt` | `164789512b062fe2bc300115f12a1101e1b0cee53baeb582b09fa2deb9caddca` | `46d5173bcdec4dc1301707537a176abcb93014c2aec06e05ff2ac3ae2339eeaf` | CHANGED |
| `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt` | `75f9522969fc76bae8a04869b3f2e88038648919a4f6fd8852471f9f57806538` | `d66491936f2372f2f379bad9497d938aa1abd5d53f41fbea343cfe8751e3ab1e` | CHANGED |

Approved gaps: destructive PrimaryButton tone; label/error/compact TextField; BottomSheet partial-state; BackHeader rich title. No fifth API or raw public Color/Shape/Dp escape hatch.

## Wave A — Balance / Commission / Notifications / Competition

| File | Pre SHA-256 | Post SHA-256 | Result |
|---|---|---|---|
| `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt` | `69fc0bc1c8a2333948b40c2b84000aaa150917ce9d93a820deddfcd9735e5447` | `cc645e29821188fe018b8ef18ea3f2f1c0e9ec8bdff216bf97cab2dd8e8f0d82` | CHANGED |
| `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt` | `8766c151eb4fdc653c6de8732ac526fa84eb15bd4a06a1a6caad044eb370ea9d` | `fbe8fb342726a7ebc71a7418dfe39a5678293b018cb090c6b07617664149627e` | CHANGED |
| `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt` | `b84c23a020cbc7aeec5ae0517f07b7ae86fa4f22ed1735ec02fa4b3f0971d6b2` | `b84c23a020cbc7aeec5ae0517f07b7ae86fa4f22ed1735ec02fa4b3f0971d6b2` | UNCHANGED |
| `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt` | `6f960bddcbc1cda4d6c824f52d592c9088763f806abf6fc3a1eec1e28e2e99e8` | `6f960bddcbc1cda4d6c824f52d592c9088763f806abf6fc3a1eec1e28e2e99e8` | UNCHANGED |
| `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt` | `7dd23b0bf3a3692f472f75d741c6a4c8f3fa67c8e9701ec7c911e0b5a862b2d6` | `e379ff14b8b6cc5a7aa8b4eaccb5f6fc08ccc86d640ffa061c20793171dabbf0` | CHANGED |
| `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt` | `667226b3d182a8f043073e523262bc32e55f001ed3d54fa2e9f28e580d5bc539` | `af1fbe1c263f84b77e1ca54e0e0047b467fa59c12b3e4cdc5102d2cec217a45e` | CHANGED |
| `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt` | `d01cae03a44545c945b2a6519ada4e73af66eaf21dc2b1e12a4b1270e11ecb22` | `494546a101095f27a010e16c2dfb35f85cc2e22881a9169f70fc50743397091b` | CHANGED |
| `feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt` | `d30172be7085bad9db3511ab78afba95a4210d571c32f49038d17c76082a880d` | `e72d8f39fc5e1a630b75f5e3fa1fd29249ced61348cebe48aab591fdde8fc757` | CHANGED |

Target: 10 confirmed Material IDs + 4 local Material candidates. Adopted official buttons, divider, sheet, dialog, FAB, headers, loading state, status chip.

## Wave B — Info / Auth

| File | Pre SHA-256 | Post SHA-256 | Result |
|---|---|---|---|
| `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt` | `06b45ca947cea396c19431068c2a94f2051fc50c0f1491806ac0bd8ff27e1cd8` | `06b45ca947cea396c19431068c2a94f2051fc50c0f1491806ac0bd8ff27e1cd8` | UNCHANGED |
| `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/AboutAppScreen.kt` | `302fac510879bf9014a02d9202d2dd0589cb2b12c45d52e4a324350f2324b836` | `d9bc7a3213ef927255bceb9cc00dbb0087f926edca56568f89e2c7d191a91dfd` | CHANGED |
| `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt` | `ec846f455a8ecb44ef05b05ce426e798056ea5fe2ed8b9e6e524e2578e2e10f7` | `3d9d62d6b704d32abc0cd7573320c1d5a8d893ec8fc3eeb044f82ec8b5f7dc38` | CHANGED |
| `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/PrivacyPolicyScreen.kt` | `81a72a3603f315b10d8884bdc8d90a5c7927e714f8196502626d31a0e5786115` | `11285193b479595c0552d64ebe9021e1f535f76288391a42bb07c4d795a45076` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt` | `2c7124596398d941629a401e8dc94e44b4cf6448b6e3f608097e492e0ec8aacc` | `9273892e1ab842a469defbe6146edb4137aad130cad42840cba5415665c083b7` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt` | `0f77936bd7f2469be778839d5d81263b6b451aa4196dfff2ac737c13977a7fa8` | `2e9a4df7731ad5cb141cfae01b9ae0c7aeedee38f17eb0273c8f2e1999bd59ba` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WelcomeScreen.kt` | `8f1f3fcb23b01482f965489740f47be4e5614e95585029c4bf5083a69883a09d` | `8f1f3fcb23b01482f965489740f47be4e5614e95585029c4bf5083a69883a09d` | UNCHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt` | `c1e8e9abd9e41553e65f519972e830bce13dc06928b6c17b5d4e5249acfddd3a` | `c1e8e9abd9e41553e65f519972e830bce13dc06928b6c17b5d4e5249acfddd3a` | UNCHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/OtpInputScreen.kt` | `95f367e4d61da485ee1d469dea74669f9c0646e3877ba217e622eb5dcdade924` | `ec8b639652ebbe16e58963a0a77871fbcacfe3984ac7443c47e06c49d0765611` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt` | `6155413d2889d8dad7f739a3ae99867cb24a91bbd2dbe7b633b5f30344497f8d` | `5ca1ce40124106553754a8f7da47a9b872f242d7c7edd6ba209601ecaf982933` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt` | `e28cf7dd94d21f9553c1c00884c46316d2a96f0e3b7cd003fdec92c2a36f04a5` | `389676823832a891d68cb0e248ba9b124f6b9000f5960b6547b29a4d501872ee` | CHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt` | `19a5a010275cbb78eb9b883b9cb4b60984bcec82bc14e56b9acbd357f5fc9185` | `19a5a010275cbb78eb9b883b9cb4b60984bcec82bc14e56b9acbd357f5fc9185` | UNCHANGED |
| `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt` | `1bf84a0baa974d0a8bf0c447ebf11e37fdded6ea0df9bbbc621e7d8de2b32e9a` | `5d6c6b6106a65a743c372d91e0366f603c160ecf38ca50e5201996be326fc1e5` | CHANGED |

Target: 17 confirmed Material IDs. Validation, callbacks, OTP/phone/join-code semantics and visible content ownership retained.

## Wave C — Chat / Navigation / Recent

| File | Pre SHA-256 | Post SHA-256 | Result |
|---|---|---|---|
| `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt` | `61958995f7f3a34aa253e1410de7aec42e1f2fa6312fe77e70770f51bd39365f` | `61958995f7f3a34aa253e1410de7aec42e1f2fa6312fe77e70770f51bd39365f` | UNCHANGED |
| `app/src/main/kotlin/com/autodrive/app/navigation/AppNavigation.kt` | `38f1f56444c74eb9b4322732996c9cf8ccfc5a64c6db798155d474aa9ef32b2b` | `38f1f56444c74eb9b4322732996c9cf8ccfc5a64c6db798155d474aa9ef32b2b` | UNCHANGED |
| `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt` | `a48ac2845376f69abc8a573dec2b297c5e7ebb69f377f75a7047e3cfac0eb07f` | `edea02de0f0752e929cb290bfc20edf1cb35e5f333c6c126ab0eef0eb46cfcac` | CHANGED |
| `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt` | `397880dbb9fc3d8b2306ea60d1cc7a140b772ab0a1b5afbaea2aba4ddc7df6fc` | `674f426342ebb1e8acbdfd86be8ec7be9f514ce80807ddb992bcc7701b4a247b` | CHANGED |
| `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt` | `6375b79497252cc9ee2c0fc8657358cdd07770826377980cb6cef30e7ac838e8` | `2d2ab3de4a0a9ef7809d640876753e4e2f77fa2b01cbedc3a63cd3d1398a658b` | CHANGED |
| `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatScreen.kt` | `d31bf1fc6c41fc4a5332bb9a4e014cc90b7d5652b7b3cdbbec116d0483b7bdf0` | `f7601b536ddf504ff3714c6b5b19e935fd568156cab741ae63fba29ed9bc3dc8` | CHANGED |
| `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt` | `cf9367823569821345580c8e66ac01f850580105560ceeb401158331e92f9b3b` | `cf9367823569821345580c8e66ac01f850580105560ceeb401158331e92f9b3b` | UNCHANGED |

Target: 16 confirmed Material IDs + 3 forced A11Y IDs + 8 candidate resolutions (2 Material + 6 touch-target). Chat recording/media/message behavior retained.

## Verification

- `tools/verify_designsystem_v64_adoption.py`: PASS
- `tools/test_designsystem_verification_v64.py`: PASS / 22
- Protected production digest: `164fb50c87aa7bad2d583fc5a4fa299f49b93e1a332b73f547cb17dc00b7a580`
- Protected DS digest: `41ff466d17f9395a5318a4ac4d332baaeba7c8f23cc28f859745e3662fd48b26`
- Production Kotlin count: 251
- Production Kotlin changed: exactly 23

Final wave status: `STATIC_COMPONENT_ADOPTION_COMPLETE / UI_RUNTIME_BLOCKED`; Ratchet accepted at `v64`, parent gates PASS, runtime bootstrap blocked by `services.gradle.org`.
