import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hypermax.common"
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
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    api(libs.bundles.miuix)
    // libxposed API 102
    compileOnlyApi(libs.libxposed.api)
}
