buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.2")
    }
}

plugins {
    kotlin("jvm") version "1.3.70"
    application
}

application {
    mainClassName = "org.araqnid.libraryversions.Main"
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
            attributes["X-Service-Class"] = application.mainClassName
            attributes["Automatic-Module-Name"] = "org.araqnid.libraryversions"
        }
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api:${LibraryVersions.slf4j}")
    implementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    implementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:atomicfu:0.14.2")
    implementation("com.google.guava:guava:${LibraryVersions.guava}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("xom:xom:1.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.1")
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13")
    testImplementation("com.natpryce:hamkrest:${LibraryVersions.hamkrest}")
    testImplementation("org.mockito:mockito-core:3.2.4")
    testImplementation("com.timgroup:clocks-testing:1.0.1088")
    testImplementation("org.araqnid:hamkrest-json:1.1.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
    runtimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
    testRuntimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
}

pluginManager.apply("kotlinx-atomicfu")

the<kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension>().apply {
    variant = "VH"
}
