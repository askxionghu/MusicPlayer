package projekt.cloud.piece.music.player.ui.play

import android.animation.ValueAnimator.ofArgb
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kotlinx.coroutines.delay
import lib.github1552980358.ktExtension.android.content.getStatusBarHeight
import lib.github1552980358.ktExtension.android.view.heightF
import lib.github1552980358.ktExtension.android.view.widthF
import lib.github1552980358.ktExtension.kotlinx.coroutines.io
import lib.github1552980358.ktExtension.kotlinx.coroutines.ui
import projekt.cloud.piece.music.player.R
import projekt.cloud.piece.music.player.base.BasePlayFragment
import projekt.cloud.piece.music.player.database.item.AudioItem
import projekt.cloud.piece.music.player.databinding.FragmentPlayBinding
import projekt.cloud.piece.music.player.service.play.Config.PLAY_CONFIG_REPEAT
import projekt.cloud.piece.music.player.service.play.Config.PLAY_CONFIG_REPEAT_ONE
import projekt.cloud.piece.music.player.service.play.Config.PLAY_CONFIG_SHUFFLE
import projekt.cloud.piece.music.player.service.play.Config.getConfig
import projekt.cloud.piece.music.player.service.play.Config.setConfig
import projekt.cloud.piece.music.player.util.ActivityUtil.pixelHeight
import projekt.cloud.piece.music.player.util.Constant.ANIMATION_DURATION_HALF_LONG
import projekt.cloud.piece.music.player.util.Constant.ANIMATION_DURATION_LONG
import projekt.cloud.piece.music.player.util.ContextUtil.navigationBarHeight

class PlayFragment: BasePlayFragment() {
    
    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    
    private val contentPlayFragmentBottomSheet get() =
        binding.contentPlayFragmentBottomSheet
    
    private val contentPlayFragmentButtons get() =
        binding.contentPlayFragmentButtons
    
    private val imageViewCycle get() = contentPlayFragmentButtons.imageViewCycle
    private val imageViewPrev get() = contentPlayFragmentButtons.imageViewPrev
    private val imageViewNext get() = contentPlayFragmentButtons.imageViewNext
    private val imageViewShuffle get() = contentPlayFragmentButtons.imageViewShuffle
    
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CardView>
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_play, container, false)
        
        binding.imageView.apply {
            layoutParams = layoutParams.apply {
                height = resources.displayMetrics.widthPixels
            }
        }
    
        loadMetadata(activityInterface.requestMetadata())
        
        activityInterface.setListener(
            loadMetadata = { audioItem -> loadMetadata(audioItem) },
            updatePlayConfig = { playConfig ->
                if (contentPlayFragmentButtons.playConfig != playConfig) {
                    contentPlayFragmentButtons.playConfig = playConfig
                }
            },
            loadBitmap = { bitmap -> binding.coverImage = bitmap },
            loadColor = { primaryColor, secondaryColor ->  },
            loadIsLight = { isLight ->
                when(contentPlayFragmentButtons.iconTintColor) {
                    null -> contentPlayFragmentButtons.iconTintColor = when {
                        isLight -> BLACK
                        else -> WHITE
                    }
                    else -> {
                        when {
                            isLight -> {
                                if (contentPlayFragmentButtons.iconTintColor != BLACK) {
                                    ofArgb(contentPlayFragmentButtons.iconTintColor!!, BLACK).apply {
                                        duration = ANIMATION_DURATION_LONG
                                        addUpdateListener { contentPlayFragmentButtons.iconTintColor = animatedValue as Int }
                                    }.start()
                                }
                            }
                            else -> {
                                if (contentPlayFragmentButtons.iconTintColor != WHITE) {
                                    ofArgb(contentPlayFragmentButtons.iconTintColor!!, WHITE).apply {
                                        duration = ANIMATION_DURATION_LONG
                                        addUpdateListener { contentPlayFragmentButtons.iconTintColor = animatedValue as Int }
                                    }.start()
                                }
                            }
                        }
                    }
                }
            },
            updateTime = {  },
            updatePlayState = { isPlaying -> contentPlayFragmentButtons.isPlaying = isPlaying }
        )
        
        return binding.root
    }
    
    @Suppress("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        contentPlayFragmentBottomSheet.cardView.apply {
            layoutParams = layoutParams.apply {
                height = pixelHeight - requireContext().getStatusBarHeight()
            }
            setContentPadding(0, 0, 0, navigationBarHeight)
        }
    
        val bottomHeight = pixelHeight - resources.displayMetrics.widthPixels
        
        contentPlayFragmentButtons.linearLayout.apply {
            layoutParams = layoutParams.apply { height = bottomHeight * 2 / 5 }
        }
        
        contentPlayFragmentButtons.floatingActionButton.setOnClickListener {
            when (contentPlayFragmentButtons.isPlaying) {
                true -> activityInterface.transportControls.pause()
                else -> activityInterface.transportControls.play()
            }
        }
        
        bottomSheetBehavior = BottomSheetBehavior.from(contentPlayFragmentBottomSheet.cardView)
        with(bottomSheetBehavior) {
            peekHeight = bottomHeight * 3 / 5
            isHideable = true
            state = STATE_HIDDEN
        }
        
        contentPlayFragmentButtons.linearLayout.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                ACTION_DOWN -> {
                    binding.relativeLayout.apply {
                        (background as RippleDrawable).setHotspot(motionEvent.x, motionEvent.y)
                        isPressed = true
                    }
                }
                ACTION_UP -> {
                    binding.relativeLayout.isPressed = false
                    val touchX = motionEvent.rawX
                    val touchY = motionEvent.rawY
                    val rawAxis = IntArray(2)
                    when {
                        compareAxis(touchX, touchY, imageViewCycle, rawAxis) -> {
                            contentPlayFragmentButtons.playConfig?.let { playConfig ->
                                val repeat = playConfig.getConfig(PLAY_CONFIG_REPEAT)
                                val repeatOne = playConfig.getConfig(PLAY_CONFIG_REPEAT_ONE)
                                when {
                                    // Repeat with list
                                    repeat && !repeatOne -> activityInterface.changePlayConfig(
                                        playConfig.setConfig(PLAY_CONFIG_REPEAT, false)
                                            .setConfig(PLAY_CONFIG_REPEAT_ONE, true)
                                    )
                                    // Repeat with single audio
                                    !repeat && repeatOne ->
                                        activityInterface.changePlayConfig(playConfig.setConfig(PLAY_CONFIG_REPEAT_ONE, false))
                                    // Play list, no repeat
                                    !repeat && !repeatOne ->
                                        activityInterface.changePlayConfig(playConfig.setConfig(PLAY_CONFIG_REPEAT, true))
                                }
                            }
                        }
                        
                        compareAxis(touchX, touchY, imageViewPrev, rawAxis) ->
                            activityInterface.transportControls.skipToPrevious()
                        
                        compareAxis(touchX, touchY, imageViewNext, rawAxis) ->
                            activityInterface.transportControls.skipToNext()
                        
                        compareAxis(touchX, touchY, imageViewShuffle, rawAxis) -> {
                            contentPlayFragmentButtons.playConfig?.let { playConfig ->
                                activityInterface.changePlayConfig(playConfig.setConfig(PLAY_CONFIG_SHUFFLE, !playConfig.getConfig(PLAY_CONFIG_SHUFFLE)))
                            }
                        }
                    }
                }
                ACTION_CANCEL -> { binding.relativeLayout.isPressed = false }
            }
            return@setOnTouchListener true
        }
    }
    
    private fun compareAxis(x: Float, y: Float, view: View, rawAxis: IntArray): Boolean {
        view.getLocationOnScreen(rawAxis)
        return compareAxis(x, rawAxis.first().toFloat(), view.widthF) && compareAxis(y, rawAxis.last().toFloat(), view.heightF)
    }
    
    private fun compareAxis(axis: Float, targetX: Float, length: Float) =
        axis in (targetX .. targetX + length)
    
    override fun onStart() {
        super.onStart()
        io {
            delay(ANIMATION_DURATION_HALF_LONG)
            ui {
                bottomSheetBehavior.state = STATE_COLLAPSED
                bottomSheetBehavior.isHideable = false
            }
        }
    }
    
    private fun loadMetadata(audioItem: AudioItem) {
        contentPlayFragmentBottomSheet.title = audioItem.title
        contentPlayFragmentBottomSheet.subtitle = "${audioItem.artistItem.name} - ${audioItem.albumItem.title}"
    }

}