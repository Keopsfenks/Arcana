group = "com.keopsfenks"
version = rootProject.version

subprojects {
    dependencies {
        compileOnly(project(":arcana-api"))
    }
}
