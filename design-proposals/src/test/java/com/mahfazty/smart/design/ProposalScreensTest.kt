package com.mahfazty.smart.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.core.view.WindowCompat
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * التقاط صور التصاميم المقترحة — المواصفات الإلزامية:
 *  · Robolectric SDK 35 + Roborazzi
 *  · محرك الرسم Native Skia (RNG) — بلا محاكي
 *  · 640dp × 360dp عمودي، كثافة mdpi (الصورة 360×640 بكسل)
 *  · عربي RTL عبر مؤهلات ar-ldrtl + CompositionLocal
 *
 * تشغيل: ./gradlew :design-proposals:recordRoborazziDebug
 * الصور: مجلد build/outputs/roborazzi داخل الوحدة (ملفات PNG)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-ldrtl-w360dp-h640dp-mdpi")
class ProposalScreensTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ProposalCaptureActivity>()

    private fun capture(fileName: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.runOnUiThread {
            val window = composeRule.activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        composeRule.setContent {
            ProposalTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DeviceFrame { content() }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$fileName")
    }

    // ===== الشاشات =====

    @Test fun homeLight() = capture("01_home_light.png") { HomeRedesign() }

    @Test fun homeDark() = capture("02_home_dark.png", darkTheme = true) { HomeRedesign() }

    @Test fun transactions() = capture("03_transactions.png") { TransactionsRedesign() }

    @Test fun goals() = capture("04_goals.png") { GoalsRedesign() }

    @Test fun savings() = capture("05_savings.png") { SavingsRedesign() }

    @Test fun clients() = capture("06_clients.png") { ClientsRedesign() }

    @Test fun accountOps() = capture("07_account_ops.png") { AccountOpsRedesign() }

    @Test fun settings() = capture("08_settings.png") { SettingsRedesign() }

    // ===== نوافذ الإجراءات =====

    @Test fun addTransactionSheet() = capture("09_add_tx_sheet.png") { AddTransactionSheetRedesign() }

    @Test fun transferSheet() = capture("10_transfer_sheet.png") { TransferSheetRedesign() }

    @Test fun operationSheet() = capture("11_operation_sheet.png") { OperationSheetRedesign() }

    @Test fun insufficientSheet() = capture("12_insufficient_sheet.png") { InsufficientSheetRedesign() }

    @Test fun statementOptions() = capture("13_statement_options.png") { StatementOptionsRedesign() }
}
