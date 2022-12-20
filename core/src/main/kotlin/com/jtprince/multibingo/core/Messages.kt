package com.jtprince.multibingo.core

import com.jtprince.multibingo.core.automark.AutomatedSpace
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

object Messages {
    private fun sendWithHeader(a: Audience, component: Component) {
        // TODO: Need game code access here
        a.sendMessage(component)
    }

    fun Audience.bingoTellGoalProgress(space: AutomatedSpace, progress: Int, towards: Int) {
        val number = Component.text("$progress/$towards")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
        val component = Component.text("Progress: ")
            .append(number)
            .append(Component.text("  [${space.text}]"))
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC)
            .hoverEvent(HoverEvent.showText(Component.text(space.text)))
        sendWithHeader(this, component)
    }
}
