package app.skynight.musicplayer.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.audiofx.Visualizer
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.RelativeLayout.LayoutParams
import android.widget.RelativeLayout.CENTER_HORIZONTAL
import android.widget.RelativeLayout.CENTER_VERTICAL
import android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.skynight.musicplayer.R
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.CLIENT_BROADCAST_LAST
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.CLIENT_BROADCAST_NEXT
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.CLIENT_BROADCAST_ONPAUSE
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.CLIENT_BROADCAST_ONSTART
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.SERVER_BROADCAST_MUSICCHANGE
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.SERVER_BROADCAST_ONPAUSE
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.SERVER_BROADCAST_ONSTART
import app.skynight.musicplayer.util.getTime
import app.skynight.musicplayer.view.MusicAlbumRoundedImageView
import kotlinx.android.synthetic.main.activity_player.relativeLayout
import kotlinx.android.synthetic.main.activity_player.textView_timePass
import kotlinx.android.synthetic.main.activity_player.seekBar
import kotlinx.android.synthetic.main.activity_player.layout_filter
import kotlinx.android.synthetic.main.activity_player.imageButton_list
import kotlinx.android.synthetic.main.activity_player.imageButton_next
import kotlinx.android.synthetic.main.activity_player.imageButton_last
import kotlinx.android.synthetic.main.activity_player.toolbar
import kotlinx.android.synthetic.main.activity_player.backgroundDrawerLayout
import kotlinx.android.synthetic.main.activity_player.textView_timeTotal
import kotlinx.android.synthetic.main.activity_player.imageButton_playForm
import kotlinx.android.synthetic.main.activity_player.checkBox_playControl
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.app.ActivityCompat
import app.skynight.musicplayer.pulse.BaseMusicVisiblePulse
import app.skynight.musicplayer.pulse.VerticalColumnPulse
import app.skynight.musicplayer.pulse.ElectronicCurrentPulse
import app.skynight.musicplayer.pulse.CompatWavePulse
import app.skynight.musicplayer.util.log
import app.skynight.musicplayer.util.Player
import app.skynight.musicplayer.util.Player.Companion.Pulse
import app.skynight.musicplayer.util.Player.Companion.PulseColor
import app.skynight.musicplayer.util.setColorFilter
import app.skynight.musicplayer.util.Player.Companion.PulseType
import app.skynight.musicplayer.util.Player.Companion.PulseType_ElectricCurrent
import app.skynight.musicplayer.util.Player.Companion.PulseType_VerticalColumn
import mkaflowski.mediastylepalette.MediaNotificationProcessor

/**
 * @FILE:   PlayerActivity
 * @AUTHOR: 1552950358
 * @DATE:   18 Jul 2019
 * @TIME:   7:23 PM
 **/

class PlayerActivity : AppCompatActivity() {

    private val broadcastReceiver by lazy { object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                SERVER_BROADCAST_ONSTART -> {
                    checkBox_playControl.isChecked = true
                    startThread()
                }
                SERVER_BROADCAST_ONPAUSE -> {
                    try {
                        thread!!.interrupt()
                        thread = null
                    } catch (e: Exception) {
                        //e.printStackTrace()
                    }
                    checkBox_playControl.isChecked = false
                }
                SERVER_BROADCAST_MUSICCHANGE -> {
                    try {
                        thread!!.interrupt()
                        thread = null
                    } catch (e: Exception) {
                        //e.printStackTrace()
                    }
                    onUpdateMusic()
                    startThread()
                }
            }
        }

    } }
    private lateinit var albumPic: MusicAlbumRoundedImageView
    private var thread: Thread? = null
    private var seekBarOnTouched = false
    private var tintColor = 0
    private lateinit var visualizer: Visualizer
    private lateinit var musicVisiblePulse: BaseMusicVisiblePulse

    private fun setBackgroundProp() {
        window.decorView.systemUiVisibility =
            if (Player.settings[Player.StatusBar]!! as Boolean) (SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            else (SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        log("PlayerActivity", "onCreate")
        overridePendingTransition(R.anim.anim_static, R.anim.anim_top2down)
        setBackgroundProp()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        if (Player.settings[Player.Filter]!! as Boolean) {
            layout_filter.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
        }

        toolbar.apply {
            setSupportActionBar(this)
            setTitleTextColor(ContextCompat.getColor(this@PlayerActivity, R.color.player_title))
            setSubtitleTextColor(
                ContextCompat.getColor(
                    this@PlayerActivity, R.color.player_subtitle
                )
            )
            setNavigationOnClickListener {
                finish()
            }
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        imageButton_playForm.setBackgroundResource(
            when (Player.getPlayer.getPlayingType()) {
                Player.Companion.PlayingType.CYCLE -> {
                    R.drawable.ic_player_cycle
                }
                Player.Companion.PlayingType.SINGLE -> {
                    R.drawable.ic_player_single
                }
                else -> {
                    R.drawable.ic_player_random
                }
            }
        )

        imageButton_playForm.setOnClickListener {
            when (Player.getPlayer.getPlayingType()) {
                Player.Companion.PlayingType.CYCLE -> {
                    Player.getPlayer.setPlayingType(Player.Companion.PlayingType.SINGLE)
                    imageButton_playForm.setBackgroundResource(R.drawable.ic_player_single)
                }
                Player.Companion.PlayingType.SINGLE -> {
                    Player.getPlayer.setPlayingType(Player.Companion.PlayingType.RANDOM)
                    imageButton_playForm.setBackgroundResource(R.drawable.ic_player_random)
                }
                Player.Companion.PlayingType.RANDOM -> {
                    Player.getPlayer.setPlayingType(Player.Companion.PlayingType.CYCLE)
                    imageButton_playForm.setBackgroundResource(R.drawable.ic_player_cycle)
                }
            }
            if (Player.settings[Player.Button]!! as Boolean) {
                imageButton_playForm.background.setTint(tintColor)
            }
        }

        imageButton_last.setOnClickListener { sendBroadcast(Intent(CLIENT_BROADCAST_LAST)) }
        checkBox_playControl.apply {
            setOnClickListener {
                sendBroadcast(Intent(if (isChecked) CLIENT_BROADCAST_ONSTART else CLIENT_BROADCAST_ONPAUSE))
            }
        }
        imageButton_next.setOnClickListener { sendBroadcast(Intent(CLIENT_BROADCAST_NEXT)) }
        imageButton_list.setOnClickListener {
            startActivity(
                Intent(
                    this, BottomListActivity::class.java
                )
            )
        }

        try {
            (toolbar.javaClass.getDeclaredField("mTitleTextView").apply { isAccessible = true }.get(
                toolbar
            ) as TextView).apply {
                setHorizontallyScrolling(true)
                marqueeRepeatLimit = -1
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSelected = true
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
                textView_timePass.text = getTime(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                seekBarOnTouched = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Player.getPlayer.onSeekChange(seekBar!!.progress)
                seekBarOnTouched = false
            }
        })

        relativeLayout.apply {
            addView(MusicAlbumRoundedImageView(this@PlayerActivity).apply {
                albumPic = this
                size = resources.displayMetrics.widthPixels * 2 / 3
            }, LayoutParams(
                resources.displayMetrics.widthPixels * 2 / 3,
                resources.displayMetrics.widthPixels * 2 / 3
            ).apply {
                addRule(CENTER_HORIZONTAL)
                addRule(CENTER_VERTICAL)
            })

            if (Player.settings[Pulse] as Boolean) {
                addView(when (Player.settings[PulseType]) {
                    PulseType_ElectricCurrent -> ElectronicCurrentPulse(
                        this@PlayerActivity,
                        resources.displayMetrics.widthPixels,
                        resources.getDimensionPixelSize(R.dimen.playerActivity_renderer_height)
                    )
                    PulseType_VerticalColumn -> VerticalColumnPulse(
                        this@PlayerActivity,
                        resources.displayMetrics.widthPixels,
                        resources.getDimensionPixelSize(R.dimen.playerActivity_renderer_height)
                    )
                    else -> {
                        CompatWavePulse(
                            this@PlayerActivity,
                            resources.displayMetrics.widthPixels,
                            resources.getDimensionPixelSize(R.dimen.playerActivity_renderer_height)
                        )
                    }
                }.apply {
                    musicVisiblePulse = this
                    checkPermission()
                }, LayoutParams(
                    MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.playerActivity_renderer_height)
                ).apply {
                    addRule(ALIGN_PARENT_BOTTOM)
                    setMargins(
                        0,
                        0,
                        0,
                        resources.getDimensionPixelSize(R.dimen.playerActivity_renderer_marginBottom)
                    )
                })
            }

            val width = resources.displayMetrics.widthPixels / 5
            val height = resources.displayMetrics.heightPixels / 5
            isLongClickable = true
            val gestureDetector = GestureDetector(
                this@PlayerActivity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float
                    ): Boolean {
                        try {
                            if (e1!!.x - e2!!.x < -width) {
                                sendBroadcast(Intent(CLIENT_BROADCAST_LAST))
                                return true
                            }
                            if (e1.x - e2.x > width) {
                                sendBroadcast(Intent(CLIENT_BROADCAST_NEXT))
                                return true
                            }

                            if (e1.y - e2.y < -height) {
                                onBackPressed()
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return false
                    }

                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        sendBroadcast(Intent(if (Player.getPlayer.isPlaying()) CLIENT_BROADCAST_ONPAUSE else CLIENT_BROADCAST_ONSTART))
                        return true
                    }

                })
            setOnTouchListener { _, motionEvent ->
                return@setOnTouchListener gestureDetector.onTouchEvent(motionEvent)
                /*
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = motionEvent.x
                        lastY = motionEvent.y
                        System.currentTimeMillis().apply {
                            if (this - lastTime < 300) {
                                sendBroadcast(Intent(if (Player.getPlayer.isPlaying()) CLIENT_BROADCAST_ONPAUSE else CLIENT_BROADCAST_ONSTART))
                            }
                            lastTime = this
                        }
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (lastX - motionEvent.x < -width) {
                            sendBroadcast(Intent(CLIENT_BROADCAST_LAST))
                            return@setOnTouchListener true
                        }
                        if (lastX - motionEvent.x > width) {
                            sendBroadcast(Intent(CLIENT_BROADCAST_NEXT))
                            return@setOnTouchListener true
                        }

                        if (lastY - motionEvent.y < -height) {
                            onBackPressed()
                            return@setOnTouchListener true
                        }

                        return@setOnTouchListener false
                    }
                    else -> return@setOnTouchListener false
                }
                */
            }
        }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.MODIFY_AUDIO_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setUpVisualizer()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
                ), 0
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                onBackPressed()
                return
            }
        }
        setUpVisualizer()
    }

    private fun setUpVisualizer() {
        visualizer = Visualizer(Player.getPlayer.getMediaPlayer().audioSessionId).apply {
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    p0: Visualizer?, p1: ByteArray?, p2: Int
                ) {
                    //log("onWaveFormDataCapture", p1!!.toList())
                    musicVisiblePulse.setData(p1!!)

                }

                override fun onFftDataCapture(
                    p0: Visualizer?, p1: ByteArray?, p2: Int
                ) {
                    //log("onFftDataCapture", p1!!.toList())
                    /*
                    val tmp = ByteArray(p1!!.size / 2 + 1)
                    tmp[0] = abs(p1[1].toInt()).toByte()
                    var j = 1
                    for (i in 2 .. 17 step 2) {
                        tmp[j] = hypot(p1[i].toDouble(), p1[i+1].toDouble()).toByte()
                        j++
                    }*/
                    musicVisiblePulse.setData(p1!!)
                }

            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            captureSize =
                Visualizer.getCaptureSizeRange()[if (Player.settings[Player.PulseDensity] as Boolean) 0 else 1]
            //enabled = true
        }
    }

    private fun startThread() {
        Player.getPlayer.getCurrent().apply {
            if (this != -1) {
                log("Player.getPlayer.getCurrent", this)
                textView_timePass.text = getTime(this)
                seekBar.progress = this
            } else {
                textView_timePass.text = getTime(0)
                seekBar.progress = 0
            }
        }
        thread = Thread {
            /*
            if (::musicVisiblePulseView.isInitialized) {
                musicVisiblePulseView.start = true
            }
            if (::visualizer.isInitialized) {
                visualizer.enabled = true
            }
            */

            /* 提升执行效率 */
            try {
                musicVisiblePulse.start = true
                visualizer.enabled = true
            } catch (e: Exception) {
                //e.printStackTrace()
            }

            while (Player.getPlayer.isPlaying()) {

                if (!seekBarOnTouched) {
                    runOnUiThread {
                        textView_timePass.text = getTime(Player.getPlayer.getCurrent())
                        seekBar.progress = Player.getPlayer.getCurrent()
                    }
                }

                try {
                    Thread.sleep(500)
                } catch (e: Exception) {
                    //e.printStackTrace()
                }
            }
            /*
            if (::musicVisiblePulseView.isInitialized) {
                musicVisiblePulseView.start = false
            }
            if (::visualizer.isInitialized) {
                visualizer.enabled = false
            }
            */
            try {
                musicVisiblePulse.start = false
                visualizer.enabled = false
            } catch (e: Exception) {
                //e.printStackTrace()
            }
        }.apply { start() }
    }

    private fun registerReceiver() {
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(SERVER_BROADCAST_ONSTART)
            addAction(SERVER_BROADCAST_ONPAUSE)
            addAction(SERVER_BROADCAST_MUSICCHANGE)
        })
    }

    private fun unregisterReceiver() {
        unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        log("PlayerActivity", "onResume")
        super.onResume()
        registerReceiver()
        onUpdateMusic()
        startThread()
    }

    fun onUpdateMusic() {
        val musicInfo = Player.getCurrentMusicInfo()
        val alPic = musicInfo.albumPic()
        Thread {
            try {
                val mediaNotificationProcessor = MediaNotificationProcessor(this, alPic)

                if (Player.settings[Player.BgColor]!! as Boolean) {
                    runOnUiThread {
                        backgroundDrawerLayout.setBackgroundColor(
                            mediaNotificationProcessor.backgroundColor
                        )
                    }

                } else {
                    val tmp =
                        Bitmap.createBitmap(alPic, 0, 0, alPic.width, alPic.height, Matrix().apply {
                            val scale =
                                resources.displayMetrics.heightPixels / alPic.height.toFloat()
                            postScale(scale, scale)
                        }, true)

                    val drawable = BitmapDrawable(
                        resources, Bitmap.createBitmap(
                            tmp,
                            if (tmp.width <= resources.displayMetrics.widthPixels) 0 else (tmp.width - resources.displayMetrics.widthPixels) / 2,
                            0,
                            resources.displayMetrics.widthPixels,
                            resources.displayMetrics.heightPixels,
                            null,
                            true
                        )
                    )
                    runOnUiThread { backgroundDrawerLayout.background = drawable }
                }

                tintColor = mediaNotificationProcessor.primaryTextColor
                if (Player.settings[Player.Button]!! as Boolean) {
                    runOnUiThread {
                        toolbar.apply {
                            setTitleTextColor(mediaNotificationProcessor.primaryTextColor)
                            setSubtitleTextColor(mediaNotificationProcessor.secondaryTextColor)
                            navigationIcon!!.setTint(mediaNotificationProcessor.secondaryTextColor)
                        }
                        textView_timeTotal.setTextColor(mediaNotificationProcessor.primaryTextColor)
                        textView_timePass.setTextColor(mediaNotificationProcessor.primaryTextColor)
                        try {
                            imageButton_last.background.setTint(mediaNotificationProcessor.primaryTextColor)
                            imageButton_playForm.background.setTint(mediaNotificationProcessor.primaryTextColor)
                            imageButton_next.background.setTint(mediaNotificationProcessor.primaryTextColor)
                            imageButton_list.background.setTint(mediaNotificationProcessor.primaryTextColor)
                            checkBox_playControl.background.setTint(mediaNotificationProcessor.primaryTextColor)
                            seekBar.apply {
                                thumb.apply {
                                    runOnUiThread {
                                        setColorFilter(
                                            mediaNotificationProcessor.primaryTextColor
                                        )
                                    }
                                }
                                progressDrawable.apply {
                                    runOnUiThread {
                                        setTint(
                                            mediaNotificationProcessor.primaryTextColor
                                        )
                                    }
                                }
                                indeterminateDrawable.apply {
                                    runOnUiThread {
                                        setTint(
                                            mediaNotificationProcessor.secondaryTextColor
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                if (Player.settings[PulseColor] as Boolean) {
                    musicVisiblePulse.setPaintColor(mediaNotificationProcessor.primaryTextColor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        try {
            toolbar.title = musicInfo.title()
            toolbar.subtitle = musicInfo.artist()
            albumPic.setImageBitmap(alPic)
            textView_timeTotal.text = getTime(musicInfo.duration())
            seekBar.max = musicInfo.duration()
            checkBox_playControl.isChecked = Player.getPlayer.isPlaying()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        log("PlayerActivity", "onPause")
        super.onPause()
        try {
            unregisterReceiver()
            visualizer.enabled = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        log("PlayerActivity", "onBackPressed")
        finish()
    }

    override fun finish() {
        log("PlayerActivity", "finish")
        super.finish()
        overridePendingTransition(R.anim.anim_static, R.anim.anim_top2down)
    }

    override fun onDestroy() {
        log("PlayerActivity", "onDestroy")
        try {
            unregisterReceiver()
            visualizer.enabled = false
            visualizer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
