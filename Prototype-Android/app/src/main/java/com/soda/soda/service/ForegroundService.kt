package com.soda.soda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.soda.soda.MainActivity
import com.soda.soda.R
import com.soda.soda.fragments.AudioFragment
import com.soda.soda.helper.AudioClassificationHelper

private const val TAG = "ForegroundService"

class ForegroundService : Service() {
    private val channelId = "ForegroundServiceChannel"
    private val notificationId = 1
    private val handler = Handler(Looper.getMainLooper())
    private var interval = 1000L // 1초
    private val updateRunnable = object : Runnable {
        override fun run() {
            if(AudioClassificationHelper.label != null){
                interval = AudioClassificationHelper.interval
                updateForegroundNotification() // 알림 업데이트 작업 수행
            }
            handler.postDelayed(this, interval) // 주기적으로 실행
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AudioFragment.startRecording()

        if(AudioClassificationHelper.label != null){
            val notification = createNotification()
            startForeground(notificationId, notification)
        }

        // 주기적으로 알림 업데이트를 위해 Runnable 실행
        handler.postDelayed(updateRunnable, interval)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Runnable 중지
        handler.removeCallbacks(updateRunnable)

        // 포그라운드 서비스 종료
        stopForeground(true)
        stopSelf()

        AudioFragment.stopRecording()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // 알림 클릭 시 실행될 Intent 정의
        val openAppIntent = Intent(this, MainActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        var label = AudioClassificationHelper.label
        // 알림을 클릭시 openAppIntent를 실행

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_run_service)
            .setContentText(label)
            .setContentIntent(pendingIntent) // 알림 탭했을 시 pendingIntent 실행
            .setAutoCancel(true) // 알림을 탭한 후 자동으로 제거
            .build()

        return notification
    }

    /** 포그라운드 알림을 업데이트하는 함수 **/
    private fun updateForegroundNotification() {
        val notification = createNotification() // 새로운 알림 생성
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification) // 알림 업데이트
    }

}