package com.corps.healthmate.repository

import com.corps.healthmate.models.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale

@Singleton
class MedicineRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val medicineCollection = firestore.collection("medicines")
    private var lastDocument: DocumentSnapshot? = null

    suspend fun searchMedicines(query: String, isNewSearch: Boolean = false): Flow<List<Medicine>> = flow {
        try {
            if (isNewSearch) {
                lastDocument = null
            }

            var firestoreQuery = medicineCollection
                .orderBy("name")
                .limit(20)

            if (query.isNotBlank()) {
                firestoreQuery = medicineCollection
                    .orderBy("name")
                    .whereGreaterThanOrEqualTo("name", query.lowercase(Locale.getDefault()))
                    .whereLessThanOrEqualTo("name", query.lowercase(Locale.getDefault()) + '\uf8ff')
                    .limit(20)
            }

            // Add startAfter if we're paginating
            if (lastDocument != null) {
                firestoreQuery = firestoreQuery.startAfter(lastDocument!!)
            }

            val snapshot = firestoreQuery.get().await()
            
            if (snapshot.documents.isNotEmpty()) {
                lastDocument = snapshot.documents[snapshot.size() - 1]
            }
            
            val medicines = snapshot.toObjects(Medicine::class.java)
            Timber.d("Found ${medicines.size} medicines")
            emit(medicines)
        } catch (e: Exception) {
            Timber.e(e, "Error searching medicines: ${e.message}")
            emit(emptyList())
        }
    }

    fun resetPagination() {
        lastDocument = null
    }

    suspend fun getInitialMedicines(limit: Int = 20): Flow<List<Medicine>> = flow {
        try {
            val snapshot = medicineCollection
                .limit(limit.toLong())
                .get()
                .await()
            
            emit(snapshot.toObjects(Medicine::class.java))
        } catch (e: Exception) {
            Timber.e(e, "Error getting initial medicines")
            emit(emptyList())
        }
    }


    suspend fun verifyMedicineData(): Boolean {
        return try {
            val count = medicineCollection
                .get()
                .await()
                .size()
            
            Timber.d("Found $count medicines in Firestore")
            count > 0
        } catch (e: Exception) {
            Timber.e(e, "Error verifying medicine data")
            false
        }
    }
} 