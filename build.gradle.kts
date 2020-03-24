plugins {
    kotlin("jvm") version "1.3.71"
    application
}

application {
    mainClassName = "org.araqnid.libraryversions.JvmMainKt"
}

repositories {
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
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

dependencies {
    implementation("org.slf4j:slf4j-api:${LibraryVersions.slf4j}")
    implementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    implementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:${LibraryVersions.kotlinCoroutines}")
    implementation("com.google.guava:guava:${LibraryVersions.guava}")
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    implementation("xom:xom:1.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${LibraryVersions.jackson}")
    runtimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13")
    testImplementation("com.timgroup:clocks-testing:1.0.1088")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${LibraryVersions.kotlinCoroutines}")
    testRuntimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
}
