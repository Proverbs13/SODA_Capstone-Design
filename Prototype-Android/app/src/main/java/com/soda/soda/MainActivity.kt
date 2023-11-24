package com.soda.soda

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import com.soda.soda.databinding.ActivityMainBinding
import com.soda.soda.databinding.ToolbarLayoutBinding
import com.soda.soda.fragments.AudioFragment
import com.soda.soda.fragments.PermissionsFragment
import com.soda.soda.fragments.WarningFragment
import com.soda.soda.helper.SoundCheckHelper

private const val TAG = "MainActivity"

interface DialogInterface{
    fun dialogEvents()
    fun showAuthorizationDialog(context: Context, feature: String)
}

class MainActivity : AppCompatActivity(), DialogInterface{
    private lateinit var activityMainBinding: ActivityMainBinding
    lateinit var toolbarLayoutBinding: ToolbarLayoutBinding
    private var currentDialog: DialogFragment? = null
    private var isPaused = false
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // 앱 권한 설정 화면에서 돌아왔을 때 수행
        this.recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // 인터페이스 설정
        SoundCheckHelper.setInterface(this)

        // 툴바 레이아웃 뷰 가져오기
        val toolbarLayoutView = activityMainBinding.root.findViewById<View>(R.id.toolbar_layout)
        // 툴바 레이아웃 뷰 바인딩
        toolbarLayoutBinding = ToolbarLayoutBinding.bind(toolbarLayoutView)
        setSupportActionBar(toolbarLayoutBinding.toolbar)

        toolbarLayoutBinding.buttonBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        isPaused = false

        // 'show_dialog' 키의 값을 가져옴 (기본값은 false)
        val showDialog = intent.getBooleanExtra("show_dialog", false)
        if (showDialog) {
            dialogEvents()
        }
    }

    /** intent 갱신용 함수**/
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 새로운 Intent를 액티비티의 Intent로 설정
        this.intent = intent
    }

    override fun onDestroy() {
        super.onDestroy()

        // 액티비티가 종료될 때 Object에 설정된 인터페이스 제거
        SoundCheckHelper.setInterface(null)

        // 녹음 및 스레드 작업 종료
        AudioFragment.getAudioHelper()?.stopAudioClassification()

        // audioHelper = null로 초기화 -> 앱을 다시 실행할 때마다 audioHelper를 다시 생성해야 함
        // 앱을 다시 실행할 때마다 MainActivity가 다시 생성되므로
        // AudioFragment에서 다시 requireContext()로 context를 갱신한 후 다시 audioHelper객체를 생성하면서 context를 넘겨줘야 함
        AudioFragment.setAudioHelper()
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    override fun onBackPressed() {
        // 현재 프래그먼트가 SettingFragment인지 확인
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if(currentFragment is NavHostFragment){ // AudioFragment, PermissionsFragment = NavHostFragment
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            val currentNavHostFragment = navHostFragment.childFragmentManager.fragments[0]
            if (currentNavHostFragment is PermissionsFragment) {
                // PermissionsFragment에서 뒤로 가기를 눌렀을 때 앱을 종료함
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition()
                } else {
                    finish()
                }
                return
            }
        }

        // 현재 프래그먼트가 ChattingFragment이면 툴바 타이틀 가시성을 GONE으로 변경
        if (currentFragment is NavHostFragment) {
            toolbarLayoutBinding.buttonSetting.visibility = View.VISIBLE
            toolbarLayoutBinding.toolbarTitle.visibility = View.GONE
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    /** 대화상자 생성 **/
    override fun dialogEvents() {
        // 백그라운드에서 실행된 경우 팝업을 띄우지 않음
        if(isPaused) return

        // 이전에 띄워진 다이얼로그가 있으면 닫기
        currentDialog?.dismiss()

        val dialog = WarningFragment(SoundCheckHelper.warningLabel)

        // 알림창이 띄워져있는 동안 배경 클릭 막기
        dialog.isCancelable = false
        dialog.show(this.supportFragmentManager, "WarningDialog")

        // 대화상자를 보여준 후에는 "show_dialog" 값을 다시 false로 설정
        intent.putExtra("show_dialog", false)

        // 현재 띄워진 다이얼로그를 기억하여 나중에 닫기 위해 변수에 저장
        currentDialog = dialog
    }

    /** 권한 대화상자 **/
    override fun showAuthorizationDialog(context: Context, feature: String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.authorization_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams?.gravity = Gravity.BOTTOM // 화면의 밑에 위치하도록 설정

        // 100px 만큼 margin 추가
        val marginInPx = 200
        layoutParams?.y = marginInPx

        // TextView 설정
        val textView = dialog.findViewById<TextView>(R.id.confirm_textView)
        textView?.text = feature+"을 위해 권한을 허용해 주세요."
        if(feature == "소리 인식")
            textView?.text = feature+"을 위해 마이크 권한을 허용해 주세요."

        dialog.findViewById<Button>(R.id.yes_button)?.setOnClickListener {
            dialog.dismiss()
            // 핸드폰 설정 화면으로 이동
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
            startForResult.launch(intent)
        }

        dialog.findViewById<Button>(R.id.no_button)?.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

}
