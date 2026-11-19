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
 * اقتراح 3 — الأهداف 2027:
 * بطاقة بطل لكل هدف: حلقة تقدم دائرية متحدة المركز مع المبلغ المتبقي
 * وعدد الأيام، مع أزرار إضافة/سحب مدمجة في البطاقة نفسها بدل الحوار.
 */
@Composable
fun GoalsRedesign() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("أهدافي", style = MaterialTheme.typography.titleLarge)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProposalPalette.Primary),
                    contentAlignment = Alignment.Center,
                ) { Text("＋", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }

        items(ProposalData.goals) { g ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // حلقة التقدم الدائرية
                        GoalRing(g.pct)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(g.icon, fontSize = 18.sp)
                                Text(g.name, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "ادخرت ${g.saved} من ${g.target}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "متبقٍ ${(g.pct * 100).toInt()}% · ${g.daysLeft} يومًا",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProposalPalette.Primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("إضافة", Modifier.weight(1f), icon = "＋", container = ProposalPalette.PrimaryContainer, contentColor = ProposalPalette.OnPrimaryContainer)
                        PillButton("سحب", Modifier.weight(1f), icon = "−", container = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text(
                "💡 اجعل إضافة صغيرة كل أسبوع — ٥٪ من الراتب تُقصّر مدة الهدف ٤٠٪",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** حلقة تقدم دائرية (Canvas) */
@Composable
private fun GoalRing(pct: Float) {
    val ringColor = when {
        pct >= 0.8f -> ProposalColors.gold
        else -> ProposalPalette.Primary
    }
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(56.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = ringColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            )
        }
        Text("${(pct * 100).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ringColor)
    }
}
