package com.jtprince.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

object ChatUtils {
    fun commaSeparated(items: Collection<Component>): TextComponent {
        val builder = Component.text()
        val iter = items.iterator()
        while (iter.hasNext()) {
            val current = iter.next()
            builder.append(current)
            if (iter.hasNext()) {
                builder.append(Component.text(", "))
            }
        }
        return builder.build()
    }
}
