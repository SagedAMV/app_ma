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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * اقتراح 12 — الإعدادات 2027:
 * تبويبات علوية (عام / أمان / بيانات) بدل قائمة طويلة واحدة، بطاقات مجمعة
 * حسب الوظيفة، ومفاتيح M3 واضحة — أقل طولاً وأسرع وصولًا.
 */
@Composable
fun SettingsRedesign() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ترويسة المستخدم
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(ProposalPalette.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("س", color = ProposalPalette.OnPrimaryContainer, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ساجد الشريف", style = MaterialTheme.typography.titleMedium)
                    Text("العملة: ريال سعودي · نسخة 2.8.3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("تعديل", style = MaterialTheme.typography.labelMedium, color = ProposalPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }

        // التبويبات
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabOption("عام", Modifier.weight(1f), selected = true)
                TabOption("أمان", Modifier.weight(1f), selected = false)
                TabOption("بيانات", Modifier.weight(1f), selected = false)
            }
        }

        // قسم المظهر
        item {
            PropCard {
                Text("المظهر", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                SettingRow("🌙", "الوضع الداكن", "يتبع النظام") { SwitchM3(true) }
                SettingRow("🎨", "لون التطبيق", "بنفسجي كلاسيك") { Box(Modifier.size(18.dp).clip(CircleShape).background(ProposalPalette.Primary)) }
                SettingRow("🙈", "إخفاء الأرصدة", "عند فتح التطبيق") { SwitchM3(false) }
            }
        }

        // قسم العملة والأرقام
        item {
            PropCard {
                Text("العملة والحدود", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                SettingRow("💱", "العملة", "ريال سعودي (ر.س)") { Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SettingRow("🎯", "هدف الادخار الشهري", "١٠٬٠٠٠") { Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        // قسم الأمان
        item {
            PropCard {
                Text("الأمان", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                SettingRow("🔒", "قفل تبويب العملاء", "بصمة أو رمز الجهاز") { SwitchM3(true) }
                SettingRow("🧾", "سجل التعديلات", "آخر 3 تغييرات") { Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        // قسم البيانات
        item {
            PropCard {
                Text("البيانات", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                SettingRow("💾", "نسخة احتياطية JSON", "آخر نسخة: أمس 22:00") { Text("حفظ", color = ProposalPalette.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                SettingRow("📊", "تصدير CSV", "جدول Excel كامل") { Text("تصدير", color = ProposalPalette.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                SettingRow("📥", "استيراد نسخة", "من ملف JSON") { Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SettingRow("🗑", "مسح كل البيانات", "لا رجعة فيه") {
                    Text("مسح", color = ProposalColors.expense, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TabOption(label: String, modifier: Modifier = Modifier, selected: Boolean) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) ProposalPalette.Primary else Color.Transparent)
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
private fun SettingRow(icon: String, title: String, sub: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 14.sp) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** مفتاح M3 مبسط (رسم ثابت — الحالة تُمرر) */
@Composable
fun SwitchM3(checked: Boolean) {
    val thumbColor = if (checked) Color.White else MaterialTheme.colorScheme.outline
    val trackColor = if (checked) ProposalPalette.Primary else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor)
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(thumbColor))
    }
}
