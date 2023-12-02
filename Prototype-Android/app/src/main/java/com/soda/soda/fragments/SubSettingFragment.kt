package com.soda.soda.fragments

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.soda.soda.MainActivity
import com.soda.soda.databinding.FragmentSubSettingBinding
import com.soda.soda.service.ForegroundService
import kotlin.properties.Delegates

class SubSettingFragment : Fragment(){
    private var _binding: FragmentSubSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var serviceIntent: Intent
    private lateinit var stopServiceIntent: Intent

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = requireActivity() as MainActivity

        serviceIntent = Intent(mainActivity, ForegroundService::class.java)
        stopServiceIntent = Intent(mainActivity, ForegroundService::class.java)

        /** 백그라운드 스위치 **/
        binding.backgroundSwitch.isChecked = backgroundSwitchState
        binding.backgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            val sharedPref = context?.getSharedPreferences("background_shared_pref", Context.MODE_PRIVATE) ?: return@setOnCheckedChangeListener
            with (sharedPref.edit()) {
                putBoolean("background_switch_state", isChecked) //"saved_switch_state_key"라는 키 값으로 isChecked라는 값을 SharedPreferences에 저장
                apply()
                backgroundSwitchState = isChecked
            }
            if (isChecked) {
                ContextCompat.startForegroundService(mainActivity, serviceIntent)
            } else {
                mainActivity.stopService(stopServiceIntent)
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var backgroundSwitchState by Delegates.notNull<Boolean>()

        fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        /** 스위치 상태 설정 **/
        fun setSwitchState(context: Context, sharedPref: String, key: String, defValue: Boolean): Boolean{
            //SharedPreferences에서 스위치 상태 가져옴
            val sharedPref = context.getSharedPreferences(sharedPref, Context.MODE_PRIVATE)
            //sharedPref.getBoolean은 이 키 값에 해당하는 값이 SharedPreferences에 저장되어 있으면 그 값을 반환하고 없으면 defValue를 반환
            return sharedPref.getBoolean(key, defValue)
        }

    }

}