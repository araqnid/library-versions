import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform") version "1.3.70"
    application
}

application {
    mainClassName = "org.araqnid.libraryversions.Main"
}

repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlin-js-wrappers")
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
            attributes["X-Service-Class"] = application.mainClassName
            attributes["Automatic-Module-Name"] = "org.araqnid.libraryversions"
        }
    }
}

kotlin {
    jvm()
    js {
        nodejs {

        }
    }

    sourceSets["jvmMain"].dependencies {
        implementation("org.slf4j:slf4j-api:${LibraryVersions.slf4j}")
        implementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
        implementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
        implementation("com.google.guava:guava:${LibraryVersions.guava}")
        api(kotlin("stdlib-jdk8"))
        api(kotlin("reflect"))
        implementation("xom:xom:1.3.4")
        implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.1")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${LibraryVersions.jackson}")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${LibraryVersions.jackson}")
        runtimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
    }

    sourceSets["jvmTest"].dependencies {
        implementation(kotlin("test-junit"))
        implementation("junit:junit:4.13")
        implementation("org.mockito:mockito-core:3.2.4")
        implementation("com.timgroup:clocks-testing:1.0.1088")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
        runtimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
    }

    sourceSets["jsMain"].dependencies {
        implementation(kotlin("stdlib-js"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.4")
        implementation(npm("axios", "0.19.2"))
        implementation(npm("xml2js", "0.4.23"))
        implementation("org.jetbrains:kotlin-extensions:1.0.1-pre.93-kotlin-1.3.70")
    }

    sourceSets["jsTest"].dependencies {
        implementation(kotlin("test-js"))
    }

    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib-common"))
    }

    sourceSets["commonTest"].dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
    }
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
