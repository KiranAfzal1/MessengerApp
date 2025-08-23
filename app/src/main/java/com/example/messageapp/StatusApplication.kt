package com.example.messageapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class StatusApplication : Application() {

    private val databaseUrl =
        "https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleListener())
    }

    inner class AppLifecycleListener : ActivityLifecycleCallbacks {
        private var activityReferences = 0
        private var isActivityChangingConfigurations = false

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {
            activityReferences++
            if (activityReferences == 1 && !isActivityChangingConfigurations) {
                setCurrentUserStatus(true)
            }
        }

        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {
            isActivityChangingConfigurations = activity.isChangingConfigurations
            activityReferences--
            if (activityReferences == 0 && !isActivityChangingConfigurations) {
                setCurrentUserStatus(false)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun setCurrentUserStatus(isOnline: Boolean) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users")
            .child(currentUserId)

        val updateMap = HashMap<String, Any>()
        updateMap["isOnline"] = isOnline
        updateMap["lastSeen"] = System.currentTimeMillis()
        userRef.updateChildren(updateMap)
    }
}
