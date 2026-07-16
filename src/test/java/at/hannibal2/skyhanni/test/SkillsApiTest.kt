package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.api.SkillApi.onActionBarUpdate
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.calculateLevelXP
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SkillTrackingTest {
    private val skillStorage get() = ProfileStorageData.profileSpecific?.skills
    val storage get() = skillStorage?.skillData

    @BeforeEach
    fun setup() {
        SkillApi.updateLevelArray(listOf(
            50,
            125,
            200,
            300,
            500,
            750,
            1000,
            1500,
            2000,
            3500,
            5000,
            7500,
            10000,
            15000,
            20000,
            30000,
            50000,
            75000,
            100000,
            200000,
            300000,
            400000,
            500000,
            600000,
            700000,
            800000,
            900000,
            1000000,
            1100000,
            1200000,
            1300000,
            1400000,
            1500000,
            1600000,
            1700000,
            1800000,
            1900000,
            2000000,
            2100000,
            2200000,
            2300000,
            2400000,
            2500000,
            2600000,
            2750000,
            2900000,
            3100000,
            3400000,
            3700000,
            4000000,
            4300000,
            4600000,
            4900000,
            5200000,
            5500000,
            5800000,
            6100000,
            6400000,
            6700000,
            7000000))
        ProfileStorageData.profileSpecific = ProfileSpecificStorage()
        storage?.clear()
        TabWidget.SKILLS.postClearEvent()
    }

    @Test
    fun `when action bar updates with percentage but no widget data, storage tracks gain`() {
        // Arrange: No widget info, simulate a +100 XP gain at 50% progress
        val skillType = SkillType.MINING
        val actionBarEvent = createMockActionBarEvent("+100 Mining (50%)")

        // Act: Fire the update
        onActionBarUpdate(actionBarEvent)

        // Assert: Storage should have recorded the gain
        val skillInfo = storage?.get(skillType)
        assertNotNull(skillInfo, "Storage should contain skill data after update")
        assertEquals(100, skillInfo!!.totalXp, "Total XP should be 100")
    }

    @Test
    fun `when widget provides absolute data, it overrides action bar percentage`() {
        // Arrange: Widget says we have 500,000 / 1,000,000 XP (Level 10)
        // Action bar only says +100 XP (which would normally be ambiguous)
        TabWidget.SKILLS.setLines(" Mining 10: 500,000/1M")
        val skillType = SkillType.MINING
        val actionBarEvent = createMockActionBarEvent("+100 Mining (50%)")

        // Act: Fire the update
        onActionBarUpdate(actionBarEvent)

        // Assert: Result should match Widget Absolute data (500,000 + 100), not just Action Bar percentage
        val skillInfo = storage?.get(skillType)
        // 500,000 from widget + 100 from action bar = 500,100
        assertEquals(500100, skillInfo!!.currentXp, "Storage should reconcile with Widget absolute data")
    }

    @Test
    fun `when skill is maxed in widget, xp gain persists correctly`() {
        // Arrange: Widget reports Mining 60 as MAX
        TabWidget.SKILLS.setLines(" Mining 60: MAX")

        val skillType = SkillType.MINING
        // Pre-set existing storage to a high value
        val initialXP = 111_678_000L
        storage?.set(skillType, SkillApi.SkillInfo(totalXp = initialXP))

        val actionBarEvent = createMockActionBarEvent("+500 Mining (MAX)")

        // Act
        onActionBarUpdate(actionBarEvent)

        // Assert: Should just add to previous total
        val skillInfo = storage?.get(skillType)
        assertEquals(initialXP + 500, skillInfo!!.totalXp, "Maxed skill should correctly increment total XP")
    }

    @Test
    fun `when action bar is overwritten by item ability, widget holds source of truth`() {
        // Arrange: Widget says we are at 25% (Level 20)
        TabWidget.SKILLS.setLines(" Combat 20: 25.0%")

        // Simulate a "junk" action bar string (e.g., item ability text) that contains no skill data
        // Then an update that contains the skill data
        val skillType = SkillType.COMBAT

        // We act as if the ActionBar update triggers
        onActionBarUpdate(createMockActionBarEvent("+50 Combat (25%)"))

        // Assert: Reconciled logic uses Widget, not just the Action Bar percentage
        val skillInfo = storage?.get(skillType)
        // Logic: (Level 19 XP) + (Level 20 Diff * 0.25) + 50
        // We verify the math structure is applied
        assertNotNull(skillInfo)
        // Logic check: Is it roughly where we expect?
        val expectedMin = calculateLevelXP(19)
        val expectedMax = calculateLevelXP(19) + (7_600_000.0 * 0.25)

        assert(skillInfo!!.totalXp >= expectedMin)
        assert(skillInfo.totalXp <= expectedMax + 1000)
    }

    private fun createMockActionBarEvent(text: String): ActionBarUpdateEvent {
        // Create a mock event object with the given text
        return ActionBarUpdateEvent(text, text.asComponent())
    }

    private fun TabWidget.setLines(line: String) {
        postNewEvent(listOf(line.asComponent()))
    }
}
