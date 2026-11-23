group = "com.keopsfenks"
version = rootProject.version

val minecraftApiVersion: String by project

dependencies {
    compileOnly(project(":arcana-api"))
    compileOnly(project(":arcana-skills"))
    compileOnly("io.papermc.paper:paper-api:$minecraftApiVersion-R0.1-SNAPSHOT")
    testImplementation(project(":arcana-api"))
    testImplementation(project(":arcana-skills"))
    testImplementation("io.papermc.paper:paper-api:$minecraftApiVersion-R0.1-SNAPSHOT")
    testImplementation("org.yaml:snakeyaml:2.5")
}
