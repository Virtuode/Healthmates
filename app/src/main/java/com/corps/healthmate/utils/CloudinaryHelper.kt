package com.corps.healthmate.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {
    private var isInitialized = false

    // Initialization of Cloudinary
    fun init(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "ds6okhko7",
                "api_key" to "299348186728561",
                "api_secret" to "jJXVUk3qxYTNLuDXrbu52vyhNT4"
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }

    // Upload image and return the URL
    suspend fun uploadImage(imageUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Optional: Add logging or progress tracking here if needed
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Optional: Can add progress feedback here
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["url"] as? String
                        if (imageUrl != null) {
                            continuation.resume(imageUrl)
                        } else {
                            continuation.resumeWithException(Exception("Image URL not found in the response"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception("Upload failed: ${error.description}"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Optional: Handle rescheduling if needed
                    }
                }).dispatch()
        }
    }

    // Centralized function to handle profile image upload and storage
    suspend fun uploadProfileImage(imageUri: Uri, userId: String): String {
        try {
            // Upload image to Cloudinary
            val imageUrl = uploadImage(imageUri)
            
            // Convert to HTTPS immediately
            val secureUrl = if (imageUrl.startsWith("http://")) {
                imageUrl.replace("http://", "https://")
            } else {
                imageUrl
            }
            
            println("CloudinaryHelper: Image uploaded successfully to Cloudinary. URL: $secureUrl")
            
            // Store URL in Firebase
            storeImageUrlInFirebase(secureUrl, userId)
            
            return secureUrl
        } catch (e: Exception) {
            println("CloudinaryHelper: Error during profile image upload: ${e.message}")
            throw Exception("Failed to upload profile image: ${e.message}")
        }
    }

    // Store image URL in Firebase under survey node for patients
    private fun storeImageUrlInFirebase(imageUrl: String, userId: String) {
        // Convert HTTP to HTTPS if needed
        val secureUrl = if (imageUrl.startsWith("http://")) {
            imageUrl.replace("http://", "https://")
        } else {
            imageUrl
        }

        println("CloudinaryHelper: Storing secure image URL: $secureUrl")

        // Update directly to basicInfo/imageUrl
        FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)
            .child("survey")
            .child("basicInfo")
            .child("imageUrl")
            .setValue(secureUrl)
            .addOnSuccessListener {
                println("CloudinaryHelper: Successfully stored image URL in Firebase")
            }
            .addOnFailureListener { e ->
                println("CloudinaryHelper: Failed to store image URL in Firebase: ${e.message}")
            }
    }
}