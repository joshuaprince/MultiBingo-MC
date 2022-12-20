package com.jtprince.multibingo.fabric.automark

import com.jtprince.multibingo.core.automark.AutoMarkConsumer
import com.jtprince.multibingo.core.game.BingoGameFactory
import com.jtprince.multibingo.fabric.FabricMain
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

object FabricEventListener {
    fun register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(FabricEventListener::afterKilledOtherEntity)
        UseItemCallback.EVENT.register(FabricEventListener::afterUseItem)
    }

    private fun activate(goalId: String) {
        // Called regardless of whether this goal ID is active

        val game = BingoGameFactory.currentGame ?: return
        val player = FabricMain.bingoPlayer ?: run {
            FabricMain.logger.error("Got an automark but the Fabric BingoPlayer singleton is null!")
            return
        }

        for (space in game.automatedSpaces.filter { it.goalId == goalId }) {
            val activation = AutoMarkConsumer.Activation(player, space, true)
            game.autoMarkConsumer.receiveAutoMark(activation)
        }
    }

    private fun afterKilledOtherEntity(world: ServerLevel, entity: Entity, killed: LivingEntity) {
        if (killed is IronGolem) {
            activate("jm_kill_golem_iron")
        }
    }

    private fun afterUseItem(player: Player, world: Level, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)
        val material: String = itemStack.item.descriptionId

        if (material == "item.minecraft.spyglass") {
            activate("jtp18_spyglass")
        }

        return InteractionResultHolder.pass(itemStack)
    }
}
