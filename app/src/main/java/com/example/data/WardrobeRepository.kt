package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WardrobeRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Wardrobe.")
    }

    fun observeWardrobeItems(userId: String = requireUserId()): Flow<List<WardrobeItem>> = flow {
        val path = COLLECTION_WARDROBE
        emitAll(
            db.collection(COLLECTION_WARDROBE)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            WardrobeItem::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserWardrobeItems(): Result<List<WardrobeItem>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = COLLECTION_WARDROBE
        return try {
            val snapshot = db.collection(COLLECTION_WARDROBE)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    WardrobeItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getWardrobeItemById(itemId: String): Result<WardrobeItem?> {
        val docRef = db.collection(COLLECTION_WARDROBE).document(itemId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(
                snapshot.toObject(
                    WardrobeItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createWardrobeItem(item: WardrobeItem): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val collectionRef = db.collection(COLLECTION_WARDROBE)
        val docRef = if (item.id.isNotBlank()) {
            collectionRef.document(item.id)
        } else {
            collectionRef.document()
        }
        val payload = item.toCreatePayload(uid)
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateWardrobeItem(item: WardrobeItem): Result<Unit> {
        requireUserId()
        require(item.id.isNotBlank()) { "WardrobeItem ID must not be blank for update" }
        val docRef = db.collection(COLLECTION_WARDROBE).document(item.id)
        val updates = mapOf(
            "name" to item.name.trim().take(100),
            "imageUrl" to item.imageUrl,
            "category" to ClothingCategory.fromDb(item.category).dbValue,
            "weather" to WeatherType.fromDb(item.weather).dbValue,
            "style" to StyleType.fromDb(item.style).dbValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWardrobeItem(itemId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_WARDROBE).document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterWardrobe(isArabic: Boolean): Result<Int> {
        val starter = SampleWardrobeData.getStarterItems(isArabic)
        var count = 0
        for (item in starter) {
            val res = createWardrobeItem(item)
            if (res.isSuccess) count++
        }
        return Result.success(count)
    }

    companion object {
        const val COLLECTION_WARDROBE = "wardrobe"
    }
}
