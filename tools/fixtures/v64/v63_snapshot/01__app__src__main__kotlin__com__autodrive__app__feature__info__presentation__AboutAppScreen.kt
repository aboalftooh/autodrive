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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightbulbCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability

@Composable
fun AboutAppScreen(
    competitionAvailability: CompetitionAvailability,
    onBack: () -> Unit,
) {
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
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "رجوع", tint = AutoDriveText.Primary)
                }
                Text(
                    text  = "عن التطبيق",
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
            // ─── هوية التطبيق ──────────────────────────
            Box(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AutoDriveSurface.Raised)
                    .border(1.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                contentAlignment  = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier          = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AutoDriveFinance.Withdrawable.copy(alpha = 0.12f))
                            .border(1.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        contentAlignment  = Alignment.Center
                    ) {
                        Text("⛽", fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = "بنزين",
                        style      = MaterialTheme.typography.displaySmall,
                        color      = AutoDriveFinance.Withdrawable,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "الإصدار 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutoDriveText.Secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text      = "تطبيق مخصص لمتابعة العمولات والمشتريات والفواتير الخاصة بالمسوّقين وأصحاب الورش المرتبطين بالمنصوري عون الله.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = AutoDriveText.Secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── ماذا يقدم التطبيق ─────────────────────
            SectionHeader("ما يقدمه التطبيق")

            val features = buildList {
                add(Triple(Icons.Rounded.Smartphone, "تسجيل آمن برقم الهاتف", "يدخل المستخدم رقم هاتفه، يستلم رمز تحقق OTP، ويستخدمه للدخول إلى التطبيق."))
                add(Triple(Icons.Rounded.Groups, "الانضمام بكود دعوة", "بعد تسجيل الهاتف، يحتاج المستخدم إلى كود دعوة صحيح للربط بحسابه داخل النظام."))
                add(Triple(Icons.Rounded.Person, "نوع الحساب", "يدعم التطبيق نوعين: مسوّق يجلب عملاء ويحصل على عمولة، وصاحب ورشة يرسل سيارات ومشتريات ويحصل على عمولة."))
                add(Triple(Icons.Rounded.BarChart, "متابعة العمولات", "إجمالي العمولات، القابلة للسحب، المعلّقة، المصروفة، والأسبوعية."))
                add(Triple(Icons.Rounded.Wallet, "الرصيد وطلبات السحب", "يمكن للمستخدم طلب سحب الرصيد المتاح ومتابعة حالة الطلب: قيد المعالجة، ناجح، مرفوض، مكتمل."))
                add(Triple(Icons.Rounded.Receipt, "الفواتير", "يعرض الفواتير المرتبطة بالمستخدم وتفاصيل كل فاتورة، بما فيها قيمة الفاتورة وقيمة العمولة."))
                when (competitionAvailability) {
                    CompetitionAvailability.DISABLED -> Unit
                    CompetitionAvailability.LOCKED -> add(
                        Triple(
                            Icons.Rounded.EmojiEvents,
                            "المسابقة الأسبوعية",
                            "ميزة تنافسية أسبوعية ستتوفر لاحقاً بعد اكتمال جاهزية المنافسة.",
                        )
                    )
                    CompetitionAvailability.ACTIVE -> {
                        add(Triple(Icons.Rounded.EmojiEvents, "المسابقة الأسبوعية", "ترتيب أسبوعي حسب قيمة المشتريات مع إخفاء أسماء المستخدمين الآخرين حفاظًا على الخصوصية."))
                        add(Triple(Icons.Rounded.Star, "شارة زعيم الأسبوع", "يعرض عدد المرات التي حصل فيها المستخدم على شارة زعيم الأسبوع مع إمكانية عرض الأسابيع التي فاز فيها."))
                    }
                }
                add(Triple(Icons.Rounded.Info, "عم دينمو", "شخصية تفاعلية تتغير حالتها حسب نشاط المستخدم الأسبوعي: مطفي، تعبان، صاحي، مولع، الأسطورة."))
                add(Triple(Icons.Rounded.LightbulbCircle, "النصائح الذكية", "نصائح ورسائل ذكية مرتبطة بالنشاط أو الفواتير أو تحسين فرص زيادة العمولة."))
                add(Triple(Icons.Rounded.ChatBubble, "الرسائل والإشعارات", "دعم الرسائل بين المستخدم والإدارة وإشعارات العمولات، الرسائل، وطلبات السحب."))
            }

            features.forEach { (icon, title, desc) ->
                FeatureItem(icon = icon, title = title, description = desc)
            }

            // ─── لمن صُمم التطبيق ─────────────────────
            SectionHeader("لمن صُمم التطبيق؟")

            Surface(
                shape  = RoundedCornerShape(16.dp),
                color  = AutoDriveSurface.Raised,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint               = AutoDriveFinance.Withdrawable,
                        modifier           = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text       = "صُمم تطبيق بنزين للمسوّقين وأصحاب الورش الذين يتعاملون مع المنصوري عون الله ويريدون متابعة عمولاتهم ومشترياتهم بشكل واضح ومنظم.",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = AutoDriveText.Primary,
                        lineHeight = 24.sp
                    )
                }
            }

            // ─── خلاصة التطبيق ────────────────────────
            SectionHeader("خلاصة التطبيق")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AutoDriveFinance.Pending.copy(alpha = 0.06f))
                    .border(1.dp, AutoDriveFinance.Pending.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text       = "بنزين ليس مجرد شاشة رصيد، بل نظام متابعة كامل للعمولات والنشاط الأسبوعي والفواتير والسحب والتواصل مع الإدارة، مع عنصر تحفيزي خفيف يجعل المستخدم يعرف وضعه ويتابع تقدمه أسبوعًا بعد أسبوع.",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = AutoDriveFinance.Pending,
                    lineHeight = 24.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.headlineSmall,
        color      = AutoDriveText.Primary,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AutoDriveFinance.Withdrawable.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AutoDriveFinance.Withdrawable, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AutoDriveText.Primary, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary, lineHeight = 20.sp)
            }
        }
    }
}
