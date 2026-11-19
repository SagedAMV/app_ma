package com.mahfazty.smart.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * اقتراح 6 — نافذة إضافة عملية 2027 (ModalBottomSheet):
 * نوع العملية كمفتاح مقسّم كبير (مصروف/دخل) بألوان دلالية فورية، حقل مبلغ
 * بارز بمسطرة أرقام عربية، اختيار صندوق كراديو-حبوب، وفئات FlowRow مضغوطة.
 */
@Composable
fun AddTransactionSheetRedesign() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // مقبض السحب
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))

        Text("عملية جديدة", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // مفتاح النوع المقسّم
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentOption("💸 مصروف", Modifier.weight(1f), selected = true, selectedColor = ProposalColors.expense)
            SegmentOption("💼 دخل", Modifier.weight(1f), selected = false, selectedColor = ProposalColors.income)
        }
        Spacer(Modifier.height(12.dp))

        // المبلغ البارز
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
                    Text("١٬٢٥٠", style = MaterialTheme.typography.headlineMedium)
                    Text("ر.س", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // الصندوق
        Text("الصندوق", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WalletChoice("💵 كاش", "٨٬١٢٠ متاح", selected = true, modifier = Modifier.weight(1f))
            WalletChoice("🏦 بنك", "٤٥٬٢٣٠ متاح", selected = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // الفئات
        Text("الفئة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProposalData.expenseCats.forEach { (label, _) ->
                CategoryChip(label, selected = label.startsWith("🍔"))
            }
        }
        Spacer(Modifier.height(12.dp))

        // زر الحفظ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrushPrimary())
                ,
            contentAlignment = Alignment.Center,
        ) { Text("حفظ العملية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable
private fun BrushPrimary() = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(ProposalPalette.Primary, ProposalPalette.PrimaryDeep)
)

@Composable
private fun SegmentOption(label: String, modifier: Modifier = Modifier, selected: Boolean, selectedColor: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WalletChoice(label: String, sub: String, selected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ProposalPalette.PrimaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape)
                .background(if (selected) ProposalPalette.Primary else Color.Transparent)
                .borderRing(selected),
        )
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.borderRing(selected: Boolean): Modifier = if (selected) this else this.border(
    androidx.compose.foundation.BorderStroke(1.5.dp, ProposalPalette.OutlineL), CircleShape
)

@Composable
private fun CategoryChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ProposalPalette.PrimaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ProposalPalette.OnPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}
