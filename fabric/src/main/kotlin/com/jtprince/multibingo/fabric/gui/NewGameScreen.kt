package com.jtprince.multibingo.fabric.gui

import com.jtprince.multibingo.fabric.BingoGameCreator
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component


class NewGameScreen(val previousScreen: Screen) : Screen(Component.literal("MultiBingo")) {
    private lateinit var gameCodeTextField: EditBox
    private lateinit var doneButton: Button
    private lateinit var cancelButton: Button

    override fun init() {
        super.init()

        /* Game code field */
        gameCodeTextField = EditBox(this.font, this.width / 2 - 100, 106, 200, 20, Component.literal("Game Code"))
        this.addWidget(gameCodeTextField)
        this.setInitialFocus(gameCodeTextField)

        /* Done button */
        doneButton = Button(this.width / 2 - 100, this.height / 4 + 96 + 18, 200, 20, Component.literal("Done")) {
//            val settings = WebGameSettings(gameCodeTextField.value)
//            FabricMain.core.httpClient.generateBoard(settings) {
//                FabricMain.logger.info("Generated board $it!")
//            }
            BingoGameCreator.createAndLoadNewGame(gameCodeTextField.value)
        }
        this.addRenderableWidget(doneButton)

        /* Cancel button */
        cancelButton = Button(this.width / 2 - 100, this.height / 4 + 120 + 18, 200, 20, CommonComponents.GUI_CANCEL) {
            this.minecraft?.setScreen(previousScreen)
        }
        this.addRenderableWidget(cancelButton)
    }

    override fun render(poseStack: PoseStack, i: Int, j: Int, f: Float) {
        renderBackground(poseStack)

        GuiComponent.drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF)
        GuiComponent.drawString(poseStack, this.font, Component.literal("Game Code"), this.width / 2 - 100, 94, 0xA0A0A0)
        gameCodeTextField.render(poseStack, i, j, f)

        super.render(poseStack, i, j, f)
    }

    override fun tick() {
        this.gameCodeTextField.tick()
    }
}
