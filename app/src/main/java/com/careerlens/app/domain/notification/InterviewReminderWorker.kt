package com.careerlens.app.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

class InterviewReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val company = inputData.getString(KEY_COMPANY) ?: "Upcoming Interview"
        val role = inputData.getString(KEY_ROLE) ?: ""
        val roundType = inputData.getString(KEY_ROUND_TYPE) ?: "Interview"

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Interview Reminders (Offline)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Local reminders for upcoming CareerLens interviews and prep checklists"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Interview Prep: $company ($roundType)")
            .setContentText("Role: $role — Open CareerLens to review your checklist & questions.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(company.hashCode(), notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "careerlens_interview_channel"
        private const val KEY_COMPANY = "company"
        private const val KEY_ROLE = "role"
        private const val KEY_ROUND_TYPE = "round_type"

        fun scheduleReminder(
            context: Context,
            interviewId: Long,
            companyName: String,
            roleTitle: String,
            roundLabel: String,
            scheduledAtEpoch: Long
        ) {
            // Remind 2 hours before the interview (or 10 seconds from now if testing near-term)
            val twoHoursBefore = scheduledAtEpoch - TimeUnit.HOURS.toMillis(2)
            val delayMs = max(5_000L, twoHoursBefore - System.currentTimeMillis())

            val data = workDataOf(
                KEY_COMPANY to companyName,
                KEY_ROLE to roleTitle,
                KEY_ROUND_TYPE to roundLabel
            )

            val request = OneTimeWorkRequestBuilder<InterviewReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "interview_reminder_$interviewId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
