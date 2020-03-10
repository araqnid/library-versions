import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js")
}

kotlin {
    target {
        nodejs {
        }
    }
    sourceSets["main"].dependencies {
    }
}

repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlin-js-wrappers")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.4")
    implementation(npm("axios", "0.19.2"))
    implementation(npm("xml2js", "0.4.23"))
    implementation("org.jetbrains:kotlin-extensions:1.0.1-pre.93-kotlin-1.3.70")
    testImplementation(kotlin("test-js"))
}

val compileKotlinJs: Kotlin2JsCompile by tasks
compileKotlinJs.kotlinOptions {
    moduleKind = "commonjs"
}

val compileTestKotlinJs: Kotlin2JsCompile by tasks
compileTestKotlinJs.kotlinOptions {
    moduleKind = "commonjs"
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions {
    }
}
