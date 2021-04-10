package com.jtprince.bingo.plugin.automarking.itemtrigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemTriggerYaml {
    private static final String YAML_PATH = "/item_triggers.yml";
    private static ItemTriggerYaml defaultYaml;
    public final @NotNull Map<String, MatchGroup> itemTriggers;

    @JsonCreator
    private ItemTriggerYaml(@JsonProperty("item_triggers") @NotNull Map<String, MatchGroup> root) {
        this.itemTriggers = root;
    }

    private ItemTriggerYaml() {
        this.itemTriggers = new HashMap<>();
    }

    /**
     * Load the YAML specification containing item trigger definitions packaged with the JAR, at
     * resources/item_triggers.yml.
     */
    public static ItemTriggerYaml defaultYaml() {
        if (defaultYaml == null) {
             defaultYaml = ItemTriggerYaml.fromFile(ItemTrigger.class.getResourceAsStream(YAML_PATH));
        }
        return defaultYaml;
    }

    /**
     * Load a YAML specification from an InputStream.
     */
    public static ItemTriggerYaml fromFile(InputStream yamlFile) {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

        // Allow `name` field to be either a String or list of Strings
        yaml.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        try {
            return yaml.readValue(yamlFile, ItemTriggerYaml.class);
        } catch (IOException e) {
            MCBingoPlugin.logger().log(
                Level.SEVERE, "Could not parse item triggers yaml", e);
            return new ItemTriggerYaml();
        }
    }

    /**
     * Get the specifications for the Item Trigger that is configured for a given goal ID. If the
     * goal ID is not present in item_triggers.yml, returns null. The return value is an Item Match
     * Group that can be considered the Root Item Match Group for this goal ID.
     */
    @Nullable ItemTriggerYaml.MatchGroup get(String goalId) {
        return itemTriggers.get(goalId);
    }

    /**
     * An Item Match Group is a node in a tree of Item Match Group objects, with the root of this
     * tree belonging to a goal ID. This mechanism is described in detail in README.md.
     */
    static class MatchGroup {
        private final List<Pattern> names;

        private final Variable unique;
        private final Variable total;
        public final List<MatchGroup> children;

        @JsonCreator
        MatchGroup(@JsonProperty("name") List<String> name,
                          @JsonProperty("unique") String unique,
                          @JsonProperty("total") String total,
                          @JsonProperty("groups") List<MatchGroup> groups) {
            if (name != null) {
                this.names = name.stream().map(Pattern::compile).collect(Collectors.toList());
            } else {
                this.names = Collections.emptyList();
            }
            this.unique = new Variable(unique, 1);
            this.total = new Variable(total, 1);
            this.children = Objects.requireNonNullElse(groups, Collections.emptyList());
        }

        public boolean nameMatches(String name) {
            return names.stream().anyMatch(p -> p.matcher(name).matches());
        }

        public int getUnique(@NotNull Map<String, Integer> setVariables) {
            return unique.getValue(setVariables);
        }

        public int getTotal(@NotNull Map<String, Integer> setVariables) {
            return total.getValue(setVariables);
        }
    }

    static class Variable {
        @Nullable String name;
        @Nullable Integer constant;

        Variable(@Nullable String yamlVarString, int defaultConstant) {
            if (yamlVarString == null) {
                this.constant = defaultConstant;
            } else if (yamlVarString.startsWith("$")) {
                this.name = yamlVarString.substring(1);
            } else {
                this.constant = Integer.parseInt(yamlVarString);
            }
        }

        int getValue(@NotNull Map<String, Integer> setVariables) {
            if (constant != null) {
                return constant;
            } else if (setVariables.containsKey(name)) {
                return setVariables.get(name);
            } else {
                throw new RuntimeException("Unknown variable " + name);
            }
        }
    }
}
