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


// Paper's static potion registry fixture belongs to a separate JVM; it must not alter
// the registry state seen by the ordinary engine compatibility tests.
tasks.named<Test>("test") { exclude("**/NativeContributionTests.class") }
val nativeContributionTest by tasks.registering(Test::class) {
    description = "Checks native attribute and potion lease ownership with an isolated Paper registry fixture"
    group = "verification"
    dependsOn(tasks.named("testClasses"))
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/NativeContributionTests.class")
    useJUnitPlatform()
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    testLogging { events("passed", "skipped", "failed") }
    shouldRunAfter(tasks.named("test"))
}
tasks.named("check") { dependsOn(nativeContributionTest) }
