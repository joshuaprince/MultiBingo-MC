package com.jtprince.multibingo.fabric.automark

import com.jtprince.multibingo.core.automark.AutoMarkConsumer
import com.jtprince.multibingo.core.automark.AutoMarkTrigger
import com.jtprince.multibingo.core.automark.AutoMarkTriggerFactory

object FabricAutoMarkTriggerFactory : AutoMarkTriggerFactory {
    override fun create(
        goalId: String,
        variables: Map<String, Int>,
        consumer: AutoMarkConsumer
    ): Collection<AutoMarkTrigger> {
        println("Pretend that I'm listening for $goalId")
        return listOf(FabricAutoMarkTrigger())
    }
}
