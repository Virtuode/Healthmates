package com.corps.healthmate.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object FirebaseReferenceManager {
    private val syncedReferences = ConcurrentHashMap<String, Boolean>()
    
    fun keepSynced(path: String) {
        if (syncedReferences.containsKey(path)) {
            Timber.d("Reference already synced: $path")
            return
        }
        
        try {
            val reference = FirebaseDatabase.getInstance().reference.child(path)
            reference.keepSynced(true)
            syncedReferences[path] = true
            Timber.d("Successfully enabled sync for: $path")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable sync for: $path")
        }
    }

    fun getReference(path: String): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child(path)
    }

    fun clearSyncedReferences() {
        syncedReferences.keys.forEach { path ->
            try {
                val reference = FirebaseDatabase.getInstance().reference.child(path)
                reference.keepSynced(false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear sync for: $path")
            }
        }
        syncedReferences.clear()
    }
} 