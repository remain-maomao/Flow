import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.jna.core)
    implementation(libs.jna.platform)
    implementation(libs.java.websocket)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "org.example.flow.MainKt"
        jvmArgs += "-Dfile.encoding=GBK"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Flow"
            packageVersion = "0.1.0"
            vendor = "Flow"
            description = "Flow - 专注助手，监测工作/娱乐模式并智能提醒"
            windows {
                menuGroup = "Flow"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }
        }
    }
}