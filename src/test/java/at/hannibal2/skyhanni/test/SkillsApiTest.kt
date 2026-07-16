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
        val skillType = SkillType.MINING
        onActionBarUpdate(createMockActionBarEvent("+100 Mining (50%)"))
        val skillInfo = storage?.get(skillType)
        assertNotNull(skillInfo, "Storage should contain skill data after update")
        assertEquals(100, skillInfo!!.totalXp, "Total XP should be 100")
    }

    @Test
    fun `when widget provides absolute data, it overrides action bar percentage`() {
        TabWidget.SKILLS.setLines(" Mining 10: 500,000/1M")
        val skillType = SkillType.MINING
        onActionBarUpdate(createMockActionBarEvent("+100 Mining (50%)"))
        val skillInfo = storage?.get(skillType)
        assertEquals(500100, skillInfo!!.currentXp, "Storage should reconcile with Widget absolute data")
    }

    @Test
    fun `when skill is maxed in widget, xp gain persists correctly`() {
        TabWidget.SKILLS.setLines(" Mining 60: MAX")
        val skillType = SkillType.MINING
        val initialXP = 111_678_000L
        storage?.set(skillType, SkillApi.SkillInfo(totalXp = initialXP))
        onActionBarUpdate(createMockActionBarEvent("+500 Mining (0/1000)"))
        val skillInfo = storage?.get(skillType)
        assertEquals(initialXP + 500, skillInfo!!.totalXp, "Maxed skill should correctly increment total XP")
    }

    @Test
    fun `when action bar is overwritten by item ability, widget holds source of truth`() {
        TabWidget.SKILLS.setLines(" Combat 20: 25.0%")
        val skillType = SkillType.COMBAT
        onActionBarUpdate(createMockActionBarEvent("+50 Combat (25%)"))
        val skillInfo = storage?.get(skillType)
        assertNotNull(skillInfo)
        val expectedMin = calculateLevelXP(19)
        val expectedMax = calculateLevelXP(19) + (7_600_000.0 * 0.25)

        assert(skillInfo!!.totalXp >= expectedMin)
        assert(skillInfo.totalXp <= expectedMax + 1000)
    }
    @Test
    fun `when overflow occurs, storage correctly tracks total and overflow`() {
        val skillType = SkillType.MINING
        val maxLevelXP = calculateLevelXP(skillType.maxLevel - 1).toLong()
        storage?.set(skillType, SkillApi.SkillInfo(totalXp = maxLevelXP, level = skillType.maxLevel))

        onActionBarUpdate(createMockActionBarEvent("+1000 Mining (500/0)"))

        val skillInfo = storage?.get(skillType)
        assertEquals(maxLevelXP + 1000, skillInfo!!.totalXp)
        assertEquals(500, skillInfo.currentXp)
        assertEquals(0, skillInfo.currentXpMax)
    }

    @Test
    fun `when significant xp gain triggers level up, storage updates level correctly`() {
        val skillType = SkillType.MINING
        val startLevel = 10
        val startXP = calculateLevelXP(startLevel - 1).toLong()
        storage?.set(skillType, SkillApi.SkillInfo(totalXp = startXP, level = startLevel))

        val gain = 1000L
        val nextLevelXP = calculateLevelXP(startLevel).toLong() // XP at start of level 11

        onActionBarUpdate(createMockActionBarEvent("+$gain Mining (${gain}/10000)"))

        val skillInfo = storage?.get(skillType)
        assertEquals(startLevel + 1, skillInfo!!.level)
        assertEquals(startXP + gain, skillInfo.totalXp)
    }

    @Test
    fun `when multiple skill updates occur in succession, storage accumulates correctly`() {
        val skillType = SkillType.FARMING

        onActionBarUpdate(createMockActionBarEvent("+100 Farming (10/50)"))
        onActionBarUpdate(createMockActionBarEvent("+200 Farming (60/125)"))

        val skillInfo = storage?.get(skillType)
        assertEquals(310, skillInfo!!.totalXp)
        assertEquals(60, skillInfo.currentXp)
        assertEquals(2, skillInfo.level)
    }

    @Test
    fun `when widget max state is delayed, storage corrects level upon next update`() {
        TabWidget.SKILLS.setLines(" Mining 60: MAX")
        val skillType = SkillType.MINING
        val initialXP = calculateLevelXP(59).toLong()
        storage?.set(skillType, SkillApi.SkillInfo(totalXp = initialXP, level = 59))

        onActionBarUpdate(createMockActionBarEvent("+500 Mining (0/0)"))

        val skillInfo = storage?.get(skillType)
        assertEquals(60, skillInfo!!.level)
        assertEquals(initialXP + 500, skillInfo.totalXp)
    }

    private fun createMockActionBarEvent(text: String): ActionBarUpdateEvent {
        return ActionBarUpdateEvent(text, text.asComponent())
    }

    private fun TabWidget.setLines(line: String) {
        postNewEvent(listOf(line.asComponent()))
    }
}
