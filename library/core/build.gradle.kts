plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hypermax.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.hypermax\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
    }

    testOptions {
        // 版本水印的校验逻辑是不依赖 Android 的纯 Java 代码，用 JUnit 5 直接跑在 JVM 上
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // api(projects.library.hook)
    api(projects.library.common)
    api(projects.library.provision)
    api(libs.appiconloader)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
