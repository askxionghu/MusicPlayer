package sakuraba.saki.player.music

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.graphics.Color.WHITE
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.util.Log
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lib.github1552980358.ktExtension.android.content.getStatusBarHeight
import lib.github1552980358.ktExtension.android.graphics.toBitmap
import lib.github1552980358.ktExtension.android.os.bundle
import lib.github1552980358.ktExtension.jvm.keyword.tryRun
import mkaflowski.mediastylepalette.MediaNotificationProcessor
import sakuraba.saki.player.music.base.BaseMediaControlActivity
import sakuraba.saki.player.music.databinding.ActivityPlayBinding
import sakuraba.saki.player.music.service.util.AudioInfo
import sakuraba.saki.player.music.util.BitmapUtil.loadAlbumArt
import sakuraba.saki.player.music.util.Constants
import sakuraba.saki.player.music.util.Constants.ACTION_UPDATE_PLAY_MODE
import sakuraba.saki.player.music.util.Constants.EXTRAS_AUDIO_INFO
import sakuraba.saki.player.music.util.Constants.EXTRAS_PLAY_MODE
import sakuraba.saki.player.music.util.Constants.EXTRAS_STATUS
import sakuraba.saki.player.music.util.Constants.PLAY_MODE_LIST
import sakuraba.saki.player.music.util.Constants.PLAY_MODE_RANDOM
import sakuraba.saki.player.music.util.Constants.PLAY_MODE_SINGLE
import sakuraba.saki.player.music.util.Constants.PLAY_MODE_SINGLE_CYCLE
import sakuraba.saki.player.music.util.Coroutine.delay1second
import sakuraba.saki.player.music.util.Coroutine.ms_1000_int
import sakuraba.saki.player.music.util.LifeStateConstant.ON_CREATE
import sakuraba.saki.player.music.util.LifeStateConstant.ON_DESTROY
import sakuraba.saki.player.music.util.LifeStateConstant.ON_PAUSE
import sakuraba.saki.player.music.util.LifeStateConstant.ON_RESUME
import sakuraba.saki.player.music.util.SystemUtil.pixelHeight

class PlayActivity: BaseMediaControlActivity() {
    
    companion object {
        private const val TAG = "PlayActivity"
    }
    
    private var _activityPlayBinding: ActivityPlayBinding? = null
    private val activityPlay get() = _activityPlayBinding!!
    private lateinit var viewModel: PlayViewModel
    
    private var _textViewTitle: TextView? = null
    private val textViewTitle get() = _textViewTitle!!
    
    private var _textViewSummary: TextView? = null
    private val textViewSummary get() = _textViewSummary!!
    
    private lateinit var behavior: BottomSheetBehavior<LinearLayout>
    
    private var isPlaying = false
    private var job: Job? = null
    
    private var activityBackgroundColor = TRANSPARENT
    
    private lateinit var playModeListCycle: Drawable
    private val playModeSingleCycle by lazy { resources.getDrawable(R.drawable.ic_single_cycle, null) }
    private val playModeRandom by lazy { resources.getDrawable(R.drawable.ic_random, null) }
    private val playModeSingle by lazy { resources.getDrawable(R.drawable.ic_single, null) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e(TAG, ON_CREATE)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = TRANSPARENT
        
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(PlayViewModel::class.java)
        _activityPlayBinding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(activityPlay.root)
        
        setSupportActionBar(activityPlay.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activityPlay.toolbar.navigationIcon?.setTint(WHITE)
        activityPlay.toolbar.setNavigationOnClickListener { onBackPressed() }
        
        activityPlay.imageView.layoutParams = activityPlay.imageView.layoutParams.apply { height = resources.displayMetrics.widthPixels }
        
        _textViewTitle = findViewById(R.id.text_view_title)
        _textViewSummary = findViewById(R.id.text_view_summary)
    
        playModeListCycle = activityPlay.imageButtonPlayMode.drawable
        viewModel.updatePlayMode(PLAY_MODE_LIST)
        
        CoroutineScope(Dispatchers.IO).launch {
            val audioInfo = intent?.getSerializableExtra(EXTRAS_AUDIO_INFO) as AudioInfo? ?: return@launch
            var bitmap = tryRun { loadAlbumArt(audioInfo.audioAlbumId) }
            if (bitmap == null) {
                bitmap = resources.getDrawable(R.drawable.ic_music, null).toBitmap()
            }
            launch(Dispatchers.Main) { activityPlay.imageView.setImageBitmap(bitmap) }
            launch(Dispatchers.Main) {
                textViewTitle.text = audioInfo.audioTitle
                @Suppress("SetTextI18n")
                textViewSummary.text = "${audioInfo.audioArtist} - ${audioInfo.audioAlbum}"
            }
            MediaNotificationProcessor(this@PlayActivity, bitmap).getColorUpdated(true)
        }
    
        findViewById<LinearLayout>(R.id.linear_layout).apply {
            layoutParams = layoutParams.apply { height = pixelHeight - getStatusBarHeight() }
        }
        
        behavior = BottomSheetBehavior.from(findViewById(R.id.linear_layout))
        behavior.peekHeight =
            pixelHeight - resources.displayMetrics.widthPixels -
                resources.getDimensionPixelSize(R.dimen.play_controller_height) -
                resources.getDimensionPixelSize(R.dimen.play_controller_seekbar_height)
        behavior.isHideable = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        
        viewModel.progress.observe(this) { newProgress ->
            if (!activityPlay.playSeekBar.isUserTouched) {
                activityPlay.playSeekBar.progress = newProgress
            }
        }
        viewModel.duration.observe(this) { newDuration ->
            activityPlay.playSeekBar.max = newDuration
            activityPlay.durationViewDuration.duration = newDuration
        }
        viewModel.state.observe(this) { newState ->
            activityPlay.floatingActionButton.apply {
                when (newState) {
                    STATE_PLAYING -> {
                        setImageResource(R.drawable.ani_play_to_pause)
                        (drawable as AnimatedVectorDrawable).start()
                    }
                    STATE_PAUSED -> {
                        setImageResource(R.drawable.ani_pause_to_play)
                        (drawable as AnimatedVectorDrawable).start()
                    }
                }
            }
        }
        
        activityPlay.playSeekBar.setOnSeekChangeListener { progress, isUser, isReleased ->
            activityPlay.durationViewProgress.duration = progress
            if (isUser && isReleased) {
                mediaControllerCompat.transportControls.seekTo(progress)
            }
        }
        
        activityPlay.floatingActionButton.setOnClickListener {
            when (viewModel.stateValue) {
                STATE_PLAYING -> mediaControllerCompat.transportControls.pause()
                STATE_PAUSED -> mediaControllerCompat.transportControls.play()
            }
        }
        
        activityPlay.imageButtonNext.setOnClickListener { mediaControllerCompat.transportControls.skipToNext() }
        activityPlay.imageButtonPrev.setOnClickListener { mediaControllerCompat.transportControls.skipToPrevious() }
        
        activityPlay.imageView.setOnClickListener {
            when (activityPlay.relativeLayoutToolbarRoot.visibility) {
                GONE -> {
                    activityPlay.relativeLayoutToolbarRoot.apply {
                        animate()
                            .alpha(1F)
                            .setDuration(500)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationStart(p0: Animator?) {
                                    alpha = 0F
                                    visibility = VISIBLE
                                }
                            })
                            .start()
                    }
                }
                VISIBLE -> {
                    activityPlay.relativeLayoutToolbarRoot.animate()
                        .alpha(0F)
                        .setDuration(500)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                activityPlay.relativeLayoutToolbarRoot.visibility = GONE
                            }
                        })
                        .start()
                }
                INVISIBLE -> activityPlay.relativeLayoutToolbarRoot.visibility = GONE
            }
        }
        
        activityPlay.imageButtonPlayMode.setOnClickListener {
            mediaBrowserCompat.sendCustomAction(
                ACTION_UPDATE_PLAY_MODE,
                bundle {
                    putInt(EXTRAS_PLAY_MODE,
                        when (viewModel.playModeValue) {
                            PLAY_MODE_LIST -> PLAY_MODE_SINGLE_CYCLE
                            PLAY_MODE_SINGLE_CYCLE -> PLAY_MODE_RANDOM
                            PLAY_MODE_RANDOM -> PLAY_MODE_SINGLE
                            PLAY_MODE_SINGLE -> PLAY_MODE_LIST
                            else -> PLAY_MODE_LIST
                        }
                    ) },
                null
            )
        }
        
        viewModel.playMode.observe(this) { newPlayMode ->
            activityPlay.imageButtonPlayMode.setImageDrawable(
                when (newPlayMode) {
                    PLAY_MODE_LIST -> playModeListCycle
                    PLAY_MODE_SINGLE_CYCLE -> playModeSingleCycle
                    PLAY_MODE_RANDOM -> playModeRandom
                    PLAY_MODE_SINGLE -> playModeSingle
                    else -> playModeListCycle
                }
            )
            activityPlay.imageButtonPlayMode.drawable.setTint(if (viewModel.isLightBackground.value == true) BLACK else WHITE)
        }
        
    }
    
    override fun onMediaBrowserConnected() {
        if (mediaBrowserCompat.isConnected) {
            registerMediaController()
    
            mediaBrowserCompat.sendCustomAction(Constants.ACTION_REQUEST_STATUS, null, object : MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(action: String?, extras: Bundle?, resultData: Bundle?) {
                    resultData?:return
                    isPlaying = false
                    job?.cancel()
                    val audioInfo = (resultData.getSerializable(EXTRAS_AUDIO_INFO) as AudioInfo?) ?: return
                    viewModel.updateDuration(audioInfo.audioDuration)
                    viewModel.updateProgress(resultData.getInt(Constants.EXTRAS_PROGRESS).toLong())
                    viewModel.updateState(resultData.getInt(EXTRAS_STATUS))
                    when (viewModel.stateValue) {
                        STATE_PLAYING -> {
                            activityPlay.floatingActionButton.setImageResource(R.drawable.ic_pause)
                            isPlaying = true
                            job = getProgressSyncJob(activityPlay.playSeekBar.progress)
                        }
                        STATE_PAUSED -> activityPlay.floatingActionButton.setImageResource(R.drawable.ic_play)
                    }
                }
            })
        }
    }
    
    @Suppress("DuplicatedCode")
    private fun getProgressSyncJob(progress: Long) = CoroutineScope(Dispatchers.IO).launch {
        val currentProgress = delayForCorrection(progress)
        launch(Dispatchers.Main) { viewModel.updateProgress(currentProgress) }
        while (isPlaying) {
            delay1second()
            launch(Dispatchers.Main) { viewModel.updateProgress(viewModel.progressValue + ms_1000_int) }
        }
    }
    
    private suspend fun delayForCorrection(progress: Long): Long {
        val diff = progress % 1000
        if (diff != 0L) {
            delay(diff)
        }
        return progress + diff
    }
    
    override fun onMediaControllerPlaybackStateChanged(state: PlaybackStateCompat?) {
        Log.e(TAG, "MediaControllerCompat.Callback.onPlaybackStateChanged ${state?.state}")
        state ?: return
        viewModel.updateState(state.state)
        when (state.state) {
            STATE_PLAYING -> {
                job?.cancel()
                if (!isPlaying) {
                    isPlaying = true
                }
                job = getProgressSyncJob(state.position)
            }
            STATE_PAUSED -> {
                isPlaying = false
                job?.cancel()
            }
            STATE_BUFFERING -> {
                isPlaying = false
                job?.cancel()
            }
            else -> Unit
        }
        viewModel.updatePlayMode(state.extras?.getInt(EXTRAS_PLAY_MODE, PLAY_MODE_LIST) ?: PLAY_MODE_LIST)
    }
    
    override fun onMediaControllerMetadataChanged(metadata: MediaMetadataCompat?) {
        metadata ?: return
        CoroutineScope(Dispatchers.IO).launch {
            var bitmap = tryRun { loadAlbumArt(metadata.getString(METADATA_KEY_ALBUM_ART_URI)) }
            if (bitmap == null) {
                bitmap = resources.getDrawable(R.drawable.ic_music, null).toBitmap()
            }
            launch(Dispatchers.Main) { activityPlay.imageView.setImageBitmap(bitmap) }
            MediaNotificationProcessor(this@PlayActivity, bitmap).getColorUpdated(false)
        }
        viewModel.updateDuration(metadata.getLong(METADATA_KEY_DURATION))
        textViewTitle.text = metadata.getString(METADATA_KEY_TITLE)
        textViewSummary.text = metadata.getString(METADATA_KEY_ARTIST)
    }
    
    private fun MediaNotificationProcessor.getColorUpdated(isInit: Boolean) = CoroutineScope(Dispatchers.Main).launch {
        if (activityBackgroundColor != backgroundColor) {
            ValueAnimator.ofArgb(activityBackgroundColor, backgroundColor).apply {
                duration = 500
                addUpdateListener {
                    activityPlay.root.setBackgroundColor(animatedValue as Int)
                }
                start()
            }
            activityBackgroundColor = backgroundColor
        }
        viewModel.setIsLightBackground(isLight)
        if (isInit) {
            if (isLight) {
                activityPlay.imageButtonNext.drawable.setTint(BLACK)
                activityPlay.imageButtonPrev.drawable.setTint(BLACK)
                activityPlay.imageButtonPlayMode.drawable.setTint(BLACK)
            } else {
                activityPlay.imageButtonNext.drawable.setTint(WHITE)
                activityPlay.imageButtonPrev.drawable.setTint(WHITE)
                activityPlay.imageButtonPlayMode.drawable.setTint(WHITE)
            }
            viewModel.isLightBackground.observe(this@PlayActivity) { isLight ->
                if (isLight) {
                    ValueAnimator.ofArgb(WHITE, BLACK).apply {
                        duration = 500
                        addUpdateListener {
                            activityPlay.imageButtonNext.drawable.setTint(animatedValue as Int)
                            activityPlay.imageButtonPrev.drawable.setTint(animatedValue as Int)
                            activityPlay.imageButtonPlayMode.drawable.setTint(animatedValue as Int)
                        }
                        start()
                    }
                } else {
                    ValueAnimator.ofArgb(BLACK, WHITE).apply {
                        duration = 500
                        addUpdateListener {
                            activityPlay.imageButtonNext.drawable.setTint(animatedValue as Int)
                            activityPlay.imageButtonPrev.drawable.setTint(animatedValue as Int)
                            activityPlay.imageButtonPlayMode.drawable.setTint(animatedValue as Int)
                        }
                        start()
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        isPlaying = false
        job?.cancel()
        Log.e(TAG, ON_PAUSE)
        super.onPause()
    }
    
    override fun onResume() {
        Log.e(TAG, ON_RESUME)
        super.onResume()
    }
    
    override fun onDestroy() {
        Log.e(TAG, ON_DESTROY)
        super.onDestroy()
        _activityPlayBinding = null
    }
    
}