package com.ledger.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ledger.app.R
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.WriteNoteEntity
import com.ledger.app.data.WriteTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object WriteReminderScheduler {
    const val NEVER = "Never"
    private const val CHANNEL = "write_reminders"
    private fun pending(context: Context, id: Long, type: String, title: String, repeat: String, at: Long, flags: Int): PendingIntent {
        val intent = Intent(context, WriteReminderReceiver::class.java).apply {
            putExtra("id", id); putExtra("type", type); putExtra("title", title); putExtra("repeat", repeat); putExtra("scheduledAt", at)
        }
        return PendingIntent.getBroadcast(context, (if (type == "note") 100000 else 200000) + id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun schedule(context: Context, id: Long, type: String, title: String, at: Long?, repeat: String) {
        cancel(context, id, type)
        if (at == null || at <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, id, type, title, repeat, at, 0)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
    fun cancel(context: Context, id: Long, type: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, id, type, "", NEVER, 0L, 0)
        am.cancel(pi); pi.cancel()
    }
    fun next(at: Long, repeat: String): Long? {
        if (repeat == NEVER) return null
        val c = Calendar.getInstance().apply { timeInMillis = at }
        when (repeat) {
            "Daily" -> c.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> c.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> c.add(Calendar.MONTH, 1)
            "Yearly" -> c.add(Calendar.YEAR, 1)
            else -> return null
        }
        return c.timeInMillis
    }
    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = LedgerDatabaseProvider.get(context)
            db.writeNoteDao().reminders().forEach { n -> schedule(context, n.id, "note", n.title, n.reminderAt, n.repeatRule) }
            db.writeTaskDao().reminders().forEach { t -> schedule(context, t.id, "task", t.title, t.reminderAt, t.repeatRule) }
        }
    }
}

class WriteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val type = intent.getStringExtra("type") ?: "note"
        val title = intent.getStringExtra("title") ?: "Reminder"
        val repeat = intent.getStringExtra("repeat") ?: "Never"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("write_reminders", "Notes & Tasks", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(context, "write_reminders")
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(if (type == "task") "Task reminder" else "Note reminder")
            .setContentText(title.ifBlank { "You have a reminder" }).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(context).notify((if (type == "note") 100000 else 200000) + id.toInt(), notification) }
        val next = intent.getLongExtra("scheduledAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis()
        val nextAt = WriteReminderScheduler.next(next, repeat)
        if (nextAt != null) WriteReminderScheduler.schedule(context, id, type, title, nextAt, repeat)
    }
}

class WriteReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            WriteReminderScheduler.rescheduleAll(context)
        }
    }
}
