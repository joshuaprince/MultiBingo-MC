package com.jtprince.multibingo.core.automark

interface AutoMarkTriggerFactory {
    fun create(
        goalId: String,
        variables: Map<String, Int>,
        consumer: AutoMarkConsumer
    ) : Collection<AutoMarkTrigger>
}
