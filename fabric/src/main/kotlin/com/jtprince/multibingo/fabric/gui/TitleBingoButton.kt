package com.jtprince.multibingo.fabric.gui

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class TitleBingoButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    text: Component,
    onPress: (button: Button) -> Unit
) : Button(x, y, width, height, text, onPress)
