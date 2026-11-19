package com.mahfazty.smart.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * اقتراح 7 — نافذة تحويل بنك↔كاش 2027:
 * اتجاه التحويل كبطاقة تفاعلية بصرية (سهم متحرك بين الصندوقين) بدل قائمة منسدلة،
 * المبلغ البارز، وتأكيد مباشر مع تحذير الرصيد إن لزم.
 */
@Composable
fun TransferSheetRedesign() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Text("تحويل بين الصناديق", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))

        // بطاقة الاتجاه المرئية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionCard("🏦", "من البنك", "٤٥٬٢٣٠", Modifier.weight(1f), tint = ProposalPalette.Primary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⇄", fontSize = 20.sp, color = ProposalPalette.Primary, fontWeight = FontWeight.Bold)
                Text("تحويل", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DirectionCard("💵", "إلى الكاش", "٨٬١٢٠", Modifier.weight(1f), tint = ProposalColors.income)
        }
        Spacer(Modifier.height(14.dp))

        // المبلغ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column {
                Text("المبلغ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("٥٬٠٠٠", style = MaterialTheme.typography.headlineMedium)
                    Text("ر.س", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // أزرار سريعة
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("١٬٠٠٠", "٢٬٠٠٠", "٥٬٠٠٠", "الكل").forEach { q ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(q, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // تحذير الرصيد
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ProposalPalette.ErrorContainer)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("⚠️", fontSize = 14.sp)
            Text(
                "رصيد البنك بعد التحويل: ٤٠٬٢٣٠ — سليم ✓",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5C1F22),
            )
        }
        Spacer(Modifier.height(12.dp))

        // زر التنفيذ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(ProposalPalette.Primary, ProposalPalette.PrimaryDeep))),
            contentAlignment = Alignment.Center,
        ) { Text("تنفيذ التحويل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable
private fun DirectionCard(emoji: String, label: String, amount: String, modifier: Modifier = Modifier, tint: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * اقتراح 8 — نافذة عملية عميل 2027 (عليه/له + فاتورة بالمواد):
 * نوع العملية كقطبين متقابلين بلونين دلاليين، مواد الفاتورة كسطور قابلة للإضافة
 * مع حساب تلقائي للإجمالي، ومفتاح فاتورة مع مرجعها وتاريخ استحقاقها.
 */
@Composable
fun OperationSheetRedesign() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Text("عملية على حساب «ماطور الحي»", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // قطبا عليه/له
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProposalColors.expense.copy(alpha = 0.10f))
                    .border(1.dp, ProposalColors.expense.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("⬆ عليه (دين لنا)", color = ProposalColors.expense, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProposalColors.income.copy(alpha = 0.10f))
                    .border(1.dp, ProposalColors.income.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("⬇ له (ندين له)", color = ProposalColors.income, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))

        // المبلغ والمواد
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("المبلغ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("٢٣٬٠٠٠", style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("من المواد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("٢ بند · إجمالي ٢٣٬٠٠٠", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // مواد الفاتورة
        MaterialLine("🛢️", "دبب بترول", "٥ × ٤٬٦٠٠", "٢٣٬٠٠٠")
        Spacer(Modifier.height(6.dp))
        MaterialLine("⚡", "سلك لف + عدة", "١ × ٥٠٠", "٥٠٠")
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("＋", color = ProposalPalette.Primary, fontWeight = FontWeight.Bold)
            Text("إضافة مادة للفاتورة", style = MaterialTheme.typography.labelMedium, color = ProposalPalette.Primary)
        }
        Spacer(Modifier.height(10.dp))

        // فاتورة + استحقاق
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(ProposalPalette.TertiaryContainer).padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🧾", fontSize = 14.sp)
                Text("فاتورة #٢٠٢٦-٤٤", style = MaterialTheme.typography.labelMedium, color = Color(0xFF5A430C))
            }
            Text("استحقاق: ٢٥ نوفمبر", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5A430C))
        }
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(ProposalColors.expense, Color(0xFFC93A3F)))),
            contentAlignment = Alignment.Center,
        ) { Text("حفظ العملية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable
private fun MaterialLine(icon: String, name: String, qty: String, total: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAvatar(icon, size = 30.dp, container = MaterialTheme.colorScheme.surfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(qty, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(total, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * اقتراح 9 — ورقة الرصيد غير الكافي 2027:
 * توضيح العجز بصريًا (متاح مقابل مطلوب) + اقتراحات سحب ذكية من مصادر
 * (ادخار/أهداف) بأشرطة نسب جاهزة للنقر — نفس منطق InsufficientSheet الحالي.
 */
@Composable
fun InsufficientSheetRedesign() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Text("الرصيد غير كافٍ", style = MaterialTheme.typography.titleLarge, color = ProposalColors.expense)
        Spacer(Modifier.height(10.dp))

        // مقارنة العجز
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp)
            ) {
                Text("متاح في الكاش", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("٨٬١٢٠", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProposalColors.expense.copy(alpha = 0.10f))
                    .padding(10.dp)
            ) {
                Text("المطلوب", style = MaterialTheme.typography.labelSmall, color = ProposalColors.expense)
                Text("١٢٬٠٠٠", style = MaterialTheme.typography.titleLarge, color = ProposalColors.expense)
            }
        }
        Spacer(Modifier.height(8.dp))
        // شريط العجز
        Row(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(ProposalColors.expense.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxWidth().height(10.dp)
                    .background(ProposalColors.expense)
            )
        }
        Text("العجز: ٣٬٨٨٠", style = MaterialTheme.typography.labelSmall, color = ProposalColors.expense, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // مصادر الاقتراح
        Text("اسحب من:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        ProposalData.smartSources.forEach { (src, amt, pct) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAvatar(emoji = src.substringBefore(" "), size = 34.dp, container = MaterialTheme.colorScheme.surfaceVariant)
                Column(modifier = Modifier.weight(1f)) {
                    Text(src.substringAfter(" "), style = MaterialTheme.typography.titleSmall)
                    SegmentedProgress(pct = pct, segments = 12)
                }
                Text("سحب", style = MaterialTheme.typography.labelMedium, color = ProposalPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * اقتراح 10 — خيارات كشف الحساب 2027:
 * أزرار كبيرة واضحة (واتساب/CSV) مع وصف مسار كل خيار بدل نص طويل.
 */
@Composable
fun StatementOptionsRedesign() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Text("كشف حساب «ماطور الحي»", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("٧ عمليات · رصيده الآن عليه ٤٥٬٠٠٠", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        StatementOption("💬", "إرسال واتساب — آخر 10", "كشف نصي مختصر للعميل", ProposalColors.income)
        Spacer(Modifier.height(8.dp))
        StatementOption("📄", "كشف كامل واتساب", "كل العمليات بالرصيد الجاري", ProposalPalette.Primary)
        Spacer(Modifier.height(8.dp))
        StatementOption("📊", "تصدير CSV (Excel)", "جدول كامل للعمليات والفواتير", ProposalPalette.Tertiary)
    }
}

@Composable
private fun StatementOption(icon: String, title: String, sub: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAvatar(icon, size = 38.dp, container = tint.copy(alpha = 0.15f))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("‹", color = tint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
