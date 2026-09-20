package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.EnforcedConfigValues
import at.hannibal2.skyhanni.config.storage.EnforcedUserValuesStorage
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValue
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValueData
import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull

class EnforcedConfigValuesTest {

    companion object {
        private val config get() = SkyHanniMod.feature.dev.debug

        private var enabled
            get() = config.enabled
            set(value) {
                config.enabled = value
            }

        private var assumeMayor
            get() = config.assumeMayor.get()
            set(value) {
                config.assumeMayor.set(value)
            }

        val userValues get() = EnforcedConfigValues.userValues

        private const val ENABLED = "dev.debug.enabled"
        private const val ASSUME_MAYOR = "dev.debug.assumeMayor"

        @BeforeAll
        @JvmStatic
        fun setup() {
            SkyHanniMod.enforcedUserValuesStorage = EnforcedUserValuesStorage()
            SkyHanniMod.configManager = mockk<ConfigManager>(relaxed = true)
        }
    }

    @BeforeEach
    fun cleanup() {
        enabled = true
        assumeMayor = DIANA
        userValues.clear()
        update()
    }

    @Test
    fun `enforced value overrides config and restores original`() {
        update(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)
        assertEquals(true, isEnforced(ENABLED))
        assertEquals(JsonPrimitive(true), userValues[ENABLED])

        update()

        assertEquals(true, enabled)
        assertEquals(false, isEnforced(ENABLED))
        assertEquals(false, userValues.containsKey(ENABLED))
    }

    @Test
    fun `persisted value remains enforced value without backup`() {
        update(
            enforcedValue(ENABLED, false, persist = true),
        )

        assertEquals(false, enabled)
        assertEquals(false, userValues.containsKey(ENABLED))

        update()

        assertEquals(false, enabled)
        assertEquals(false, isEnforced(ENABLED))
    }

    @Test
    fun `backup is created only once while value is enforced`() {
        update(
            enforcedValue(ENABLED, false),
        )

        // Simulate the user/config changing the value while it is enforced.
        enabled = true

        update(
            enforcedValue(ENABLED, false),
        )

        update()

        // Restore the value from before enforcement, not the intermediate value.
        assertEquals(true, enabled)
    }

    @Test
    fun `changing enforced value keeps original backup`() {
        update(
            enforcedValue(ENABLED, false),
        )

        update(
            enforcedValue(ENABLED, true),
        )

        assertEquals(true, enabled)

        update()

        // The backup was made when enforcement started.
        assertEquals(true, enabled)
    }

    @Test
    fun `multiple values are enforced and restored independently`() {
        update(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertEquals(true, isEnforced(ENABLED))
        assertEquals(true, isEnforced(ASSUME_MAYOR))

        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        // Only the expired enforcement is restored.
        assertEquals(true, enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertEquals(false, isEnforced(ENABLED))
        assertEquals(true, isEnforced(ASSUME_MAYOR))
    }

    @Test
    fun `persisted and non-persisted values are independent`() {
        update(
            enforcedValue(ENABLED, false, persist = true),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertEquals(false, userValues.containsKey(ENABLED))
        assertEquals(true, userValues.containsKey(ASSUME_MAYOR))

        update()

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `enforcement metadata is exposed`() {
        update(
            enforcedValue(
                path = ENABLED,
                value = false,
                extraMessage = "This setting is managed by the server.",
            ),
        )

        assertEquals(
            "This setting is managed by the server.",
            enforcementMessage(ENABLED),
        )

        update()

        assertNull(enforcementMessage(ENABLED))
    }

    @Test
    fun `unknown entries are ignored`() {
        assertEquals(false, isEnforced("dev.debug.doesNotExist"))
        userValues["dev.debug.doesNotExist"] = JsonPrimitive(false)
        assertDoesNotThrow { update() }
    }

    @Test fun `left over user values are restored`() {
        userValues[ENABLED] = JsonPrimitive(false)
        update()
        assertEquals(false, enabled)
        assertEquals(false, userValues.containsKey(ENABLED))
    }

    private fun enforcedValue(
        path: String,
        value: JsonElement,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = EnforcedValueData(
        enforcedValues = listOf(
            EnforcedValue(
                path = path,
                value = value,
                persist = persist,
            ),
        ),
        affectedVersion = ModVersion.installed,
        extraMessage = extraMessage,
    )

    private fun enforcedValue(
        path: String,
        value: Boolean,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = enforcedValue(
        path,
        JsonPrimitive(value),
        extraMessage,
        persist,
    )

    private fun enforcedValue(
        path: String,
        value: Enum<*>,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = enforcedValue(
        path,
        JsonPrimitive(value.name),
        extraMessage,
        persist,
    )

    private fun update(vararg values: EnforcedValueData) {
        EnforcedConfigValues.updateData(values.toList())
    }

    private fun isEnforced(path: String): Boolean =
        EnforcedConfigValues.isBlockedFromEditing(path) != null

    private fun enforcementMessage(path: String): String? =
        EnforcedConfigValues.isBlockedFromEditing(path)
}
