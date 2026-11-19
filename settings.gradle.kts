pluginManagement {
    repositories {
        // مرايا Huawei/Tencent أولًا — mavenCentral يرد أحيانًا بـ 429 (حد معدل)
        // من بيئة البناء السحابية، ومرايا Aliyun كانت ترد 502 تحت حِمل طلبات Gradle
        maven("https://repo.huaweicloud.com/repository/maven/")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.huaweicloud.com/repository/maven/")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
    }
}

rootProject.name = "Mahfazty"
include(":app")
include(":design-proposals")
