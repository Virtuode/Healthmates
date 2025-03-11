package com.corps.healthmate.utils

import android.content.Context
import com.corps.healthmate.models.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class MedicineDataLoader(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun loadMedicinesIntoFirestore() {
        try {
            Timber.d("Starting medicine data load process...")
            
            // Check if data already exists
            val existingCount = firestore.collection("medicines")
                .get()
                .await()
                .size()

            Timber.d("Current medicine count in Firestore: $existingCount")

            // Read and parse JSON
            val jsonString = try {
                context.assets.open("medicines.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read medicines.json")
                throw e
            }

            Timber.d("Successfully read medicines.json, length: ${jsonString.length}")

            // Parse JSON to objects
            val medicineType = object : TypeToken<List<Medicine>>() {}.type
            val medicines: List<Medicine> = try {
                Gson().fromJson(jsonString, medicineType)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse JSON")
                throw e
            }

            Timber.d("Parsed ${medicines.size} medicines from JSON")

            if (medicines.isEmpty()) {
                Timber.e("No medicines parsed from JSON")
                return
            }

            // Process in smaller batches
            val batchSize = 400
            medicines.chunked(batchSize).forEachIndexed { index, batch ->
                Timber.d("Processing batch ${index + 1}")
                val writeBatch = firestore.batch()
                
                batch.forEach { medicine ->
                    val docRef = firestore.collection("medicines").document(medicine.id)
                    val medicineData = medicine.copy(name = medicine.name.lowercase())
                    writeBatch.set(docRef, medicineData)
                }
                
                try {
                    writeBatch.commit().await()
                    Timber.d("Successfully committed batch ${index + 1}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to commit batch ${index + 1}")
                    throw e
                }
            }

            val finalCount = firestore.collection("medicines")
                .get()
                .await()
                .size()
            
            Timber.d("Final medicine count in Firestore: $finalCount")

        } catch (e: Exception) {
            Timber.e(e, "Failed to load medicines into Firestore")
            throw e
        }
    }

    suspend fun forceReloadMedicines() {
        try {
            // Delete existing medicines
            val existingMedicines = firestore.collection("medicines")
                .get()
                .await()
            
            if (!existingMedicines.isEmpty) {
                val batch = firestore.batch()
                existingMedicines.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                Timber.d("Deleted existing medicines")
            }
            
            // Load new data
            loadMedicinesIntoFirestore()
        } catch (e: Exception) {
            Timber.e(e, "Failed to force reload medicines")
            throw e
        }
    }
} 