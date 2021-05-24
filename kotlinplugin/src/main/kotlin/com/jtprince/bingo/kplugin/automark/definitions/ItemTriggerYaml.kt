package com.jtprince.bingo.kplugin.automark.definitions

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.automark.MissingVariableException
import com.jtprince.bingo.kplugin.game.SetVariables
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level


/**
 * Wrapper for a .yml file that contains Item Trigger definitions. For more information of the
 * specifications of these files, see README.md in this file's directory.
 */
class ItemTriggerYaml private constructor(
    @JsonProperty("item_triggers") private val rootMatchGroups: Map<String, MatchGroup>
) {
    class Definition internal constructor(
        internal val rootMatchGroup: MatchGroup
    ) : TriggerDefinition {
        override val neededVars: Array<String>
            get() = rootMatchGroup.neededVars
    }

    private val definitionCache = mutableMapOf<String, Definition?>()

    companion object {
        val defaultYaml: ItemTriggerYaml by lazy {
            fromFile(ItemTriggerYaml::class.java.getResourceAsStream("/item_triggers.yml"))
        }

        /**
         * Load a YAML specification from an InputStream.
         */
        fun fromFile(yamlFile: InputStream?): ItemTriggerYaml {
            val yaml = ObjectMapper(YAMLFactory())
            // Allow `name` field to be either a String or list of Strings
            yaml.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            return try {
                yaml.readValue(yamlFile, ItemTriggerYaml::class.java)
            } catch (e: IOException) {
                BingoPlugin.logger.log(Level.SEVERE, "Could not parse item triggers yaml", e)
                ItemTriggerYaml(emptyMap())  // Return an empty YAML
            }
        }
    }

    val allAutomatedGoals: Set<String>
        get() = rootMatchGroups.keys

    /**
     * Get the specifications for the Item Trigger that is configured for a given goal ID. If the
     * goal ID is not present in item_triggers.yml, returns null. The return value is an Item Match
     * Group that can be considered the Root Item Match Group for this goal ID.
     */
    operator fun get(goalId: String): Definition? {
        return definitionCache.getOrPut(goalId) {
            rootMatchGroups[goalId]?.let { Definition(it) }
        }
    }

    /**
     * An Item Match Group is a node in a tree of Item Match Group objects, with the root of this
     * tree belonging to a goal ID. This mechanism is described in detail in README.md.
     */
    class MatchGroup @JsonCreator private constructor(
        @JsonProperty("name") names: List<String>?,
        @JsonProperty("unique") unique: String?,
        @JsonProperty("total") total: String?,
        @JsonProperty("groups") children: List<MatchGroup>?
    ) {
        private val names: List<Regex> = names?.map{ s -> Regex(s) } ?: emptyList()
        private val unique: Variable = Variable(unique, 1)
        private val total: Variable = Variable(total, 1)
        internal val children: List<MatchGroup> = children ?: emptyList()

        val neededVars: Array<String>
            get() {
                val v = setOfNotNull(this.unique.name, this.total.name).toMutableSet()
                this.children.forEach { c -> v.addAll(c.neededVars) }
                return v.toTypedArray()
            }

        fun nameMatches(name: String): Boolean {
            return names.any { it.matches(name) }
        }

        fun unique(setVariables: SetVariables): Int {
            return unique.getSetVariable(setVariables)
        }

        fun total(setVariables: SetVariables): Int {
            return total.getSetVariable(setVariables)
        }

    }

    internal class Variable(yamlVarValue: String?, defaultConstant: Int) {
        internal val name: String?
        private val constant: Int?

        init {
            if (yamlVarValue?.startsWith("$") == true) {
                name = yamlVarValue.substring(1)
                constant = null
            } else {
                name = null
                constant = yamlVarValue?.toIntOrNull() ?: defaultConstant
            }
        }

        fun getSetVariable(setVariables: SetVariables): Int {
            return constant ?: setVariables[name] ?: throw MissingVariableException(name)
        }
    }
}
