package com.keopsfenks.arcana.skills.compile

class SkillCompileException(val diagnostics: List<String>) : IllegalArgumentException(diagnostics.joinToString("\n"))
