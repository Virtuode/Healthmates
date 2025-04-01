package com.corps.healthmate.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

import com.corps.healthmate.R
import com.corps.healthmate.ZegoCredentials

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

import java.text.SimpleDateFormat
import java.util.Locale

class VideoCallActivity : AppCompatActivity() {

    private var callFragment: ZegoUIKitPrebuiltCallFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        val roomID = intent.getStringExtra("roomID")
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        if (roomID == null || userID == null) {
            finish()
            return
        }

        addCallFragment(roomID, userID)
    }

    private fun addCallFragment(roomID: String, userID: String) {
        val database = FirebaseDatabase.getInstance()
        val patientRef = database.getReference("patients/$userID")
        patientRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("survey/basicInfo/firstName").getValue(String::class.java) ?: "Patient"
                val lastName = snapshot.child("survey/basicInfo/lastName").getValue(String::class.java) ?: ""
                val userName = "$firstName $lastName".trim()

                val appID = ZegoCredentials.ZEGO_APP_ID
                val appSign = ZegoCredentials.ZEGO_APP_SIGN
                val config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()

                try {
                    callFragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                        appID,
                        appSign,
                        userID,
                        userName,
                        roomID,
                        config
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                    return
                }

                try {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.video_call_container, callFragment!!)
                        .commitNow()
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                    return
                }

                val chatRef = database.getReference("chats/$roomID")
                chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val appointmentTime = snapshot.child("appointmentTime").getValue(String::class.java)
                        if (appointmentTime != null) {
                            enforceTimeRestriction(appointmentTime)
                        } else {
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        finish()
                    }
                })

                findViewById<View>(R.id.end_call_button)?.setOnClickListener {
                    callFragment?.endCall()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val userName = "Patient_$userID"
                val appID = ZegoCredentials.ZEGO_APP_ID
                val appSign = ZegoCredentials.ZEGO_APP_SIGN
                val config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()

                try {
                    callFragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                        appID,
                        appSign,
                        userID,
                        userName,
                        roomID,
                        config
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                    return
                }

                try {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.video_call_container, callFragment!!)
                        .commitNow()
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                    return
                }

                val chatRef = database.getReference("chats/$roomID")
                chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val appointmentTime = snapshot.child("appointmentTime").getValue(String::class.java)
                        if (appointmentTime != null) {
                            enforceTimeRestriction(appointmentTime)
                        } else {
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        finish()
                    }
                })

                findViewById<View>(R.id.end_call_button)?.setOnClickListener {
                    callFragment?.endCall()
                }
            }
        })
    }

    private fun enforceTimeRestriction(appointmentTime: String) {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentDateTime = dateTimeFormat.parse(appointmentTime) ?: return

        val slotEnd = appointmentDateTime.time + 30 * 60 * 1000 // 30 minutes
        val currentTime = System.currentTimeMillis()

        if (currentTime > slotEnd) {
            callFragment?.endCall()
            return
        }

        val timeUntilEnd = slotEnd - currentTime
        Handler(Looper.getMainLooper()).postDelayed({
            callFragment?.endCall()
        }, timeUntilEnd)
    }

    override fun onDestroy() {
        super.onDestroy()
        callFragment?.endCall()
    }
}