plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "cz.jeme"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    runServer {
        minecraftVersion("1.21.5")
    }

    shadowJar {
        archiveClassifier = ""
        enableRelocation = true
        relocationPrefix = "${project.group}.${project.name.lowercase()}.shaded"

        dependencies {
            exclude(dependency("org.jetbrains:annotations:.*"))
        }
    }

    assemble {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
