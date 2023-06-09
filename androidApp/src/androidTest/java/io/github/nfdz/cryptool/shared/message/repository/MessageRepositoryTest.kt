package io.github.nfdz.cryptool.shared.message.repository

import io.github.nfdz.cryptool.shared.core.realm.FakeRealmGateway
import io.github.nfdz.cryptool.shared.encryption.entity.AlgorithmVersion
import io.github.nfdz.cryptool.shared.encryption.entity.MessageSource
import io.github.nfdz.cryptool.shared.encryption.entity.deserializeMessageSource
import io.github.nfdz.cryptool.shared.encryption.entity.serialize
import io.github.nfdz.cryptool.shared.encryption.repository.realm.EncryptionRealm
import io.github.nfdz.cryptool.shared.message.entity.Message
import io.github.nfdz.cryptool.shared.message.entity.MessageOwnership
import io.github.nfdz.cryptool.shared.message.repository.realm.MessageRealm
import io.github.nfdz.cryptool.shared.platform.storage.FakeKeyValueStorage
import io.realm.kotlin.ext.query
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class MessageRepositoryTest {

    private val messageRealmA = MessageRealm().also { new ->
        new.id = "A"
        new.encryptionId = "encryptionId-A"
        new.message = "Lorem ipsum dolor A"
        new.encryptedMessage =
            "LD8azzqjc-e8C90bJ8Ut2bYa7WU.1QVQGS10pFb-LndU.128.idB9lucOGxHPLLusE_h0iumSadSum1AqzZ3fJQfCjl4OvkS-uWMSmfYb9HhAdkOeKvGP5p4vUQ"
        new.timestampInMillis = 100
        new.isFavorite = false
        new.ownership = MessageOwnership.OTHER.name
    }
    private val encryptionRealmA = EncryptionRealm().also { new ->
        new.id = "encryptionId-A"
        new.name = "Encryption A"
        new.password = "testAA"
        new.algorithm = AlgorithmVersion.V2.name
        new.source = MessageSource.Manual.serialize()
    }
    private val messageA = Message(
        id = "A",
        encryptionId = "encryptionId-A",
        message = "Lorem ipsum dolor A",
        encryptedMessage =
        "LD8azzqjc-e8C90bJ8Ut2bYa7WU.1QVQGS10pFb-LndU.128.idB9lucOGxHPLLusE_h0iumSadSum1AqzZ3fJQfCjl4OvkS-uWMSmfYb9HhAdkOeKvGP5p4vUQ",
        timestampInMillis = 100,
        isFavorite = false,
        ownership = MessageOwnership.OTHER,
    )

    private lateinit var realm: FakeRealmGateway
    private val keyValueStorage = FakeKeyValueStorage()
    private var onSendMessageActionCount = 0
    private var onSendMessageActionArgMsg: String? = null
    private var onSendMessageActionArgSource: MessageSource? = null
    private var onSendMessageActionException: Throwable? = null

    @Before
    fun beforeTest() {
        realm = FakeRealmGateway()
    }

    @After
    fun afterTest() {
        realm.tearDownTest()
    }

    private fun createInstance(): MessageRepository {
        return MessageRepositoryImpl(
            realmGateway = realm,
            storage = keyValueStorage,
        ).also {
            it.addOnSendMessageAction { source, encryptedMessage ->
                onSendMessageActionCount++
                onSendMessageActionArgSource = source
                onSendMessageActionArgMsg = encryptedMessage
                onSendMessageActionException?.let { ex -> throw ex }
            }
        }
    }

    @Test
    fun testGetAllEmpty() {
        val instance = createInstance()

        val result = instance.getAll()

        assertEquals(emptyList<Message>(), result)
    }

    @Test
    fun testGetAll() = runTest {
        realm.instance.write {
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        val result = instance.getAll()

        assertEquals(listOf(messageA), result)
    }

    @Test
    fun testAddAll() = runTest {
        val instance = createInstance()

        instance.addAll(listOf(messageA))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        assertEquals(messageA, stored.first().toEntity())
    }

    @Test
    fun testObserve() = runTest {
        realm.instance.write {
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        val result = instance.observe(encryptionId = messageRealmA.encryptionId)

        val content = result.take(1).toList().first()
        assertEquals(listOf(messageA), content)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun testSendMessageWithInvalidEncryption() = runTest {
        val instance = createInstance()

        instance.sendMessage(encryptionId = "Invalid", message = messageA.message)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun testSendMessageWithSmsError() = runTest {
        encryptionRealmA.source = MessageSource.Sms("123").serialize()
        realm.instance.write {
            copyToRealm(encryptionRealmA)
        }

        onSendMessageActionException = java.lang.IllegalArgumentException()
        val instance = createInstance()

        instance.sendMessage(encryptionId = messageA.encryptionId, message = messageA.message)
    }

    @Test
    fun testSendMessage() = runTest {
        realm.instance.write {
            copyToRealm(encryptionRealmA)
        }
        val instance = createInstance()

        instance.sendMessage(encryptionId = messageA.encryptionId, message = messageA.message)

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        val storedMessage = stored.first().toEntity()
        assertEquals(messageA.message, storedMessage.message)
        assertEquals(MessageOwnership.OWN, storedMessage.ownership)
        assertEquals(1, onSendMessageActionCount)
        assertEquals(encryptionRealmA.source.deserializeMessageSource(), onSendMessageActionArgSource)
        assertEquals(storedMessage.encryptedMessage, onSendMessageActionArgMsg)
    }

    @Test
    fun testDelete() = runTest {
        realm.instance.write {
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        instance.delete(setOf(messageA.id))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(0, stored.size)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun testDeleteNonExisting() = runTest {
        val instance = createInstance()

        instance.delete(setOf(messageA.id))
    }

    @Test
    fun testSetFavorite() = runTest {
        realm.instance.write {
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        instance.setFavorite(setOf(messageA.id))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        val storedMessage = stored.first().toEntity()
        assertEquals(true, storedMessage.isFavorite)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun testSetFavoriteNonExisting() = runTest {
        val instance = createInstance()

        instance.setFavorite(setOf(messageA.id))
    }

    @Test
    fun testSetFavoriteTwice() = runTest {
        realm.instance.write {
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        instance.setFavorite(setOf(messageA.id))
        instance.setFavorite(setOf(messageA.id))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        val storedMessage = stored.first().toEntity()
        assertEquals(true, storedMessage.isFavorite)
    }

    @Test
    fun testUnsetFavorite() = runTest {
        realm.instance.write {
            messageRealmA.isFavorite = true
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        instance.unsetFavorite(setOf(messageA.id))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        val storedMessage = stored.first().toEntity()
        assertEquals(false, storedMessage.isFavorite)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun testUnsetFavoriteNonExisting() = runTest {
        val instance = createInstance()

        instance.unsetFavorite(setOf(messageA.id))
    }

    @Test
    fun testUnsetFavoriteTwice() = runTest {
        realm.instance.write {
            messageRealmA.isFavorite = true
            copyToRealm(messageRealmA)
        }
        val instance = createInstance()

        instance.unsetFavorite(setOf(messageA.id))
        instance.unsetFavorite(setOf(messageA.id))

        val stored = realm.instance.query<MessageRealm>().find()
        assertEquals(1, stored.size)
        val storedMessage = stored.first().toEntity()
        assertEquals(false, storedMessage.isFavorite)
    }

    @Test
    fun testGetVisibilityPreferenceDefault() = runTest {
        val instance = createInstance()

        val result = instance.getVisibilityPreference()

        assertEquals(MessageRepositoryImpl.defaultVisibility, result)
    }

    @Test
    fun testGetVisibilityPreference() = runTest {
        val visibility = false
        keyValueStorage.map[MessageRepositoryImpl.visibilityKey] = visibility
        val instance = createInstance()

        val result = instance.getVisibilityPreference()

        assertEquals(visibility, result)
    }

    @Test
    fun testSetVisibilityPreference() = runTest {
        val instance = createInstance()

        val visibility = false
        instance.setVisibilityPreference(visibility)

        val storedVisibility = keyValueStorage.map[MessageRepositoryImpl.visibilityKey]
        assertEquals(visibility, storedVisibility)
    }
}