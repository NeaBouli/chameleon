package com.stealthx.chameleon

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.ChameleonDatabase
import com.stealthx.shared.model.EncryptedPayload
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two-phase dependency-upgrade probe for the native crypto binding (lazysodium over JNA)
 * and the SQLCipher database. Skipped unless `storageUpgradePhase` is passed:
 *
 *  1. build/install the PREVIOUS dependency set, run with `-e storageUpgradePhase write`
 *  2. `adb install -r` the NEW dependency set, run with `-e storageUpgradePhase read`
 *
 * Only for emulators: it writes into the app's real database.
 */
@RunWith(AndroidJUnit4::class)
class StorageUpgradeProbeTest {
    private val phase: String? =
        InstrumentationRegistry.getArguments().getString("storageUpgradePhase")
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val marker = File(context.filesDir, "storage-upgrade-probe.json")
    private val passphrase = "storage-upgrade-probe-passphrase".toByteArray(Charsets.UTF_8)
    private val key = ByteArray(32) { index -> (index * 7 + 3).toByte() }
    private val plaintext = "chameleon storage upgrade probe".toByteArray(Charsets.UTF_8)
    private val aad = "chameleon24.app".toByteArray(Charsets.UTF_8)

    @Test
    fun persistedSecretsSurviveDependencyUpgrade() {
        assumeTrue("storageUpgradePhase not requested", phase == "write" || phase == "read")
        SodiumInitializer.ensureInit()
        if (phase == "write") write() else read()
    }

    private fun write() {
        val payload = ChameleonCrypto.encrypt(plaintext, key.copyOf(), aad)
        val database = ChameleonDatabase.build(context, passphrase)
        database.openHelper.writableDatabase.apply {
            execSQL("CREATE TABLE IF NOT EXISTS storage_upgrade_probe (id INTEGER PRIMARY KEY, value TEXT NOT NULL)")
            execSQL("DELETE FROM storage_upgrade_probe")
            execSQL("INSERT INTO storage_upgrade_probe (id, value) VALUES (1, ?)", arrayOf("sqlcipher-row"))
        }
        database.close()
        marker.writeText(
            JSONObject()
                .put("ciphertext", payload.ciphertext.b64())
                .put("nonce", payload.nonce.b64())
                .put("paddedLength", payload.paddedLength)
                .put("aad", payload.aad.b64())
                .put("algorithm", payload.algorithm)
                .put("version", payload.version)
                .toString(),
        )
    }

    private fun read() {
        val stored = JSONObject(marker.readText())
        val payload = EncryptedPayload(
            ciphertext = stored.getString("ciphertext").unb64(),
            nonce = stored.getString("nonce").unb64(),
            paddedLength = stored.getInt("paddedLength"),
            aad = stored.getString("aad").unb64(),
            algorithm = stored.getString("algorithm"),
            version = stored.getInt("version"),
        )
        assertArrayEquals(
            "ciphertext written with the previous binding must decrypt",
            plaintext,
            ChameleonCrypto.decrypt(payload, key.copyOf()),
        )

        val database = ChameleonDatabase.build(context, passphrase)
        database.openHelper.readableDatabase
            .query("SELECT value FROM storage_upgrade_probe WHERE id = 1").use { cursor ->
                assertTrue("SQLCipher probe row must stay readable", cursor.moveToFirst())
                assertEquals("sqlcipher-row", cursor.getString(0))
            }
        database.close()
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.unb64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
