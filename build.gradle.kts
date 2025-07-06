plugins {
    alias(libs.plugins.kotlin.plugin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.userdev)
}

group = "cz.jeme"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.get())
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

val minecraftVersion = libs.versions.paper.get().substringBefore('-')

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
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
