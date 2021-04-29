package com.jtprince.bingo.kplugin.automark

import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Test the functionality of the Item Trigger YAML parser.
 */
class TestItemTriggerYaml {
    private val yaml = ItemTriggerYaml.fromFile(javaClass.getResourceAsStream("/test_item_triggers.yml"))
    private val noVariables = mapOf<String, Int>()

    @Test
    fun `A simple goal with no variables can be parsed`() {
        val matchGroup = yaml["jm_book_quill"]
        assertNotNull(matchGroup)
        assertTrue(matchGroup.nameMatches("minecraft:writable_book"))
        assertFalse(matchGroup.nameMatches("minecraft:book"))
        assertEquals(1, matchGroup.unique(noVariables))
        assertEquals(1, matchGroup.total(noVariables))
        assertEquals(0, matchGroup.children.size)
    }

    @Test
    fun `A match group that does not exist returns null`() {
        val matchGroup = yaml["nonexistent_goal_id"]
        assertNull(matchGroup)
    }

    @Test
    fun `A match group with children can be parsed`() {
        val matchGroup = yaml["jm_different_edible"]
        assertNotNull(matchGroup)
        assertTrue(matchGroup.nameMatches("minecraft:beetroot"))
        assertTrue(matchGroup.nameMatches("minecraft:regex_stew"))
        assertFalse(matchGroup.nameMatches("minecraft:potato")) // In child
        assertEquals(6, matchGroup.unique(noVariables))
        assertEquals(2, matchGroup.children.size)
        val potatoChild = matchGroup.children[0]
        assertNotNull(potatoChild)
        assertTrue(potatoChild.nameMatches("minecraft:potato"))
        assertFalse(potatoChild.nameMatches("minecraft:beetroot"))
        assertEquals(1, potatoChild.unique(noVariables))
        assertEquals(1, potatoChild.total(noVariables))
        assertEquals(0, potatoChild.children.size)
    }
}
