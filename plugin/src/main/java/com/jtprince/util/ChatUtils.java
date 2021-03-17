package com.jtprince.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Collection;
import java.util.Iterator;

public class ChatUtils {
    public static TextComponent commaSeparated(Collection<TextComponent> items) {
        TextComponent.Builder builder = Component.text();

        Iterator<TextComponent> iter = items.iterator();
        while (iter.hasNext()) {
            TextComponent current = iter.next();
            builder.append(current);
            if (iter.hasNext()) {
                builder.append(Component.text(", "));
            }
        }

        return builder.build();
    }
}
