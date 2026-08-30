package com.autodrive.app.feature.info.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability

private data class FaqItem(val question: String, val answer: String)

private val FAQ_ITEMS = listOf(
    FaqItem("ما هو تطبيق بنزين؟", "بنزين تطبيق لمتابعة العمولات والفواتير والرصيد وطلبات السحب للمسوّقين وأصحاب الورش المرتبطين بالمنصوري عون الله."),
    FaqItem("كيف أسجل الدخول؟", "تدخل رقم هاتفك، ثم يصلك رمز تحقق OTP. بعد إدخال الرمز بنجاح تدخل إلى التطبيق."),
    FaqItem("هل أحتاج كود دعوة؟", "نعم. بعد التحقق من رقم الهاتف، تحتاج إلى كود دعوة صحيح حتى يتم ربطك بحسابك داخل النظام."),
    FaqItem("ما أنواع الحسابات داخل التطبيق؟", "يوجد نوعان: مسوّق وصاحب ورشة."),
    FaqItem("ما البيانات المطلوبة عند التسجيل؟", "الاسم، رقم الهاتف، بيانات البنك، ورقم الحساب. وإذا كان الحساب لصاحب ورشة، تُطلب أيضًا بيانات الورشة مثل اسم الورشة والتخصص والعنوان."),
    FaqItem("متى تظهر العمولة؟", "تظهر العمولة بعد تسجيل الفاتورة المرتبطة بحسابك ومزامنتها مع التطبيق."),
    FaqItem("متى تصبح العمولة قابلة للسحب؟", "العمولة تصبح قابلة للسحب بعد موعد الصرف الأسبوعي: الجمعة الساعة 9:00 صباحًا، بشرط تحقق شروط الفاتورة."),
    FaqItem("لماذا تظهر عمولتي معلّقة؟", "تظهر العمولة معلّقة إذا كانت لم تصل بعد إلى موعد الصرف الأسبوعي، أو إذا كانت الفاتورة آجلة ولم تُسدد بالكامل، أو إذا لم تكتمل شروط الأهلية."),
    FaqItem("متى تكون عمولة الفاتورة النقدية قابلة للسحب؟", "إذا كانت الفاتورة بيعًا نقديًا وبها عمولة أكبر من صفر، تصبح قابلة للسحب بعد مرور موعد الجمعة الساعة 9:00 صباحًا حسب منطق الأهلية."),
    FaqItem("متى تكون عمولة الفاتورة الآجلة قابلة للسحب؟", "إذا كانت الفاتورة آجلة، يجب أن تكون مسددة بالكامل، وبعدها تصبح قابلة للسحب عند موعد الصرف الأسبوعي."),
    FaqItem("أين أجد الرصيد القابل للسحب؟", "من شاشة الرصيد أو من بطاقة الرصيد داخل التقارير، حسب الواجهة الحالية في التطبيق."),
    FaqItem("كيف أطلب سحب العمولة؟", "تدخل إلى شاشة الرصيد، تضغط طلب سحب، تكتب المبلغ، ويمكنك إضافة ملاحظة اختيارية، ثم ترسل الطلب."),
    FaqItem("هل يمكنني طلب سحب مبلغ أكبر من الرصيد المتاح؟", "لا. التطبيق يمنع طلب مبلغ أكبر من الرصيد المتاح بعد خصم أي طلبات سحب قيد المعالجة."),
    FaqItem("لماذا لا أستطيع طلب السحب؟", "قد يكون السبب: لا يوجد رصيد متاح، المبلغ غير صحيح، المبلغ أكبر من الرصيد المتاح، أو بيانات الحساب البنكي غير مكتملة في الملف الشخصي."),
    FaqItem("طلب السحب كم يستغرق؟", "لا توجد مدة زمنية ثابتة لمعالجة طلب السحب. الطلب يظهر \"قيد المعالجة\" حتى تراجعه الإدارة وتقوم باعتماده أو رفضه أو إكماله."),
    FaqItem("ما حالات طلب السحب؟", "قيد المعالجة، ناجح، مرفوض، مكتمل."),
    FaqItem("هل يمكن إلغاء طلبات السحب المعلقة؟", "نعم، توجد وظيفة لإلغاء جميع طلبات السحب التي ما زالت قيد المعالجة."),
    FaqItem("ما معنى الرصيد المعلق؟", "هو مبلغ مرتبط بطلبات سحب قيد المعالجة، ولا يُحسب كرصيد متاح لطلب سحب جديد حتى تنتهي معالجته."),
    FaqItem("ما المقصود بإجمالي العمولات؟", "هو إجمالي العمولات التي حققها المستخدم منذ تاريخ الانضمام أو منذ بداية تسجيل بياناته داخل النظام."),
    FaqItem("أين أجد الفواتير؟", "من شاشة الفواتير أو من بطاقة الفواتير داخل التقارير، ويمكن فتح تفاصيل كل فاتورة لمعرفة مبلغ الفاتورة وقيمة العمولة."),
    FaqItem("ما هي العمولات الأسبوعية؟", "هي شاشة تعرض مشترياتك وعمولتك أسبوعيًا، ويظهر فيها أحدث الأسابيع حسب منطق التطبيق."),
    FaqItem("ما هي المسابقة الأسبوعية؟", "هي ترتيب أسبوعي يعتمد على قيمة مشتريات المستخدم خلال الأسبوع."),
    FaqItem("هل تظهر أسماء الورش أو المسوقين الآخرين في الترتيب؟", "لا. التطبيق يخفي أسماء الآخرين، ويظهر ترتيبك أنت فقط بوضوح."),
    FaqItem("كيف أدخل المسابقة الأسبوعية؟", "تدخل المسابقة عندما تكون لديك مشتريات مسجلة خلال الأسبوع. إذا كانت مشترياتك صفرًا، يظهر أنك لم تدخل المسابقة لذلك الأسبوع."),
    FaqItem("متى يُقفل الترتيب الأسبوعي؟", "يعرض التطبيق عدادًا لموعد قفل الترتيب، ومنطق الموعد مرتبط بالجمعة الساعة 9:00 صباحًا."),
    FaqItem("ما هي شارة زعيم الأسبوع؟", "هي شارة يحصل عليها صاحب المركز الأول في المسابقة الأسبوعية حسب قيمة المشتريات."),
    FaqItem("أين أعرف كم مرة فزت بزعيم الأسبوع؟", "من بطاقة عدد شارات زعيم الأسبوع داخل التقارير، حيث تعرض الأسابيع التي حصلت فيها على الشارة."),
    FaqItem("من هو عم دينمو؟", "عم دينمو شخصية داخل التطبيق تتفاعل مع نشاطك الأسبوعي. حالته تتغير حسب مشترياتك وترتيبك."),
    FaqItem("لماذا عم دينمو مطفي أو تعبان؟", "لأن النشاط الأسبوعي أو قيمة المشتريات منخفضة. كلما زاد نشاطك تحسنت حالة عم دينمو."),
    FaqItem("ما حالات عم دينمو؟", "مطفي، تعبان، صاحي، مولع، الأسطورة."),
    FaqItem("هل توجد نصائح ذكية داخل التطبيق؟", "نعم. التطبيق يعرض نصائح ورسائل ذكية مرتبطة بالنشاط أو الفواتير أو تحسين العمولة."),
    FaqItem("هل التطبيق يعمل بدون إنترنت؟", "التطبيق يحتفظ ببعض البيانات محليًا، لكن المزامنة، تسجيل الدخول، تحديث الفواتير، السحب، والرسائل تحتاج اتصالًا بالخادم."),
    FaqItem("ماذا يحدث لو طلبت سحب والإنترنت ضعيف؟", "قد يظهر الطلب محليًا ويتم وضعه في طابور مزامنة حتى يعود الاتصال، حسب منطق التطبيق."),
    FaqItem("هل تصلني إشعارات؟", "نعم. التطبيق يدعم إشعارات العمولات، الرسائل، تحديثات السحب، والتنبيهات المهمة."),
    FaqItem("كيف يُعبّأ رمز OTP تلقائيًا؟", "يستخدم التطبيق SMS Retriever لقراءة رسالة التحقق الخاصة به فقط دون طلب صلاحية قراءة الرسائل."),
    FaqItem("لماذا يطلب التطبيق إذن الإشعارات؟", "حتى يرسل تنبيهات عن العمولات والرسائل وتحديثات طلبات السحب."),
    FaqItem("هل يمكن تعديل بياناتي؟", "نعم، يمكن تعديل البيانات من شاشة الملف الشخصي، مثل الاسم، الهاتف، بيانات البنك، وبيانات الورشة إن وجدت."),
    FaqItem("ماذا أفعل إذا تغير رقم حسابي البنكي؟", "ادخل إلى الملف الشخصي وعدّل بيانات البنك ورقم الحساب قبل إرسال طلب سحب جديد."),
    FaqItem("هل يجب مشاركة رمز OTP مع الإدارة؟", "لا. رمز التحقق خاص بك ولا يجب مشاركته مع أي شخص."),
    FaqItem("ماذا أفعل إذا لم يصل رمز التحقق؟", "تأكد من الرقم، انتظر انتهاء عداد إعادة الإرسال، ثم اضغط إعادة الإرسال.")
)

private val COMPETITION_FAQ_QUESTIONS = setOf(
    "ما هي المسابقة الأسبوعية؟",
    "هل تظهر أسماء الورش أو المسوقين الآخرين في الترتيب؟",
    "كيف أدخل المسابقة الأسبوعية؟",
    "متى يُقفل الترتيب الأسبوعي؟",
    "ما هي شارة زعيم الأسبوع؟",
    "أين أعرف كم مرة فزت بزعيم الأسبوع؟",
)

@Composable
fun FaqScreen(
    competitionAvailability: CompetitionAvailability,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val availableItems = remember(competitionAvailability) {
        val withoutCompetition = FAQ_ITEMS.filterNot { it.question in COMPETITION_FAQ_QUESTIONS }
        when (competitionAvailability) {
            CompetitionAvailability.DISABLED -> withoutCompetition
            CompetitionAvailability.LOCKED -> withoutCompetition + FaqItem(
                "ما هي المسابقة الأسبوعية؟",
                "ميزة تنافسية أسبوعية ستتوفر لاحقاً بعد اكتمال جاهزية المنافسة.",
            )
            CompetitionAvailability.ACTIVE -> FAQ_ITEMS
        }
    }

    val filteredItems = remember(searchQuery, availableItems) {
        if (searchQuery.isBlank()) availableItems
        else availableItems.filter { item ->
            item.question.contains(searchQuery, ignoreCase = true) ||
            item.answer.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            Column {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoDriveIconButton(
                        icon = Icons.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        onClick = onBack,
                    )
                    Text(
                        text  = "الأسئلة الشائعة",
                        style = MaterialTheme.typography.titleLarge,
                        color = AutoDriveText.Primary
                    )
                }

                // ─── مربع البحث ─────────────────────────
                AutoDriveTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    placeholder = "ابحث في الأسئلة...",
                    leadingIcon = Icons.Rounded.Search,
                    trailingContent = if (searchQuery.isNotEmpty()) ({
                        AutoDriveIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "مسح",
                            onClick = { searchQuery = "" },
                        )
                    }) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }
        }
    ) { padding ->
        if (filteredItems.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint               = AutoDriveText.Disabled,
                        modifier           = Modifier.size(48.dp)
                    )
                    Text("لا توجد نتائج لـ \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text  = "${filteredItems.size} سؤال",
                        style = MaterialTheme.typography.labelSmall,
                        color = AutoDriveText.Secondary,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
                items(filteredItems, key = { it.question }) { item ->
                    FaqItemCard(item = item, query = searchQuery)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FaqItemCard(item: FaqItem, query: String) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue  = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label        = "arrow_rotation"
    )

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (expanded) AutoDriveFinance.Withdrawable.copy(alpha = 0.35f) else AutoDriveBorderColor.Default,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column {
            // ─── رأس السؤال ──────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = item.question,
                    style      = MaterialTheme.typography.titleSmall,
                    color      = if (expanded) AutoDriveText.Primary else AutoDriveText.Primary.copy(alpha = 0.9f),
                    fontWeight = if (expanded) FontWeight.Medium else FontWeight.Normal,
                    modifier   = Modifier.weight(1f).padding(end = 8.dp),
                    lineHeight = 22.sp
                )
                Icon(
                    imageVector        = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "إغلاق" else "فتح",
                    tint               = if (expanded) AutoDriveFinance.Withdrawable else AutoDriveText.Secondary,
                    modifier           = Modifier.rotate(arrowRotation).size(22.dp)
                )
            }

            // ─── الإجابة المنسدلة ─────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(animationSpec = tween(durationMillis = 250)),
                exit    = shrinkVertically(animationSpec = tween(durationMillis = 200))
            ) {
                Column {
                    AutoDriveDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(AutoDriveSurface.Overlay)
                            .padding(16.dp)
                    ) {
                        Text(
                            text       = item.answer,
                            style      = MaterialTheme.typography.bodySmall,
                            color      = AutoDriveText.Secondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
