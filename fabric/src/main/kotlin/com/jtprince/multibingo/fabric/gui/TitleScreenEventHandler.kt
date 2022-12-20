package com.jtprince.multibingo.fabric.gui

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents

object TitleScreenEventHandler {
    fun register() {
        ScreenEvents.AFTER_INIT.register(TitleScreenEventHandler::afterScreenInit)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun afterScreenInit(client: Minecraft, screen: Screen, scaledWidth: Int, scaledHeight: Int) {
        if (screen !is TitleScreen) return

        val buttons = Screens.getButtons(screen)

        val multiplayerButton = buttons.find { it.hasText("menu.multiplayer") }
        multiplayerButton?.width = 98

        val bingoText = Component.literal("MultiBingo")
        val bingoButton = TitleBingoButton(screen.width / 2 + 2, screen.height / 4 + 72, 98, 20, bingoText) {
            client.setScreen(NewGameScreen(screen))
        }
        buttons.add(bingoButton)
    }

    private fun AbstractWidget.hasText(translationKey: String): Boolean {
        val textContent = message.contents
        return (textContent as? TranslatableContents)?.key.equals(translationKey)
    }
}
