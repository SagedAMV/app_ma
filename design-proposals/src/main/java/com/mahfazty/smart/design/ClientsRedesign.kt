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
 * اقتراح 5 — قائمة العملاء 2027:
 * ترويسة بحث + بطاقة إجماليّات (عليه/له) موحدة، فلاتر حالة كرقائق بعدّادات،
 * وصفوف عملاء بحالة ملوّنة واضحة ونقطة ديناميكية عند وجود ديون مستحقة.
 */
@Composable
fun ClientsRedesign() {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ترويسة البحث
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
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
                        "ابحث عن عميل أو حساب…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(ProposalPalette.Primary),
                contentAlignment = Alignment.Center,
            ) { Text("＋", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // بطاقة الإجماليات
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TotalsBox("عليه (مستحق لنا)", "٢٨٤٬٠٠٠", ProposalColors.expense, Modifier.weight(1.4f))
                    TotalsBox("له (ندين له)", "١٢٬٥٠٠", ProposalColors.income, Modifier.weight(1f))
                }
            }

            // فلاتر الحالة
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("الكل", 4, true)
                    StatusChip("نشط", 2, false)
                    StatusChip("موقوف", 1, false)
                    StatusChip("قائمة سوداء", 1, false)
                }
            }

            // صفوف العملاء
            items(ProposalData.clients) { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconAvatar(emoji = when (c.status) { 0 -> "🙂"; 1 -> "⏸"; else -> "⛔" }, size = 40.dp, container = when (c.status) {
                        0 -> MaterialTheme.colorScheme.surfaceVariant
                        1 -> ProposalPalette.TertiaryContainer
                        else -> ProposalPalette.ErrorContainer
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            if (c.status == 2) {
                                Text("● متأخر", fontSize = 9.sp, color = ProposalColors.expense, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(c.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        c.due,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (c.due.startsWith("عليه")) ProposalColors.expense else ProposalColors.income,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsBox(label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChip(label: String, count: Int, selected: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ProposalPalette.Primary else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", fontSize = 8.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}
