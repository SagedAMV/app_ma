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
 * اقتراح 4 — الادخار 2027:
 * حصالة مركزية بمخطط تطور (Sparkline) مدمج، نسبة الهدف الشهري كحلقة،
 * وحاسبة ادخار مبسطة بخيارين (شهري/أسبوعي) داخل بطاقة واحدة.
 */
@Composable
fun SavingsRedesign() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // بطاقة الحصالة الرئيسية
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF00875A), ProposalPalette.Secondary)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("صندوق الادخار", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                            Text("٦٨٬٤٠٠", color = Color.White, style = MaterialTheme.typography.displaySmall)
                        }
                        Text("🐷", fontSize = 44.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    // شريط الهدف الشهري
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("هدف الشهر", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            Text("٦٬٠٠٠ / ١٠٬٠٠٠ (٦٠٪)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Sparkline — تطور المدخرات
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        listOf(0.3f, 0.4f, 0.35f, 0.5f, 0.62f, 0.58f, 0.7f, 0.75f, 0.68f, 0.82f).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((h * 40).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("إضافة", Modifier.weight(1f), icon = "＋", container = Color.White, contentColor = Color(0xFF00875A))
                        PillButton("سحب", Modifier.weight(1f), icon = "−", container = Color.White.copy(alpha = 0.18f), contentColor = Color.White)
                    }
                }
            }
        }

        // حاسبة الادخار
        item {
            PropCard {
                SectionHeader("حاسبة الادخار", "تطبيق")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("المبلغ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("١٠٠٬٠٠٠", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("المدة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("١٢ شهرًا", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcResult("شهريًا", "٨٬٣٣٣", selected = true, modifier = Modifier.weight(1f))
                    CalcResult("أسبوعيًا", "١٬٩٢٣", selected = false, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalcResult(label: String, value: String, selected: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) ProposalPalette.SecondaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Color(0xFF0A4A3C) else MaterialTheme.colorScheme.onSurface,
        )
    }
}
