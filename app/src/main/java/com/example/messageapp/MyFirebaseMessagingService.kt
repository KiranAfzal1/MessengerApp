package com.example.messageapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        saveTokenToDatabase(token)
    }

    private fun saveTokenToDatabase(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(uid).child("fcm_token").setValue(token)
            .addOnSuccessListener { Log.d("FCM", "Token saved to DB") }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received: ${remoteMessage.data}")
        // You can show notifications here if needed
    }
}
