package com.autodrive.app.feature.info.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider

private data class PolicySection(val title: String, val body: String)

private val POLICY_SECTIONS = listOf(
    PolicySection(
        title = "1. البيانات التي يجمعها التطبيق",
        body  = """قد يجمع التطبيق البيانات التالية حسب استخدامك:

• رقم الهاتف المستخدم في تسجيل الدخول.
• رمز التحقق OTP عند تسجيل الدخول.
• الاسم الكامل.
• نوع الحساب: مسوّق أو صاحب ورشة.
• بيانات الورشة: الاسم، التخصص، عدد العمال، والعنوان.
• بيانات الحساب البنكي: اسم البنك ورقم الحساب.
• بيانات الفواتير المرتبطة بالمستخدم.
• بيانات العمولات والرصيد وطلبات السحب.
• الرسائل المتبادلة بين المستخدم والإدارة.
• الإشعارات وحالة قراءتها.
• بيانات الترتيب الأسبوعي وعدد مرات الفوز بشارة زعيم الأسبوع."""
    ),
    PolicySection(
        title = "2. استخدام البيانات",
        body  = """تُستخدم البيانات للأغراض التالية:

• إنشاء حساب المستخدم وربطه بالنظام.
• التحقق من رقم الهاتف عبر OTP.
• عرض الفواتير والعمولات والرصيد.
• تنفيذ طلبات السحب ومتابعة حالتها.
• عرض سجل الحركات المالية.
• عرض الترتيب الأسبوعي والمسابقة الأسبوعية.
• إرسال الإشعارات المتعلقة بالعمولات والرسائل والسحب.
• تحسين تجربة المستخدم داخل التطبيق.
• تمكين التواصل بين المستخدم والإدارة."""
    ),
    PolicySection(
        title = "3. أذونات الجهاز",
        body  = """يطلب التطبيق بعض الأذونات حسب الوظائف المستخدمة:

• الإنترنت: للاتصال بالخادم ومزامنة البيانات.
• الإشعارات: لإرسال تنبيهات العمولات والرسائل والتحديثات.
• الرسائل SMS: لقراءة رمز التحقق تلقائيًا عند وصوله.
• الكاميرا والملفات/الصور/الصوت: لدعم المرفقات داخل التطبيق عند الحاجة.

لا تُستخدم الأذونات إلا لتشغيل الوظائف المرتبطة بها."""
    ),
    PolicySection(
        title = "4. تخزين البيانات",
        body  = "يستخدم التطبيق تخزينًا محليًا داخل الجهاز لتسريع عرض البيانات والعمل عند ضعف الاتصال، كما تتم مزامنة البيانات مع الخادم عند توفر الإنترنت."
    ),
    PolicySection(
        title = "5. مشاركة البيانات",
        body  = "لا يعرض التطبيق أسماء المستخدمين الآخرين في الترتيب الأسبوعي؛ تظهر المراكز الأخرى بصورة مخفية مثل: xxxxx.\n\nلا تتم مشاركة بيانات المستخدم خارج نطاق تشغيل التطبيق وإدارة الحسابات والعمولات والسحب، إلا إذا تطلب القانون ذلك أو وافق المستخدم صراحة."
    ),
    PolicySection(
        title = "6. بيانات السحب والحساب البنكي",
        body  = "تُستخدم بيانات البنك ورقم الحساب لمعالجة طلبات السحب فقط. إذا لم تكن بيانات الحساب البنكي مكتملة، لن يتمكن المستخدم من إرسال طلب سحب حتى يكملها من الملف الشخصي."
    ),
    PolicySection(
        title = "7. الإشعارات",
        body  = """قد يرسل التطبيق إشعارات مثل:

• عمولة جديدة.
• رصيد جاهز للسحب.
• تحديث حالة طلب السحب.
• رسائل من الإدارة.

يمكن للمستخدم التحكم في صلاحية الإشعارات من إعدادات الجهاز."""
    ),
    PolicySection(
        title = "8. الرسائل والمحادثات",
        body  = "يدعم التطبيق إرسال واستقبال الرسائل بين المستخدم والإدارة. قد يتم حفظ الرسائل والمرفقات المرتبطة بها لتوفير سجل تواصل واضح."
    ),
    PolicySection(
        title = "9. حماية الحساب",
        body  = "يعتمد التطبيق على تسجيل الدخول برقم الهاتف ورمز التحقق OTP. يجب على المستخدم عدم مشاركة رمز التحقق مع أي شخص."
    ),
    PolicySection(
        title = "10. حذف أو تعديل البيانات",
        body  = "يمكن للمستخدم تعديل بعض بياناته من الملف الشخصي داخل التطبيق. أما حذف الحساب أو البيانات المرتبطة به فيتم عبر التواصل مع الإدارة."
    ),
    PolicySection(
        title = "11. ملاحظات مهمة",
        body  = "مدة معالجة طلب السحب غير محددة داخل التطبيق، وتظل حالة الطلب \"قيد المعالجة\" حتى تقوم الإدارة بمراجعته واعتماده أو رفضه."
    ),
    PolicySection(
        title = "12. التواصل",
        body  = "لأي استفسار بخصوص الخصوصية أو البيانات أو الحساب، يرجى التواصل مع إدارة المنصوري عون الله عبر قنوات التواصل المعتمدة داخل التطبيق."
    )
)

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
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
                    text  = "سياسة الخصوصية",
                    style = MaterialTheme.typography.titleLarge,
                    color = AutoDriveText.Primary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── رأس السياسة ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AutoDriveSurface.Raised)
                    .border(1.dp, AutoDriveBrand.Info.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AutoDriveBrand.Info.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = AutoDriveBrand.Info, modifier = Modifier.size(26.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("سياسة الخصوصية", style = MaterialTheme.typography.titleLarge, color = AutoDriveText.Primary, fontWeight = FontWeight.Bold)
                        Text("تطبيق بنزين", style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)
                        Text("آخر تحديث: 14 مايو 2026", style = MaterialTheme.typography.bodySmall, color = AutoDriveBrand.Info.copy(alpha = 0.85f))
                    }
                }
            }

            Text(
                text       = "توضح هذه السياسة كيف يتعامل تطبيق بنزين مع بيانات المستخدمين عند استخدام التطبيق.",
                style      = MaterialTheme.typography.bodyMedium,
                color      = AutoDriveText.Secondary,
                lineHeight = 22.sp
            )

            // ─── أقسام السياسة ─────────────────────────
            POLICY_SECTIONS.forEach { section ->
                PolicySectionCard(section = section)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySectionCard(section: PolicySection) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text       = section.title,
                style      = MaterialTheme.typography.titleMedium,
                color      = AutoDriveFinance.Pending,
                fontWeight = FontWeight.Bold
            )
            AutoDriveDivider()
            Text(
                text       = section.body,
                style      = MaterialTheme.typography.bodySmall,
                color      = AutoDriveText.Secondary,
                lineHeight = 22.sp
            )
        }
    }
}
