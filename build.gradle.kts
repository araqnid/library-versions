import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.internal.file.CompositeFileCollection
import org.gradle.api.internal.file.collections.FileCollectionResolveContext
import org.gradle.api.internal.tasks.TaskDependencyResolveContext
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform") version "1.3.70"
    application
}

application {
    mainClassName = "org.araqnid.libraryversions.JvmMainKt"
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
            attributes["Automatic-Module-Name"] = "org.araqnid.libraryversions"
        }
    }
}

kotlin {
    jvm()
    js {
        nodejs {
            runTask {
                args = listOf(file("runIt.js").toString(), file("resolvers.txt").toString())
            }
        }
    }

    val hostTarget = when (val hostOs = System.getProperty("os.name")) {
        "Mac OS X" -> macosX64("native")
        "Linux" -> linuxX64("native")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        val main by compilations.getting
        val curl by main.cinterops.creating {

        }
        val libxml2 by main.cinterops.creating {

        }

        binaries {
            executable {
                entryPoint = "org.araqnid.libraryversions.main"
            }
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
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.3.4")
    }

    sourceSets["commonTest"].dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
    }

    sourceSets["nativeMain"].dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.4")
    }

    sourceSets["nativeTest"].dependencies {
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

tasks.named("run", JavaExec::class.java).configure {
    val jvmMainCompileOutput = object : AbstractFileCollection() {
        override fun getFiles() = setOf(
                file("build/classes/kotlin/jvm/main"),
                file("build/processedResources/jvm/main")
        )

        override fun visitDependencies(context: TaskDependencyResolveContext) {
            super.visitDependencies(context)
            context.add(tasks["jvmMainClasses"])
            context.add(tasks["jvmProcessResources"])
        }

        override fun getDisplayName() = "Kotlin JVM classes output"
    }
    classpath = object : CompositeFileCollection() {
        override fun visitContents(context: FileCollectionResolveContext) {
            context.add(configurations["jvmRuntimeClasspath"])
            context.add(jvmMainCompileOutput)
        }

        override fun getDisplayName() = "Kotlin JVM main classpath"
    }
}
