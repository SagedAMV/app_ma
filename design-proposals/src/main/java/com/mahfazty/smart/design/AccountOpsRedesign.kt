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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * اقتراح 11 — شاشة عمليات حساب العميل 2027 (AccountOps):
 * بطاقة رصيد مزدوجة (حقيقي/عادي) كترويسة، شريط إجراءات أفقي مصغر (شحن/سحب/تحويل/
 * تسوية/سجل)، فلاتر كبسولات واضحة، وصفوف عمليات بشارات فواتير والرصيد الجاري.
 */
@Composable
fun AccountOpsRedesign() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ترويسة العميل
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(ProposalPalette.PrimaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Text("م", color = ProposalPalette.OnPrimaryContainer, fontWeight = FontWeight.ExtraBold) }
                    Column {
                        Text("محمد ماطور الحي", style = MaterialTheme.typography.titleMedium)
                        Text("حساب أساسي · أرصدة محدثة الآن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // بطاقة الرصيد المزدوجة
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ProposalPalette.PrimaryDeep, ProposalPalette.Primary)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("الرصيد الحقيقي (رأس مالنا)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text("٥٠٬٠٠٠", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("رصيد العمليات (عليه)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text("٤٥٬٠٠٠", color = ProposalColors.gold, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // شريط إجراءات مصغر
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActChip("⚡ شحن", Modifier.weight(1f))
                        ActChip("− سحب", Modifier.weight(1f))
                        ActChip("⇄ تحويل", Modifier.weight(1f))
                        ActChip("⚖ تسوية", Modifier.weight(1f))
                    }
                }
            }
        }

        // فلاتر كبسولات
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill("الكل", true)
                FilterPill("فواتير 🧾", false)
                FilterPill("مستحق", false)
                FilterPill("٣٠ يوم", false)
            }
        }

        // عمليات مع الرصيد الجاري
        item { DayHeaderShared("نوفمبر 2026", "٧ عمليات") }
        items(ProposalData.accountOps) { op ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAvatar(op.icon, size = 38.dp, container = MaterialTheme.colorScheme.surfaceVariant)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(op.title, style = MaterialTheme.typography.titleSmall)
                        if (op.badge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (op.invoiceUndelivered) ProposalColors.expense.copy(alpha = 0.12f) else ProposalPalette.TertiaryContainer)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(op.badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (op.invoiceUndelivered) ProposalColors.expense else Color(0xFF5A430C))
                            }
                        }
                    }
                    Text(op.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        op.amount,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (op.sign == 1) ProposalColors.income else ProposalColors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("بعدها: ${op.runningBalance}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ProposalPalette.Primary else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DayHeaderShared(title: String, sub: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
