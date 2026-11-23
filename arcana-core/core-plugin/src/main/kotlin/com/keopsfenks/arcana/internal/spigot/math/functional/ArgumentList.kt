package com.keopsfenks.arcana.internal.spigot.math.functional

import com.keopsfenks.arcana.internal.spigot.math.token.Token
import com.keopsfenks.arcana.internal.spigot.math.token.TokenType
import com.keopsfenks.arcana.internal.spigot.math.token.Value

class ArgumentList(private val arguments: Array<Value>) : Token {
    override fun getType() = TokenType.ARGUMENT_LIST
    fun getArguments() = arguments
}