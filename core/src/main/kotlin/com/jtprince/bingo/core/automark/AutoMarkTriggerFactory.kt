package com.jtprince.bingo.core.automark

interface AutoMarkTriggerFactory {
    fun create(space: AutomatedSpace, consumer: AutoMarkConsumer) : Collection<AutoMarkTrigger>
}
