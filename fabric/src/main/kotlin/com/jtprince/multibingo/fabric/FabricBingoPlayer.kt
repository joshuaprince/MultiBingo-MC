package com.jtprince.multibingo.fabric

import com.jtprince.multibingo.core.automark.itemtrigger.BingoItemStack
import com.jtprince.multibingo.core.player.LocalBingoPlayer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.TextComponent

class FabricBingoPlayer(override val name: String, private val audience: Audience) : LocalBingoPlayer() {
    override fun audiences(): Iterable<Audience> = listOf(audience)

    override val inventory: Collection<BingoItemStack>
        get() = TODO("Not yet implemented")
    override val formattedName: TextComponent
        get() = TODO("Not yet implemented")
}
