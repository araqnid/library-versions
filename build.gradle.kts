plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("org.araqnid.libraryversions.JvmMainKt")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.let { it += "-parameters" }
        options.isIncremental = true
        options.isDeprecation = true
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    withType<Jar>().configureEach {
        manifest {
            attributes["Implementation-Title"] = "libraryversions"
            attributes["Implementation-Version"] = project.version
            attributes["Automatic-Module-Name"] = "org.araqnid.libraryversions"
        }
    }

    withType<Test> {
        val disableExternalIntegration: String? by project
        inputs.property("disableExternalIntegration", disableExternalIntegration ?: "")
        environment("DISABLE_EXTERNAL_INTEGRRATION", (disableExternalIntegration?.toBoolean() ?: false).toString())
    }
}

object LibraryVersions {
    const val slf4j = "1.7.36"
}

dependencies {
    constraints {
        implementation("xerces:xercesImpl") {
            version {
                require("2.12.2")
            }
        }
    }

    implementation("org.slf4j:slf4j-api") {
        version {
            strictly("[1.7, 1.8[")
            prefer(LibraryVersions.slf4j)
        }
    }

    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.1"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9")
    implementation("com.google.guava:guava:31.1-jre")
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    implementation("xom:xom:1.3.7")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.10.3"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.araqnid.kotlin.arg-parser:arg-parser:0.1.2")
    runtimeOnly("org.slf4j:slf4j-simple") {
        version {
            strictly("[1.7, 1.8[")
            prefer(LibraryVersions.slf4j)
        }
    }

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.timgroup:clocks-testing:1.0.1088")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
    testImplementation("org.araqnid.kotlin.assert-that:assert-that:0.1.1")
    testRuntimeOnly("org.slf4j:slf4j-simple") {
        version {
            strictly("[1.7, 1.8[")
            prefer(LibraryVersions.slf4j)
        }
    }
}
