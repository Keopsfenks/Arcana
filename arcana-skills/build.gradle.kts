group = "com.keopsfenks"
version = rootProject.version

val minecraftApiVersion: String by project

dependencies {
    compileOnly(project(":arcana-api"))
    compileOnly("io.papermc.paper:paper-api:$minecraftApiVersion-R0.1-SNAPSHOT")
    compileOnly("org.yaml:snakeyaml:2.5")
    compileOnly("com.google.code.gson:gson:2.8.9")

    testImplementation(project(":arcana-api"))
    testImplementation(project(":arcana-skills-components"))
    testImplementation("io.papermc.paper:paper-api:$minecraftApiVersion-R0.1-SNAPSHOT")
    testImplementation("org.yaml:snakeyaml:2.5")
    testImplementation("com.google.code.gson:gson:2.8.9")
}
