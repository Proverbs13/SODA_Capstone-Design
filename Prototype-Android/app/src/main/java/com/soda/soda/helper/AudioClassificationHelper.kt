/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soda.soda.helper

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import com.soda.soda.fragments.AudioClassificationListener
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.lang.Exception
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private const val TAG = "AudioClassificationHelper"

class AudioClassificationHelper(
    private val context: Context,
    private val listener: AudioClassificationListener,
    private var currentModel: String = YAMNET_MODEL, //현재 모델
    private var classificationThreshold: Float = DISPLAY_THRESHOLD,//분류 임계값
    private var overlap: Float = DEFAULT_OVERLAP_VALUE, //오버랩 값
    private var numOfResults: Int = DEFAULT_NUM_OF_RESULTS, //결과 개수
    private var currentDelegate: Int = 0,// (CPU, NNAPI)
    private var numThreads: Int = 1 //쓰레드 개수

) {
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var executor: ScheduledThreadPoolExecutor
    private lateinit var soundCheckExecutor: ScheduledThreadPoolExecutor
    private var bytesRead by Delegates.notNull<Int>()

    private val classifyRunnable = Runnable {
        try {
            classifyAudio()
        }catch (e: Exception){
            var errorMessage = e.message
            Log.e(TAG, "classifyAudioError: $errorMessage")
        }
    }

    /** 새로운 스레드에서 소리 크기 확인 및 진동 울리기 **/
    private val soundCheckRunnable = Runnable {
        try {
            SoundCheckHelper.soundCheck(tensorAudio, bytesRead, context)
        } catch (e: Exception){ // bytesRead가 초기화되지 않았는데 함수가 실행될 수 있음 + 기타 오류 방지
            var errorMessage = e.message
            Log.e(TAG, "soundcheckError: $errorMessage")
        }
    }

    init {
        initClassifier()
    }

    private fun initClassifier() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(numThreads)

        /** 모델 실행을 위해 지정된 CPU 하드웨어 사용 **/
        when (currentDelegate) {
            DELEGATE_CPU -> {
                //모델은 CPU에서 실행
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold) //분류할 임계값 설정
            .setMaxResults(numOfResults) //분류 결과의 최대 개수
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()
            //필요한 객체를 생성하는 분류기 생성
            recorder = classifier.createAudioRecord()
            //분류 시작
        } catch (e: IllegalStateException) {
            listener.onError(
                "Audio Classifier failed to initialize. See error logs for details"
            )
            Log.e(TAG, "AudioClassificationError: " + e.message)
        }
    }


    /** 녹음 시작 함수 **/
    fun startAudioClassification() {
        recorder.startRecording()
        executor = ScheduledThreadPoolExecutor(1)
        soundCheckExecutor = ScheduledThreadPoolExecutor(1)

        // 각 모델은 특정 오디오 녹음 길이를 예상함.  ex)YAMNET=> 0.975초 길이의 녹음 예상
        // 이 수식은 입력 버퍼 크기와 텐서 형식 샘플 속도를 사용하여 해당 길이를 계산함
        // 필요한 Long 값에서 소수점으로 떨어지는 것을 방지하려면 밀리초 단위 필요
        val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                classifier.requiredTensorAudioFormat.sampleRate) * 1000  //오디오가 얼마나 길어야 하는지 계산

        interval = (lengthInMilliSeconds * (1 - overlap)).toLong() //오버랩 값으로 반복 간격 계산

        executor.scheduleAtFixedRate(
            classifyRunnable,   //-> classifyAudio() 호출
            0,
            interval,
            TimeUnit.MILLISECONDS)

        // 소리 크기 확인 및 진동 실행 로직
        soundCheckExecutor.scheduleAtFixedRate(
            soundCheckRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS
        )
    }

    private fun classifyAudio() {
        bytesRead = tensorAudio.load(recorder)
        val output = classifier.classify(tensorAudio) //분류 실행
        listener.onResult(output[0].categories) //분류 결과를 리스너에게 전달
    }

    fun stopAudioClassification() {
        recorder.stop() //오디오 녹음 중지
        if(executor != null){
            executor?.shutdownNow() //분류 작업 중지
        }
        if(soundCheckExecutor != null){
            soundCheckExecutor.shutdownNow() //데시벨 확인 중지
        }
    }

    fun getRecorderState(): Int {
        return recorder.recordingState
    }

    companion object {
        const val DELEGATE_CPU = 0 //메인 쓰레드에서 작동
        const val DELEGATE_NNAPI = 1 //NN API 딜리게이트 사용
        const val DISPLAY_THRESHOLD = 0.3f //결과 출력에 영향을 미치는 임계선 값
        const val DEFAULT_NUM_OF_RESULTS = 1 //분류 결과의 최대 개수, default 1개 설정
        const val DEFAULT_OVERLAP_VALUE = 0.5f //반복 실행 간격, default 값은 0.5 설정
        const val YAMNET_MODEL = "yamnet.tflite" //사용하는 모델, default 값은 YAMNET 설정
        var interval by Delegates.notNull<Long>()
        var label: String = "소리분류 결과가 여기에 표시됩니다."
        lateinit var recorder: AudioRecord
        var excludedLabel: List<String> = listOf("Silence")
    }

}
