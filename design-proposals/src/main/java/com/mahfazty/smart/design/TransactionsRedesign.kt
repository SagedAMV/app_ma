package com.mahfazty.smart.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * اقتراح 2 — سجل العمليات 2027:
 * ترويسة بحث موحدة داخل بطاقة مع عدادات سريعة قابلة للنقر، فلاتر فئات
 * كرقائق أفقية مرنة، وقائمة مجمعة بالتاريخ مع رصيد جارٍ ظاهر لكل عملية.
 */
@Composable
fun TransactionsRedesign() {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ترويسة البحث
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 14.sp)
                    Text(
                        "ابحث في العمليات والفئات…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProposalPalette.Primary),
                contentAlignment = Alignment.Center,
            ) { Text("＋", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // عدادات سريعة
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickStat("اليوم", "٦ عمليات", selected = true, modifier = Modifier.weight(1f))
                    QuickStat("الأسبوع", "٢٤ عملية", selected = false, modifier = Modifier.weight(1f))
                    QuickStat("الكل", "٤٧٤", selected = false, modifier = Modifier.weight(1f))
                }
            }

            // فلاتر الفئات
            item {
                androidx.compose.foundation.lazy.LazyRow {
                    item { FilterChip("الكل", true) }
                    item { Spacer(Modifier.width(6.dp)) }
                    item { FilterChip("مصاريف", false) }
                    item { Spacer(Modifier.width(6.dp)) }
                    item { FilterChip("دخل", false) }
                    item { Spacer(Modifier.width(6.dp)) }
                    item { FilterChip("أهداف", false) }
                }
            }

            // تجميع بالتاريخ
            item {
                Spacer(Modifier.height(2.dp))
                DayHeader("اليوم — ١٩ نوفمبر", "دخل ٤٠٬٥٠٠ · مصروف ١٬٢٥٠")
            }
            items(ProposalData.recentTxs.take(2)) { TxListRow(it, showBalance = true) }

            item { DayHeader("أمس — ١٨ نوفمبر", "دخل ٢٬٠٠٠ · مصروف ٣٬٩٥٠") }
            items(ProposalData.recentTxs.drop(2).take(2)) { TxListRow(it, showBalance = true) }
        }
    }
}


@Composable
private fun DayHeader(day: String, summary: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


/** صف عملية كامل داخل سجل العمليات */
@Composable
fun TxListRow(tx: ProposalData.Tx, showBalance: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAvatar(tx.icon, size = 38.dp, container = MaterialTheme.colorScheme.surfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.titleSmall)
            Text(
                tx.category + if (showBalance) " · بعدها ${tx.balanceAfter}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                tx.amount,
                style = MaterialTheme.typography.titleSmall,
                color = when (tx.sign) { 1 -> ProposalColors.income; -1 -> ProposalColors.expense; else -> ProposalPalette.Primary },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** رقاقة فلتر */
@Composable
private fun FilterChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) ProposalPalette.Primary else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** عداد سريع قابل للنقر */
@Composable
private fun QuickStat(label: String, value: String, selected: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ProposalPalette.PrimaryContainer else MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) ProposalPalette.OnPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}
