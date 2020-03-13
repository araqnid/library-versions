plugins {
    `java-library`
}

repositories {
    jcenter()
}

dependencies {
    implementation(project(":", configuration = "jvmDefault"))
}
