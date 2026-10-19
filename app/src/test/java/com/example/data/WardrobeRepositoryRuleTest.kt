package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class WardrobeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleItem(name: String = "Sage Linen Shirt") = WardrobeItem(
        id = "item_${UUID.randomUUID().toString().replace("-", "")}",
        name = name,
        imageUrl = "preset:top_sage_linen",
        category = ClothingCategory.TOP.dbValue,
        weather = WeatherType.MILD.dbValue,
        style = StyleType.CASUAL.dbValue
    )

    @Test
    fun createWardrobeItem_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = WardrobeRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWardrobeItem(sampleItem())
        }
        assertThat(createResult.isSuccess).isTrue()
        val itemId = createResult.getOrThrow()
        assertThat(itemId).isNotEmpty()
    }

    @Test
    fun getUserWardrobeItems_authenticatedOwner_returnsMatchingItems() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = WardrobeRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWardrobeItem(sampleItem("Oxford Shirt")).getOrThrow()
        }

        val itemsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserWardrobeItems()
        }
        assertThat(itemsResult.isSuccess).isTrue()
        assertThat(itemsResult.getOrThrow().map { it.id }).contains(itemId)
    }

    @Test
    fun observeWardrobeItems_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = WardrobeRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWardrobeItem(sampleItem("Cashmere Coat")).getOrThrow()
        }

        val emittedItems = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWardrobeItems(uid).first { list -> list.any { it.id == itemId } }
        }
        assertThat(emittedItems.map { it.id }).contains(itemId)
    }

    @Test
    fun getWardrobeItemById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = WardrobeRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWardrobeItem(sampleItem("Alice Private Item")).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = WardrobeRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getWardrobeItemById(itemId)
        }
        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull() as? FirebaseFirestoreException
        assertThat(exception?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun observeWardrobeItems_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = WardrobeRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWardrobeItems(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = generateSequence(caught) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        assertThat(firestoreEx).isNotNull()
        assertThat(firestoreEx?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@wardrobe.test"
        const val BOB_EMAIL = "bob@wardrobe.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
