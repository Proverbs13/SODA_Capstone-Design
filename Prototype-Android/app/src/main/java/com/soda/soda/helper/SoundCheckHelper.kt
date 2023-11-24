package com.soda.soda.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soda.soda.DialogInterface
import com.soda.soda.MainActivity
import com.soda.soda.R
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.log10

var DECIBEL_THRESHOLD = 100
private const val TAG = "SoundCheckHelper"

object SoundCheckHelper{
    private lateinit var buffer: TensorBuffer
    private val channelId = "VibrateChannel"
    private val notificationId = 2
    lateinit var warningLabel: String
    lateinit var warningLock: String
    var soundDecibel: Int = 0
    private var dialogInterface: DialogInterface? = null
    private var isNotifying = false
    private var savedPhoneNumber: String? = null
    private var savedMessage: String? = null

    // 디폴트 번호와 메시지 설정
    val phoneNumber = "01050980318" // 대상 전화번호
    val message = "테스트!!" // 보낼 메시지 내용

    init {
        // SoundCheckHelper 객체 초기화 시 디폴트 전화번호와 메시지 설정
        savedPhoneNumber = phoneNumber
        savedMessage = message
    }

    fun soundCheck(tensorAudio: TensorAudio, bytesRead: Int, context: Context) {
        buffer = tensorAudio.tensorBuffer
        // 버퍼에서 읽은 데이터 -> 소리 크기로 변환후 확인
        var soundAmplitude = 0.0
        for (i in 0 until bytesRead) {
            val sample = buffer.getFloatValue(i)
            soundAmplitude += sample * sample
        }

        // Root Mean Square (RMS) 계산
        val rms = kotlin.math.sqrt(soundAmplitude / bytesRead)

        // RMS를 데시벨로 변환 (0에 로그를 취하는 것을 방지하기 위해 1e-7 추가)
        soundDecibel = (30 * log10(rms * 5000 + 1e-7) - 5).toInt()
        if(soundDecibel < 0) Log.d(TAG, "soundDecibel: 0")
        else Log.d(TAG, "soundDecibel: $soundDecibel")
        Log.d(TAG, "DECIBEL_THRESHOLD: $DECIBEL_THRESHOLD")

        DECIBEL_THRESHOLD = 10
        // 소리 크기가 임계값 이상 -> 핸드폰 진동
        if (soundDecibel >= DECIBEL_THRESHOLD) {
            try {
                if(AudioClassificationHelper.label == null) return // 아직 분류가 안됨
                Log.d(TAG, "label: ${AudioClassificationHelper.label}")

                if(AudioClassificationHelper.label != "자동차 경적 소리 같습니다."
                    && AudioClassificationHelper.label != "트럭 경적 소리 같습니다."
                    && AudioClassificationHelper.label != "기차 경적 소리 같습니다."
                    && AudioClassificationHelper.label != "경보 소리 같습니다."
                    && AudioClassificationHelper.label != "화재 경보 소리 같습니다."
                    && AudioClassificationHelper.label != "자동차 도난 경보 소리 같습니다."
                    && AudioClassificationHelper.label != "사이렌 소리 같습니다."
                    && AudioClassificationHelper.label != "구급차 사이렌 소리 같습니다."
                    && AudioClassificationHelper.label != "소방차 사이렌 소리 같습니다."
                    && AudioClassificationHelper.label != "경찰차 사이렌 소리 같습니다."
                    && AudioClassificationHelper.label != "민방위 사이렌 소리 같습니다."
                    && AudioClassificationHelper.label != "비명 소리 같습니다."
                    && AudioClassificationHelper.label != "어린 아이 비명 소리 같습니다."
                    && AudioClassificationHelper.label != "울부짖는 소리 같습니다."
                    && AudioClassificationHelper.label != "고함 소리 같습니다."
                    && AudioClassificationHelper.label != "쾅 소리 같습니다."
                    && AudioClassificationHelper.label != "폭발 소리 같습니다."
                    && AudioClassificationHelper.label != "포격 소리 같습니다."
                    && AudioClassificationHelper.label != "부서지는 소리 같습니다."
                    && AudioClassificationHelper.label != "깨지는 소리 같습니다."
                    && AudioClassificationHelper.label != "폭발 소리 같습니다."
                    && AudioClassificationHelper.label != "부딪치는 소리 같습니다."
                ){
                    return
                }

                createNotification(context)

            } catch (e: Exception) {
                Log.e(TAG, "Error occurred while notifying", e)
                val exceptionMessage = e.message
                Log.e(TAG, "Exception message: $exceptionMessage")
            }
        }
    }

    /** 위험알림 생성 **/
    private fun createNotification(context: Context) {
        if (isNotifying) return // 이미 위험 알림중
        isNotifying = true

        warningLabel = AudioClassificationHelper.label!! + " 주의하세요!"
        warningLock = (AudioClassificationHelper.label?.substring(0, AudioClassificationHelper.label.length - 5) ?: "") + " 발생!!"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Warning Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림을 클릭했을 때 실행될 Intent 정의
        val openAppIntent = Intent(context, MainActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        openAppIntent.putExtra("show_dialog", true) // 'show_dialog' 키에 'true' 값을 추가 -> 해당 알림을 클릭했을 때만 대화상자 띄우기 위해

        // 알림을 클릭했을 때 openAppIntent 실행
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentText(warningLabel)
            .setSmallIcon(R.drawable.ic_warning)
            .setAutoCancel(true) // 알림 탭할 시 ->  제거
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId, notification)

        dialogInterface?.dialogEvents()

        // 3초 후 다시 위험 알림
        Handler(Looper.getMainLooper()).postDelayed({
            isNotifying = false
        }, 3000)
    }

    fun setInterface(dialogInterface: DialogInterface?) {
        this.dialogInterface = dialogInterface
    }

}