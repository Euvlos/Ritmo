package com.ritmo.app.notifications

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import java.io.File

class AlarmActivity : Activity() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        val density = resources.displayMetrics.density
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER; setPadding((32 * density).toInt())
            addView(TextView(this@AlarmActivity).apply { text = intent.getStringExtra(ReminderReceiver.GOAL_TITLE) ?: "Ritmo"; textSize = 30f; gravity = android.view.Gravity.CENTER })
            addView(Button(this@AlarmActivity).apply { text = "Остановить"; setOnClickListener { finish() } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (32 * density).toInt() })
        })
        val selected = intent.getStringExtra(ReminderReceiver.SOUND_URI)?.let(Uri::parse)
        val uri = selected ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            if (uri.scheme == "file") setDataSource(File(requireNotNull(uri.path)).absolutePath) else setDataSource(this@AlarmActivity, uri)
            isLooping = true; prepare(); start()
        }
        vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 400), 0))
    }

    override fun onDestroy() { player?.stop(); player?.release(); vibrator?.cancel(); super.onDestroy() }
}
