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

private val DayLabels = listOf("سبت", "أحد", "اثن", "ثلا", "أرب", "خمي", "جمع")

/**
 * اقتراح 1 — الرئيسية 2027:
 * بطاقة ثروة موحدة تحتضن (كاش + بنك + الإجمالي) بدل عناصر متفرقة،
 * أزرار الإجراءات الرئيسية مدمجة داخل البطاقة (تحويل/إضافة)، مخطط أسبوعي
 * بأعمدة دخل/مصروف مزدوجة، وأهداف مصغرة تُظهر التقدم اللحظي.
 */
@Composable
fun HomeRedesign() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ترويسة الترحيب
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("صباح الخير، ساجد 👋", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "إليك ملخص محفظتك اليوم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(ProposalPalette.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("س", color = ProposalPalette.OnPrimaryContainer, fontWeight = FontWeight.ExtraBold) }
            }
        }

        // بطاقة الثروة الموحدة
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(ProposalPalette.Primary, ProposalPalette.PrimaryDeep))
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "إجمالي الثروة",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        ProposalData.totalWealth,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniBalance("💵 كاش", "٨٬١٢٠", Modifier.weight(1f))
                        MiniBalance("🏦 بنك", "٤٥٬٢٣٠", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            "إضافة عملية",
                            Modifier.weight(1f),
                            icon = "＋",
                            container = Color.White,
                            contentColor = ProposalPalette.PrimaryDeep,
                        )
                        PillButton(
                            "تحويل بنك↔كاش",
                            Modifier.weight(1f),
                            icon = "⇄",
                            container = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White,
                        )
                    }
                }
            }
        }

        // لافتة تذكير ذكية
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProposalPalette.TertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("💡", fontSize = 18.sp)
                Text(
                    "دين مستحق على «مؤسسة الأمانة» منذ 5 أيام — أرسل تذكيرًا؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5A430C),
                    modifier = Modifier.weight(1f),
                )
                Text("تذكير", color = ProposalPalette.Tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // إحصاءات الشهر
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("دخل الشهر", "٣٨٬٥٠٠", ProposalColors.income, Modifier.weight(1f))
                StatPill("مصروف الشهر", "٢١٬٣٠٠", ProposalColors.expense, Modifier.weight(1f))
                StatPill("ادخار الشهر", "٦٬٠٠٠", ProposalColors.gold, Modifier.weight(1f))
            }
        }

        // نشاط الأسبوع
        item {
            PropCard {
                SectionHeader("نشاط الأسبوع", "التفاصيل")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ProposalData.weekly.forEach { (inc, exp) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((inc * 1.1f).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ProposalColors.income)
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((exp * 0.9f).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ProposalColors.expense.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayLabels.forEach { d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(ProposalColors.income, "دخل")
                    Dot(ProposalColors.expense.copy(alpha = 0.7f), "مصروف")
                }
            }
        }

        // أهداف مصغرة
        item { SectionHeader("أهدافك", "الكل") }
        items(ProposalData.goals) { g ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAvatar(g.icon, size = 34.dp, container = ProposalPalette.PrimaryContainer)
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${(g.pct * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = ProposalPalette.Primary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    SegmentedProgress(pct = g.pct)
                }
            }
        }

        // آخر العمليات
        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader("آخر العمليات", "الكل")
            Spacer(Modifier.height(8.dp))
        }
        items(ProposalData.recentTxs.take(3)) { tx -> TxMiniRow(tx) }
    }
}

@Composable
private fun Dot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniBalance(label: String, amount: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(10.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(amount, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun StatPill(label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        Text(amount, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun TxMiniRow(tx: ProposalData.Tx) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAvatar(tx.icon, size = 34.dp, container = MaterialTheme.colorScheme.surfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.titleSmall)
            Text(tx.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            tx.amount,
            style = MaterialTheme.typography.titleSmall,
            color = when (tx.sign) { 1 -> ProposalColors.income; -1 -> ProposalColors.expense; else -> ProposalPalette.Primary },
            fontWeight = FontWeight.Bold,
        )
    }
}
