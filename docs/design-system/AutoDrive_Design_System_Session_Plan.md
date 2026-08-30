# AutoDrive Design System — Session Plan

## الهدف
بناء Design System موحّد لتطبيق AutoDrive اعتمادًا على:
- المشروع الحالي.
- الشاشات الموجودة فعليًا.
- التصاميم الجديدة المعتمدة بصريًا.
- مبدأ أن كل جلسة تنتج **مصدر حقيقة** تعتمد عليه الجلسة التالية.

---

## قاعدة العمل

لا تبدأ أي جلسة قبل إغلاق واعتماد مخرجات الجلسة السابقة.

كل ملف ناتج يجب أن يحتوي في نهايته على:

1. **القرارات المعتمدة**
2. **الممنوعات**
3. **العناصر المؤجلة**
4. **المشكلات المفتوحة**
5. **نقطة انطلاق الجلسة التالية**

---

# الجلسة 01 — Design Audit

## الهدف
فهم الوضع البصري الحالي للمشروع قبل تعديل أي شيء.

## المطلوب
- جرد جميع الشاشات.
- جرد المكونات المشتركة الحالية.
- فحص `core:designsystem`.
- تحديد التنسيقات المكتوبة مباشرة داخل الشاشات.
- تحديد التكرار والاختلافات.
- مقارنة الواجهات الحالية بالتصاميم الجديدة.
- تحديد ما يجب:
  - الاحتفاظ به.
  - إعادة تصميمه.
  - دمجه.
  - إلغاؤه.

## لا يتم في هذه الجلسة
- كتابة كود.
- تعديل المكونات.
- تغيير الثيم.

## المخرج
`01_DESIGN_AUDIT.md`

## يصبح مصدر الحقيقة لـ
الجلسة 02.

---

# الجلسة 02 — Foundations

## الهدف
تثبيت الهوية البصرية الرسمية للتطبيق.

## المطلوب
تعريف واعتماد:

### Colors
- Backgrounds
- Surfaces
- Primary
- Secondary
- Success
- Warning
- Error
- Info
- Text colors
- Borders
- Disabled states

### Typography
- الخط الأساسي: Tajawal
- مستويات العناوين.
- النصوص.
- الأرقام الكبيرة.
- Labels.
- الأوزان.
- Line height.

### Spacing
مقياس موحد للمسافات.

### Radius
مقياس موحد للاستدارات.

### Borders
- السمك.
- الشفافية.
- الحالات.

### Glow / Shadow
- متى يستخدم.
- شدته.
- الألوان المسموحة.

### Icons
- الأحجام.
- السمك.
- حالات اللون.

### Motion
- مدد الحركة.
- حالات الضغط.
- الانتقالات.

### RTL
قواعد الاتجاه والمحاذاة والمسافات.

## المخرج
`02_FOUNDATIONS.md`

## يصبح مصدر الحقيقة لـ
الجلسة 03.

---

# الجلسة 03 — Component Specification

## الهدف
تعريف مكتبة المكونات الأساسية قبل تنفيذها.

## المكونات الأولية

### Actions
- Primary Button
- Secondary Button
- Text Button
- Icon Button
- FAB

### Inputs
- Text Field
- Search Field
- Numeric Field

### Containers
- Base Card
- Metric Card
- Highlight Card
- Alert Card

### Navigation
- Bottom Navigation
- Top Header
- Back Header

### Feedback
- Badge
- Status Chip
- Snackbar
- Dialog
- Bottom Sheet
- Loading State
- Empty State

### Data display
- Avatar
- List Row
- Section Header
- Divider
- Stat Value
- Status Indicator

## لكل Component يتم تحديد
- الوظيفة.
- الشكل.
- الأحجام.
- الحالات.
- الألوان.
- التفاعل.
- Disabled state.
- Loading state.
- RTL behavior.
- متى يستخدم.
- متى لا يستخدم.

## المخرج
`03_COMPONENT_SPEC.md`

## يصبح مصدر الحقيقة لـ
الجلسة 04.

---

# الجلسة 04 — UI Patterns

## الهدف
تعريف التركيبات المتكررة التي تتكون من عدة Components.

## Patterns مقترحة
- Screen Header
- Dashboard Hero
- Metric Summary
- Conversation Item
- Transaction Row
- Pending Request Card
- Settings Group
- Settings Row
- Report Stat Tile
- Media Action Group
- Search Results List
- Empty Screen
- Error Screen
- Loading Screen

## المطلوب
لكل Pattern:
- التركيب.
- ترتيب العناصر.
- المسافات.
- الحالات.
- التفاعل.
- الشاشات التي تستخدمه.

## المخرج
`04_PATTERNS.md`

## يصبح مصدر الحقيقة لـ
الجلسة 05.

---

# الجلسة 05 — Screen Specifications

## الهدف
تحويل التصاميم المعتمدة إلى مواصفات تنفيذية دقيقة.

## ترتيب الشاشات

1. الرئيسية.
2. المحادثات.
3. محادثة جديدة.
4. التقارير.
5. الرصيد.
6. الإعدادات.

## لكل شاشة يتم تحديد
- الهيكل.
- ترتيب الأقسام.
- المكونات المستخدمة.
- المسافات.
- الحالات.
- Empty state.
- Loading state.
- Error state.
- Scroll behavior.
- Bottom navigation behavior.
- Responsive behavior.
- RTL behavior.

## المرجع البصري
التصاميم الجديدة المعتمدة هي المرجع الأساسي، وليس التصميم القديم.

## المخرج
`05_SCREEN_SPECS.md`

## يصبح مصدر الحقيقة لـ
الجلسة 06.

---

# الجلسة 06 — Design System Architecture

## الهدف
تحديد كيف سيعيش Design System داخل المشروع معماريًا.

## المطلوب
تحديد مسؤولية:

`core:designsystem`

وما يجب أن يحتويه فقط.

## التقسيم المقترح

```text
core/designsystem/
├── foundation/
│   ├── Color
│   ├── Typography
│   ├── Spacing
│   ├── Radius
│   ├── Border
│   ├── Motion
│   └── IconSize
│
├── components/
│   ├── buttons/
│   ├── cards/
│   ├── inputs/
│   ├── navigation/
│   ├── feedback/
│   └── data/
│
├── patterns/
│
└── theme/
```

## قواعد معمارية أساسية
- Design System لا يحتوي Business Logic.
- لا ViewModel داخل Design System.
- لا Repository داخل Design System.
- لا Feature-specific state داخله.
- الـ Feature يمرر البيانات والحالة للمكون.
- لا ألوان hardcoded داخل الشاشات.
- لا Radius عشوائي داخل Feature.
- لا TextStyle محلي بدون سبب موثق.

## المخرج
`06_DS_ARCHITECTURE.md`

## يصبح مصدر الحقيقة لـ
الجلسة 07.

---

# الجلسة 07 — Design System Implementation

## الهدف
تنفيذ Design System نفسه قبل ترحيل الشاشات.

## ترتيب التنفيذ

### أولًا
Foundations.

### ثانيًا
Primitives.

### ثالثًا
Components.

### رابعًا
Patterns.

## المطلوب
- التنفيذ تدريجيًا.
- Preview لكل Component.
- حالات متعددة لكل Component.
- التأكد من RTL.
- التأكد من Dark Mode.
- عدم لمس منطق الشاشات بعد.

## المخرج
`07_IMPLEMENTATION_STATE.md`

يحتوي على:
- ما تم تنفيذه.
- ما لم ينفذ.
- المكونات الجاهزة.
- المشكلات.
- الفروقات عن المواصفات.

## يصبح مصدر الحقيقة لـ
الجلسة 08.

---

# الجلسة 08 — Screen Migration

## الهدف
ترحيل الشاشات الحالية إلى Design System الجديد.

## ترتيب الترحيل

1. Bottom Navigation
2. Headers
3. Settings
4. Balance
5. Conversations
6. New Chat
7. Reports
8. Home

## سبب هذا الترتيب
نبدأ بالأكثر تكرارًا والأقل تعقيدًا، ثم نصل للرئيسية باعتبارها الأكثر خصوصية وتعقيدًا.

## قواعد الترحيل
- لا تغيير في Business Logic.
- لا تغيير في ViewModel إلا عند الضرورة القصوى.
- التغيير يكون في طبقة العرض أساسًا.
- كل شاشة يجب أن تعتمد على Components المعتمدة.
- ممنوع إنشاء نسخة محلية من Component موجود.

## المخرج
`08_MIGRATION_STATE.md`

## يصبح مصدر الحقيقة لـ
الجلسة 09.

---

# الجلسة 09 — Visual QA

## الهدف
التأكد أن التطبيق الحقيقي يطابق النظام والتصاميم المعتمدة.

## الفحص

### بصري
- الألوان.
- المسافات.
- الخطوط.
- الأحجام.
- Radius.
- Glow.
- Borders.

### سلوكي
- Scroll.
- Dialogs.
- Bottom Sheets.
- Navigation.
- ضغط الأزرار.
- الحالات.

### حالات العرض
- بيانات طبيعية.
- بيانات كثيرة.
- نص طويل.
- أرقام كبيرة.
- Empty.
- Loading.
- Error.
- Disabled.

### RTL
فحص كل شاشة بالكامل.

## المخرج
`09_VISUAL_QA.md`

## يصبح مصدر الحقيقة لـ
الجلسة 10.

---

# الجلسة 10 — Consolidation & Governance

## الهدف
إغلاق Design System V1 ومنع عودة الفوضى مستقبلًا.

## المطلوب
- إزالة المكونات القديمة غير المستخدمة.
- إزالة الألوان المحلية.
- إزالة الـ duplicated styles.
- تثبيت naming conventions.
- توثيق كيفية إضافة Component جديد.
- توثيق كيفية تعديل Component موجود.
- وضع قواعد مراجعة لأي شاشة جديدة.

## قواعد مستقبلية
أي تصميم جديد يجب أن يجيب أولًا:

1. هل يوجد Component قائم؟
2. هل يوجد Pattern قائم؟
3. هل المشكلة تحتاج Variant جديد؟
4. هل نحتاج Component جديد فعلًا؟

## المخرج النهائي

`DESIGN_SYSTEM_V1.md`

هذا الملف يصبح **مصدر الحقيقة الرسمي والنهائي للهوية البصرية للتطبيق**.

---

# تسلسل مصادر الحقيقة

```text
01_DESIGN_AUDIT.md
        ↓
02_FOUNDATIONS.md
        ↓
03_COMPONENT_SPEC.md
        ↓
04_PATTERNS.md
        ↓
05_SCREEN_SPECS.md
        ↓
06_DS_ARCHITECTURE.md
        ↓
07_IMPLEMENTATION_STATE.md
        ↓
08_MIGRATION_STATE.md
        ↓
09_VISUAL_QA.md
        ↓
DESIGN_SYSTEM_V1.md
```

---

# قاعدة الإغلاق لكل جلسة

قبل الانتقال للجلسة التالية يجب تسجيل:

```text
STATUS: APPROVED

Decisions:
- ...

Forbidden:
- ...

Deferred:
- ...

Open Issues:
- ...

Next Session Input:
- ...
```

إذا لم تصبح الحالة `APPROVED` فلا تبدأ الجلسة التالية.

---

# النتيجة المستهدفة

في نهاية الخطة:

- Design System واحد.
- مصدر حقيقة واحد.
- الشاشات لا تعرف تفاصيل الهوية بنفسها.
- كل Component له وظيفة واضحة.
- أي تعديل بصري مركزي ينعكس على التطبيق كله.
- التصاميم الجديدة تصبح جزءًا من النظام، وليست مجرد صور مرجعية.
