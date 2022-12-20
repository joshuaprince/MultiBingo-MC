package com.jtprince.multibingo.fabric

import com.jtprince.multibingo.core.game.BingoGameFactory
import net.minecraft.client.Minecraft
import net.minecraft.core.RegistryAccess
import net.minecraft.world.Difficulty
import net.minecraft.world.level.DataPackConfig
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.levelgen.presets.WorldPresets

object BingoGameCreator {
    fun createAndLoadNewGame(gameCode: String) {
        val minecraft = Minecraft.getInstance() ?: throw Exception("No Minecraft instance.")

        val settings = LevelSettings(
            worldName(gameCode),
            GameType.SURVIVAL,
            false,  // hardcore
            Difficulty.NORMAL,
            true, // allow commands
            GameRules(),
            DataPackConfig.DEFAULT
        )

        val registryAccess: RegistryAccess = RegistryAccess.builtinCopy().freeze()

        val seed = gameCode.hashCode().toLong()
        val worldGenSettings = WorldPresets.createNormalWorldFromPreset(registryAccess, seed)

        FabricMain.logger.info("Creating new world with name ${worldName(gameCode)} and seed $gameCode ($seed).")

        minecraft.createWorldOpenFlows().createFreshLevel(
            worldName(gameCode),
            settings,
            registryAccess,
            worldGenSettings
        )

        BingoGameFactory.createDebugGame()  // TODO
    }

    private fun worldName(gameCode: String) = "MultiBingo_$gameCode"
}
