package app.skynight.musicplayer.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import app.skynight.musicplayer.MainApplication
import app.skynight.musicplayer.base.InitConstructorNotAllowedException
import app.skynight.musicplayer.util.MusicInfo
import app.skynight.musicplayer.R
import app.skynight.musicplayer.activity.PlayerActivity
import app.skynight.musicplayer.activity.SimplePlayerActivity
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.BROADCAST_INTENT_MUSIC
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.BROADCAST_INTENT_PLAYLIST
import app.skynight.musicplayer.broadcast.BroadcastBase.Companion.CLIENT_BROADCAST_CHANGE
import app.skynight.musicplayer.util.Player

/**
 * @File    : MusicView
 * @Author  : 1552980358
 * @Date    : 4 Aug 2019
 * @TIME    : 9:35 AM
 **/

class MusicView : LinearLayout {

    constructor(
        context: Context,
        playList: Int,
        index: Int,
        musicInfo: MusicInfo
    ) : super(context) {
        background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
        isClickable = true
        isFocusable = true
        orientation = HORIZONTAL
        addView(TextView(context).apply {
            gravity = Gravity.CENTER
            text = index.plus(1).toString()
        }, LayoutParams(0, resources.getDimensionPixelSize(R.dimen.musicView_height)).apply {
            weight = 1f
        })
        addView(LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            // Title
            addView(AppCompatTextView(context).apply {
                setSingleLine()
                setTextColor(Color.BLACK)
                text = musicInfo.title()
                textSize = resources.getDimension(R.dimen.musicView_title_size)
                setHorizontallyScrolling(true)
                marqueeRepeatLimit = -1
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSelected = true
            }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            // Subtitle
            addView(AppCompatTextView(context).apply {
                setSingleLine()
                text = musicInfo.artist()
                //setTextColor(Player.ThemeTextColor)
                textSize = resources.getDimension(R.dimen.musicView_subTitle_size)
                marqueeRepeatLimit = -1
                ellipsize = TextUtils.TruncateAt.MARQUEE
            }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }, LayoutParams(0, resources.getDimensionPixelSize(R.dimen.musicView_height)).apply {
            weight = 9f
        })
        setOnClickListener {
            MainApplication.getMainApplication().apply {
                sendBroadcast(
                    Intent(CLIENT_BROADCAST_CHANGE).putExtra(
                        BROADCAST_INTENT_PLAYLIST,
                        playList
                    ).putExtra(BROADCAST_INTENT_MUSIC, index)
                )
                startActivity(Intent(
                    this,
                    if (!(Player.settings[Player.SimpleMode] as Boolean)) PlayerActivity::class.java else SimplePlayerActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    /* Not allowed */
    constructor(context: Context) : this(context, null)

    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context, attributeSet: AttributeSet?) : super(context) {
        throw InitConstructorNotAllowedException()
    }
}