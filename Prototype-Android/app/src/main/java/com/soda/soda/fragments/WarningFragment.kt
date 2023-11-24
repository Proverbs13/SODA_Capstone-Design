package com.soda.soda.fragments;

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.soda.soda.databinding.FragmentWarningBinding
import android.os.Handler
import android.os.Looper


/** 위험알림 다이얼로그 **/
class WarningFragment(
    text: String
) : DialogFragment() {

    private var _binding: FragmentWarningBinding? = null
    private val binding get() = _binding!!
    private var text: String? = null
    private var isRedBackground = true // 빨간색 배경 여부
    private val handler = Handler(Looper.getMainLooper())
    private val backgroundColorRunnable = object : Runnable {
        override fun run() {
            val backgroundResId = if (isRedBackground) {
                com.soda.soda.R.drawable.dialog_background_red
            } else {
                com.soda.soda.R.drawable.dialog_background_blue
            }
            view?.setBackgroundResource(backgroundResId) // 배경 리소스 설정
            isRedBackground = !isRedBackground // 배경 색상 변경 여부 업데이트

            // 배경 색상 변경될 때마다 텍스트 색상 업데이트
            updateTextColors(isRedBackground)

            handler.postDelayed(this, 200) // 0.2초마다 반복
        }
    }


    init {
        this.text = text
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarningBinding.inflate(inflater, container, false)
        val view = binding.root
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 레이아웃 배경 투명화
        binding.confirmTextView.text = text


        // 배경 색상 변경 시작
        handler.post(backgroundColorRunnable)


        /** 확인 버튼 클릭 이벤트 **/
        binding.checkButton.setOnClickListener {
            dismiss()
        }
        return view
    }


    private fun updateTextColors(isRedBackground: Boolean) {
        val textColorResId = if (isRedBackground) {
            android.R.color.black
        } else {
            android.R.color.white
        }
        val textColor = requireContext().getColor(textColorResId)

        // 텍스트 색상 업데이트
        binding.confirmTextView.setTextColor(textColor)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(backgroundColorRunnable) // 핸들러 콜백 중지
        _binding = null
    }

}
