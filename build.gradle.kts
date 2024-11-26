plugins {
    kotlin("jvm") version "1.9.10" // Replace with your Kotlin version
}

allprojects {
    repositories {
        mavenCentral()
    }

    group = "com.example"
    version = "1.0.0"

    tasks.register<Copy>("copyResources") {
        from("resources")
        into("build/resources")
    }
}

