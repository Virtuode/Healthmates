package com.corps.healthmate.utils

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ImagePickerHelper(
    private val onImagePicked: (Uri?) -> Unit
) : DefaultLifecycleObserver {
    private var pickImage: ActivityResultLauncher<String>? = null

    fun register(fragment: Fragment) {
        // Register for activity result during fragment creation
        pickImage = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onImagePicked(uri)
        }
    }

    fun register(activity: FragmentActivity) {
        // Register as lifecycle observer
        activity.lifecycle.addObserver(this)
        
        if (pickImage == null) {
            pickImage = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                onImagePicked(uri)
            }
        }
    }

    fun pickImage() {
        pickImage?.launch("image/*")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        pickImage = null
    }
} 