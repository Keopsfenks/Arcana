# Contributing to Arcana

Open issues and pull requests at https://github.com/Keopsfenks/Arcana. Target the `master` branch and describe the change and its validation.

Public APIs belong in `arcana-api` and must be accessible from Java. Kotlin extensions should expose the same capabilities. Backend implementation belongs in `arcana-core` and is written in Kotlin, following existing conventions.

Use `com.keopsfenks.arcana` for owned packages and Arcana-prefixed framework types. Preserve real third-party integration identifiers and attribution. Keep plugin metadata, reflection strings, shaded relocations, configuration keys, and documentation consistent.

Run `./gradlew build` (Windows: `.\gradlew.bat build`) before submitting changes. JDK 25 and network access are required; Gradle resolves the Java 21 toolchain for older server modules.
