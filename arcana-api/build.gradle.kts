val minecraftApiVersion: String by project

dependencies {
    // Adventure
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")

    // Other
    compileOnly("org.spigotmc:spigot-api:$minecraftApiVersion-R0.1-SNAPSHOT")
}

group = "com.keopsfenks"
version = rootProject.version

java {
    withJavadocJar()
}

tasks {
    build {
        dependsOn(publishToMavenLocal)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["java"])
            artifactId = "arcana-api"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/keopsfenks/arcana")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
