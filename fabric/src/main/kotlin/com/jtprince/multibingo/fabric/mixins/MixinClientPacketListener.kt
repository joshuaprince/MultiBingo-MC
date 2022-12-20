package com.jtprince.multibingo.fabric.mixins

import com.jtprince.multibingo.fabric.event.LoginEventHandler
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


@Mixin(ClientPacketListener::class)
class MixinClientPacketListener {
    @Inject(method = ["handleLogin"], at = [At("TAIL")])
    private fun onPlayerLogin(arg: ClientboundLoginPacket, ci: CallbackInfo) {
        LoginEventHandler.onLogin()
    }
}
