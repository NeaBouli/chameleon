package com.stealthx.data.identity

import android.content.Context
import androidx.room.withTransaction
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.NfcUriRelay
import com.stealthx.data.NfcWriteRelay
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.tier.TierGate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityRecoveryManager @Inject constructor(
    @ApplicationContext context: Context,
    private val database: ChameleonDatabase,
    private val preferences: AppPreferences,
    private val tierGate: TierGate,
    private val contactExchangeManager: ContactExchangeManager
) {
    private val appContext = context.applicationContext
    private val recoveryLock = Mutex()

    suspend fun recover(expectedReason: IdentityIntegrityReason): StealthXId =
        recoveryLock.withLock {
            require(expectedReason.isUserRecoverable) {
                "This identity failure cannot be reset safely"
            }

            val freshIdentity = contactExchangeManager.withIncomingPaused {
                NfcWriteRelay.reset()
                NfcUriRelay.consume()

                database.withTransaction {
                    database.messageDao().deleteAll()
                    database.chatSessionDao().deleteAll()
                    database.contactKeyDao().deleteAll()
                    database.accessTierCacheDao().deleteAll()
                }

                check(preferences.clearEntitlementToken()) {
                    "Failed to clear identity-bound entitlement"
                }
                tierGate.invalidateCache()

                // Identity is cleared last so an interrupted recovery never mixes a new ID
                // with contacts or sessions belonging to the old one.
                StealthXIdentity.resetIrrecoverable(appContext, expectedReason)
                StealthXIdentity.getOrCreateWithSeed(appContext)
            }

            contactExchangeManager.resumeAfterRecovery(preferences.backgroundListenerEnabled)
            freshIdentity
        }
}
