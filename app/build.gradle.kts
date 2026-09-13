plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mahfazty.smart"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mahfazty.smart"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "2.8.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// إصلاح A1: تصدير مخططات Room إلى app/schemas — أساس كتابة ترحيلات صحيحة عند أي تغيير مستقبلي
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // الأساسيات
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose (مُدار عبر BOM — إصدارات متوافقة تلقائياً)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // دورة الحياة والتنقل
    implementation(libs.androidx.activity.compose)

    // قفل تبويب العملاء (بصمة/رمز الجهاز) — إصلاح sec-2
    implementation(libs.androidx.biometric)
    // إضافة 3.4 من تقرير الفحص: تذكير يومي بالديون المستحقة (WorkManager)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // قاعدة البيانات Room + معالج KSP
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
