package com.ledger.app.cloudgroup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ledger.app.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LedgerFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "LedgerFCM"
        private const val CHANNEL_ID = "group_notifications"
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            FcmTokenRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val notificationType = data["type"]
        val defaultTitle = when (notificationType) {
            "attendance_approval" -> "Attendance approval required"
            "staff_invitation" -> "Attendance invitation"
            else -> "Added to a Group"
        }
        val defaultBody = when (notificationType) {
            "attendance_approval" -> "A staff member has submitted attendance for approval."
            "staff_invitation" -> "You have received a staff attendance invitation."
            else -> "You have been added to a group."
        }

        val title = data["title"] ?: defaultTitle
        val body = data["body"] ?: defaultBody
        val groupId = data["group_id"]
        val requestId = data["request_id"]
        val revision = data["revision"]
        val staffId = data["staff_id"]
        val workplaceId = data["workplace_id"]

        Log.d(
            TAG,
            "Notification received. type=$notificationType, groupId=$groupId, " +
                "requestId=$requestId, staffId=$staffId"
        )

        showNotification(
            title = title,
            body = body,
            groupId = groupId,
            requestId = requestId,
            revision = revision,
            notificationType = notificationType,
            staffId = staffId,
            workplaceId = workplaceId
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        groupId: String?,
        requestId: String?,
        revision: String?,
        notificationType: String?,
        staffId: String?,
        workplaceId: String?
    ) {
        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ledger Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Group, expense and attendance notifications"
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (!groupId.isNullOrBlank()) putExtra("group_id", groupId)
            if (!requestId.isNullOrBlank()) putExtra("request_id", requestId)
            if (!revision.isNullOrBlank()) putExtra("revision", revision)
            if (!notificationType.isNullOrBlank()) {
                putExtra("notification_type", notificationType)
            }
            if (!staffId.isNullOrBlank()) putExtra("staff_id", staffId)
            if (!workplaceId.isNullOrBlank()) {
                putExtra("workplace_id", workplaceId)
            }
        }

        val notificationKey = listOfNotNull(
            notificationType,
            requestId,
            staffId,
            groupId,
            workplaceId
        ).joinToString(":").ifBlank { System.currentTimeMillis().toString() }
        val notificationCode = notificationKey.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationCode, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
