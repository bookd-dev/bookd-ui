import de.jensklingenberg.ktorfit.gradle.ErrorCheckingMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    // Apply Ktorfit before KSP so its processor can be wired to target-specific KMP configurations.
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.bookd.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }

        withHostTest {}

        packaging {
            resources {
                excludes.add("/META-INF/AL2.0")
                excludes.add("/META-INF/LGPL2.1")
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // SQLDelight Native driver needs SQLite.
            linkerOpts("-lsqlite3")
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.compose.material.icons.extended)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)

            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktorfit)

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.nav3.ui)
            implementation(libs.nav3.material3.adaptive)
            implementation(libs.nav3.lifecycle.viewmodel)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.nav3)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network)

            implementation(libs.settings.multiplatform)
            implementation(libs.settings.multiplatform.serialization)

            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.jvm)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.compose.ui.test.junit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)

    components {
        listOf(
            "coil-core-jvm",
            "coil-core-iosarm64",
            "coil-core-iossimulatorarm64"
        ).forEach { module ->
            withModule("io.coil-kt.coil3:$module") {
                allVariants {
                    withDependencies {
                        removeAll { it.group == "org.jetbrains.skiko" && it.name == "skiko" }
                        add("org.jetbrains.skiko:skiko:${libs.versions.skiko.get()}")
                    }
                }
            }
        }
    }
//    add("kspCommonMainMetadata", libs.koin.annotations.ksp)
//    add("kspAndroid", libs.koin.annotations.ksp)
//    add("kspIosArm64", libs.koin.annotations.ksp)
//    add("kspIosSimulatorArm64", libs.koin.annotations.ksp)
//    add("kspJvm", libs.koin.annotations.ksp)
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
    add("kspJvm", libs.ktorfit.ksp)
}

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
    arg("Ktorfit_Errors", ErrorCheckingMode.ERROR.ordinal.toString())
    arg("Ktorfit_QualifiedTypeName", "false")
}

ktorfit {
    errorCheckingMode = ErrorCheckingMode.ERROR
    compilerPluginVersion.set("2.3.4")
}

sqldelight {
    databases {
        create("Database") {
            generateAsync = true
//            deriveSchemaFromMigrations = true
            verifyMigrations = true

            packageName = "com.bookd.app"
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}

compose.resources {
    packageOfResClass = "app.composeapp.generated.resources"
}
