plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
}
android {
    namespace = "io.music_assistant.sendspin"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:atomicfu:0.33.0")
    api("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-websockets:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("co.touchlab:kermit:2.1.0")
    implementation("dev.whyoleg.cryptography:cryptography-core:0.6.0")
    implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:0.6.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.85.2")
    implementation("io.github.jaredmdobson:concentus:1.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("io.ktor:ktor-client-mock:3.5.2")
}
