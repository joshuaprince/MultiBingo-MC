package com.jtprince.bingo.core.automark.itemtrigger

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.automark.MissingVariableException
import com.jtprince.bingo.core.automark.TriggerDefinition
import java.io.IOException
import java.io.InputStream


/**
 * Wrapper for a .yml file that contains Item Trigger definitions. For more information of the
 * specifications of these files, see README.md in this file's directory.
 */
class ItemTriggerYaml private constructor(
    @JsonProperty("item_triggers") rootMatchGroups: Map<String, MatchGroup>
) {
    class Definition internal constructor(
        val rootMatchGroup: MatchGroup  // TODO: Make internal, move necessary logic to this module
    ) : TriggerDefinition {
        override val neededVars: Array<String>
            get() = rootMatchGroup.neededVars
    }

    val definitions = rootMatchGroups.entries.associate { (k, v) -> (k to Definition(v)) }

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
                // BingoPlugin.logger.log(Level.SEVERE, "Could not parse item triggers yaml", e)
                    //TODO DI?
                ItemTriggerYaml(emptyMap())  // Return an empty YAML
            }
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
        val children: List<MatchGroup> = children ?: emptyList()

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
