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
        startEnforcing()
    }

    @Test
    fun `enforced value overrides config`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)
        assertEquals(true, isEnforced(ENABLED))
    }

    @Test
    fun `original value is restored after enforcement is removed`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)

        startEnforcing()

        assertEquals(true, enabled)
    }

    @Test
    fun `persisted value is not restored after enforcement is removed`() {
        startEnforcing(
            enforcedValue(ENABLED, false, persist = true),
        )

        assertEquals(false, enabled)

        startEnforcing()

        assertEquals(false, enabled)
    }

    @Test
    fun `persisted value is not backed up`() {
        startEnforcing(
            enforcedValue(ENABLED, false, persist = true),
        )

        assertEquals(false, enabled)
        assertEquals(
            false,
            userValues.containsKey(ENABLED),
        )
    }

    @Test
    fun `non-persisted value is backed up`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)
        assertEquals(
            JsonPrimitive(true),
            userValues[ENABLED],
        )
    }

    @Test
    fun `original value is only backed up once`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)

        // Simulate the value being changed while it is enforced.
        enabled = true

        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)

        // The original value should be restored, not the value
        // from the second enforcement.
        startEnforcing()

        assertEquals(true, enabled)
    }

    @Test
    fun `multiple values can be enforced`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        assertEquals(true, isEnforced(ENABLED))
        assertEquals(true, isEnforced(ASSUME_MAYOR))
    }

    @Test
    fun `multiple values are restored`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        startEnforcing()

        assertEquals(true, enabled)
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `persisted and non-persisted values behave independently`() {
        startEnforcing(
            enforcedValue(ENABLED, false, persist = true),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        assertEquals(
            false,
            userValues.containsKey(ENABLED),
        )
        assertEquals(
            true,
            userValues.containsKey(ASSUME_MAYOR),
        )

        startEnforcing()

        // Persisted value remains at the enforced value.
        assertEquals(false, enabled)

        // Non-persisted value gets its original value back.
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `unrelated config values are unchanged`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `extra message is returned`() {
        startEnforcing(
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
    }

    @Test
    fun `missing extra message returns empty string`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(
            "",
            enforcementMessage(ENABLED),
        )
    }

    @Test
    fun `unknown value is not reported as enforced`() {
        assertEquals(
            false,
            isEnforced("dev.debug.doesNotExist"),
        )
    }

    @Test
    fun `removing one enforced value restores only that value`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        startEnforcing(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        // ENABLED is no longer enforced, so its original value is restored.
        assertEquals(true, enabled)

        // ASSUME_MAYOR is still enforced.
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        assertEquals(false, isEnforced(ENABLED))
        assertEquals(true, isEnforced(ASSUME_MAYOR))
    }

    @Test
    fun `changing enforced value keeps original backup`() {
        enabled = true

        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(false, enabled)

        startEnforcing(
            enforcedValue(ENABLED, true),
        )

        assertEquals(true, enabled)

        startEnforcing()

        // The original value was true.
        assertEquals(true, enabled)
    }

    @Test
    fun `persisted value remains after changing enforced value`() {
        startEnforcing(
            enforcedValue(
                ENABLED,
                false,
                persist = true,
            ),
        )

        assertEquals(false, enabled)

        startEnforcing(
            enforcedValue(
                ENABLED,
                true,
                persist = true,
            ),
        )

        assertEquals(true, enabled)

        startEnforcing()

        // No backup was created, so the last enforced value remains.
        assertEquals(true, enabled)
    }

    @Test
    fun `persisted value does not create user value even after repeated enforcement`() {
        startEnforcing(
            enforcedValue(
                ENABLED,
                false,
                persist = true,
            ),
        )

        startEnforcing(
            enforcedValue(
                ENABLED,
                true,
                persist = true,
            ),
        )

        assertEquals(
            false,
            userValues.containsKey(ENABLED),
        )
    }

    @Test
    fun `enforced value is no longer reported after removal`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
        )

        assertEquals(true, isEnforced(ENABLED))

        startEnforcing()

        assertEquals(false, isEnforced(ENABLED))
    }

    @Test
    fun `user value without enforcement is restored and removed`() {
        enabled = true

        userValues[ENABLED] = JsonPrimitive(true)

        // ENABLED is not currently enforced, so the stale backup should
        // be restored and removed.
        startEnforcing()

        assertEquals(true, enabled)
        assertEquals(false, userValues.containsKey(ENABLED))
    }

    @Test
    fun `removing enforcement restores only expired enforced paths`() {
        startEnforcing(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(false, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        startEnforcing(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.DIAZ),
        )

        assertEquals(true, enabled)
        assertEquals(ElectionCandidate.DIAZ, assumeMayor)

        assertEquals(
            false,
            userValues.containsKey(ENABLED),
        )
        assertEquals(
            true,
            isEnforced(ASSUME_MAYOR),
        )
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

    private fun startEnforcing(vararg values: EnforcedValueData) {
        EnforcedConfigValues.updateData(values.toList())
    }

    private fun isEnforced(path: String): Boolean =
        EnforcedConfigValues.isBlockedFromEditing(path) != null

    private fun enforcementMessage(path: String): String? =
        EnforcedConfigValues.isBlockedFromEditing(path)
}
