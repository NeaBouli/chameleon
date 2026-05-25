/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.repository

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.dao.MessageDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.entity.MessageEntity
import com.stealthx.features.messenger.transport.MessengerTransport
import com.stealthx.features.messenger.transport.MessengerTransportResult
import com.stealthx.features.messenger.transport.MessengerTransportType
import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.shared.model.RatchetMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class DecryptedMessage(
    val id: String,
    val contactId: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val deliveryStatus: String,
    val transportType: MessengerTransportType
)

data class ConversationSummary(
    val contactId: String,
    val displayName: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

@Singleton
class MessengerRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val contactKeyDao: ContactKeyDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val transports: Map<MessengerTransportType, @JvmSuppressWildcards MessengerTransport>
) {

    fun observeMessages(contactId: String): Flow<List<DecryptedMessage>> =
        messageDao.observeForContact(contactId).map { entities ->
            val contact = contactKeyDao.getById(contactId)
            entities.map { it.toDecrypted(contact) }
        }

    fun observeConversationSummaries(): Flow<List<ConversationSummary>> =
        messageDao.observeLatestPerContact().combine(
            contactKeyDao.observeAll()
        ) { latestMessages, contacts ->
            val contactMap = contacts.associateBy { it.id }
            latestMessages.mapNotNull { entity ->
                val contact = contactMap[entity.contactId] ?: return@mapNotNull null
                val unread = 0 // simplified; full unread count requires per-contact flow
                ConversationSummary(
                    contactId = entity.contactId,
                    displayName = contact.displayName,
                    lastMessage = entity.toDecrypted(contact).text,
                    timestamp = entity.sentAt,
                    unreadCount = unread
                )
            }
        }

    suspend fun sendMessage(
        contactId: String,
        plaintext: String,
        transportType: MessengerTransportType
    ): DecryptedMessage {
        val contact = contactKeyDao.getById(contactId)
            ?: throw IllegalArgumentException("Contact not found: $contactId")
        val sentAt = System.currentTimeMillis()
        val aad = aadFor(contactId, sentAt)

        val outbound = chatSessionRepository.encryptForSend(
            contact = contact,
            plaintext = plaintext.toByteArray(Charsets.UTF_8),
            aad = aad
        )

        val transport = transports[transportType]
        val transportResult = if (transport != null && transport.isAvailable) {
            transport.send(contactId, outbound.message)
        } else {
            MessengerTransportResult.Failed(transportType, "Transport not available")
        }

        val localKey = localMessageKey(contact)
        val localPayload = ChameleonCrypto.encrypt(plaintext.toByteArray(Charsets.UTF_8), localKey, aad)
        ChameleonCrypto.wipeBytes(localKey)

        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            contactId = contactId,
            direction = DIRECTION_OUTGOING,
            ciphertext = localPayload.ciphertext,
            nonce = localPayload.nonce,
            aad = localPayload.aad,
            paddedLength = localPayload.paddedLength,
            algorithm = localPayload.algorithm,
            payloadVersion = localPayload.version,
            sentAt = sentAt,
            deliveryStatus = transportResult.toDeliveryStatus(),
            transportType = transportType.name,
            ratchetDhPublic = outbound.dhPublicKey,
            ratchetCounter = outbound.counter,
            ratchetPrevCounter = outbound.message.prevCounter,
            ratchetCiphertext = outbound.message.payload.ciphertext,
            ratchetNonce = outbound.message.payload.nonce,
            ratchetAad = outbound.message.payload.aad,
            ratchetPaddedLength = outbound.message.payload.paddedLength,
            ratchetAlgorithm = outbound.message.payload.algorithm,
            ratchetPayloadVersion = outbound.message.payload.version
        )
        messageDao.insert(entity)
        return entity.toDecrypted(contact)
    }

    suspend fun receiveMessage(contactId: String, message: RatchetMessage, transport: MessengerTransportType): DecryptedMessage {
        val contact = contactKeyDao.getById(contactId)
            ?: throw IllegalArgumentException("Contact not found: $contactId")
        val receivedAt = System.currentTimeMillis()
        val aad = aadForIncoming(contactId, receivedAt)
        val plaintextBytes = chatSessionRepository.decryptIncoming(contact, message)
        val plaintext = plaintextBytes.toString(Charsets.UTF_8)
        val localKey = localMessageKey(contact)
        val localPayload = try {
            ChameleonCrypto.encrypt(plaintextBytes, localKey, aad)
        } finally {
            ChameleonCrypto.wipeBytes(localKey)
            ChameleonCrypto.wipeBytes(plaintextBytes)
        }

        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            contactId = contactId,
            direction = DIRECTION_INCOMING,
            ciphertext = localPayload.ciphertext,
            nonce = localPayload.nonce,
            aad = localPayload.aad,
            paddedLength = localPayload.paddedLength,
            algorithm = localPayload.algorithm,
            payloadVersion = localPayload.version,
            sentAt = receivedAt,
            deliveryStatus = STATUS_UNREAD,
            transportType = transport.name,
            ratchetDhPublic = message.dhPublicKey,
            ratchetCounter = message.counter,
            ratchetPrevCounter = message.prevCounter,
            ratchetCiphertext = message.payload.ciphertext,
            ratchetNonce = message.payload.nonce,
            ratchetAad = message.payload.aad,
            ratchetPaddedLength = message.payload.paddedLength,
            ratchetAlgorithm = message.payload.algorithm,
            ratchetPayloadVersion = message.payload.version
        )
        messageDao.insert(entity)
        return entity.toDecrypted(contact).copy(text = plaintext)
    }

    suspend fun getContactName(contactId: String): String =
        contactKeyDao.getById(contactId)?.displayName ?: contactId

    suspend fun markRead(contactId: String) = messageDao.markRead(contactId)

    suspend fun clearMessages(contactId: String) = messageDao.deleteForContact(contactId)

    private fun MessageEntity.toDecrypted(contact: ContactKeyEntity?): DecryptedMessage {
        val text = if (contact == null) "Encrypted message" else decryptText(this, contact)
        val transport = runCatching { MessengerTransportType.valueOf(transportType) }
            .getOrDefault(MessengerTransportType.SERVER_RELAY)
        return DecryptedMessage(
            id = id,
            contactId = contactId,
            text = text,
            isOutgoing = direction == DIRECTION_OUTGOING,
            timestamp = sentAt,
            deliveryStatus = deliveryStatus,
            transportType = transport
        )
    }

    private fun decryptText(entity: MessageEntity, contact: ContactKeyEntity): String {
        val key = localMessageKey(contact)
        return try {
            val payload = EncryptedPayload(
                ciphertext = entity.ciphertext,
                nonce = entity.nonce,
                paddedLength = entity.paddedLength,
                aad = entity.aad,
                algorithm = entity.algorithm,
                version = entity.payloadVersion
            )
            ChameleonCrypto.decrypt(payload, key).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            "Encrypted message"
        } finally {
            ChameleonCrypto.wipeBytes(key)
        }
    }

    private fun localMessageKey(contact: ContactKeyEntity): ByteArray {
        val ikm = contact.identityKey + contact.dhPublicKey + contact.id.toByteArray(Charsets.UTF_8)
        return ChameleonCrypto.hkdf(
            ikm = ikm,
            salt = null,
            info = "ChameleonLocalMessageKey:v1".toByteArray(Charsets.UTF_8)
        )
    }

    private fun aadFor(contactId: String, sentAt: Long): ByteArray =
        "chameleon-msg:v1:$contactId:$sentAt".toByteArray(Charsets.UTF_8)

    private fun aadForIncoming(contactId: String, receivedAt: Long): ByteArray =
        "chameleon-incoming:v1:$contactId:$receivedAt".toByteArray(Charsets.UTF_8)

    private fun MessengerTransportResult.toDeliveryStatus(): String = when (this) {
        is MessengerTransportResult.Sent -> STATUS_SENT
        is MessengerTransportResult.Queued -> STATUS_QUEUED
        is MessengerTransportResult.Failed -> STATUS_FAILED
    }

    private companion object {
        const val DIRECTION_INCOMING = "INCOMING"
        const val DIRECTION_OUTGOING = "OUTGOING"
        const val STATUS_UNREAD = "UNREAD"
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
    }
}
