package com.mahfazty.smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.theme.LocalAppColors

/**
 * قفل تبويب العملاء (إصلاح sec-2).
 *
 * شاشة تحجب بيانات العملاء حتى يتحقق المستخدم بالبصمة أو رمز الجهاز.
 * التحقق نفسه (BiometricPrompt) يديره MainViewModel عبر [onUnlock] —
 * فلا تُعرض الشاشة إلا بعد نجاح التحقق، ولا قفل على جهاز بلا وسيلة تحقق.
 */
@Composable
fun ClientLockScreen(
    /** يستقبل دالة خطأ اختيارية ويُطلق نافذة التحقق */
    onUnlock: (onError: (String) -> Unit) -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { Text("🔒", fontSize = 48.sp) }
            Spacer(Modifier.height(20.dp))
            Text("تبويب العملاء مقفل", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "بيانات العملاء حساسة (أرصدة، ديون، هاتف) — ثبّت بصمتك أو أدخل رمز الجهاز لعرضها.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = {
                    error = null
                    onUnlock { error = it }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("🔓 فتح بالتعرف", fontSize = 15.sp) }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    error.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.red,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
