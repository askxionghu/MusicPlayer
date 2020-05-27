package app.github1552980358.android.musicplayer.adapter

import android.app.Service
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.github1552980358.android.musicplayer.R
import app.github1552980358.android.musicplayer.activity.MainActivity
import app.github1552980358.android.musicplayer.base.AudioData
import app.github1552980358.android.musicplayer.base.Constant.Companion.AudioDataDir
import app.github1552980358.android.musicplayer.base.Constant.Companion.AudioDataListFile
import app.github1552980358.android.musicplayer.base.Constant.Companion.CurrentSongList
import app.github1552980358.android.musicplayer.base.Constant.Companion.FULL_LIST
import app.github1552980358.android.musicplayer.base.Constant.Companion.IgnoredFile
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.view_media_list.view.imageButtonOpts
import java.io.File
import java.io.ObjectOutputStream

/**
 * @file    : [ListFragmentRecyclerViewAdapter]
 * @author  : 1552980358
 * @since   : 0.1
 * @date    : 2020/5/10
 * @time    : 13:05
 **/

class ListFragmentRecyclerViewAdapter(
    private val activity: MainActivity,
    list: ArrayList<AudioData>,
    private val swipeRefreshLayout: SwipeRefreshLayout
) :
    Adapter<ListFragmentRecyclerViewAdapter.ViewHolder>() {
    
    private var audioDataList = list
    
    /**
     * [onCreateViewHolder]
     * @param parent [ViewGroup]
     * @param viewType [Int]
     * @author 1552980358
     * @since 0.1
     **/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            (parent.context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.view_media_list, parent, false)
        )
    }
    
    /**
     * [getItemCount]
     * @return [Int]
     * @author 1552980358
     * @since 0.1
     **/
    override fun getItemCount(): Int {
        return audioDataList.size + 1
    }
    
    /**
     * [onBindViewHolder]
     * @param holder [ViewHolder]
     * @param position [Int]
     * @author 1552980358
     * @since 0.1
     **/
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        
        // 不显示
        if (audioDataList.isEmpty()){
            holder.relativeLayoutRoot.visibility = View.GONE
            return
        }
        if (position == audioDataList.size) {
            holder.relativeLayoutRoot.isClickable = false
            holder.imageButtonOpts.visibility = View.GONE
            return
        }
        
        // If it is not set, some items might be affected by visibility
        // set above causing they invisible
        // 如果此项不设置, 有些子按键会被以上设置影响而导致无法被看到
        holder.relativeLayoutRoot.visibility = View.VISIBLE
        
        // No need, automatically set when setting onClickListener
        // 不需要手动设置, 设置监听时会自动启动
        //holder.imageButtonOpts.isClickable = true

        holder.textViewNo.text = position.plus(1).toString()
        holder.textViewTitle.apply {
            text = audioDataList[position].title
        }
        holder.textViewSubtitle.apply {
            text = audioDataList[position].artist
        }
        
        holder.relativeLayoutRoot.setOnClickListener {
            // Remove action
            // 清除动作
            // holder.textViewTitle.ellipsize = TextUtils.TruncateAt.END
            // holder.textViewTitle.clearFocus()
            
            // Check if collapsing is required
            // 检测是否需要折叠
            if (activity.bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                return@setOnClickListener
            }
            
            activity.mediaControllerCompat.transportControls.playFromMediaId(
                audioDataList[position].id,
                Bundle().apply { putString(CurrentSongList, FULL_LIST) }
            )
            
        }
        
        // Toast out full name
        // 弹出全名
        holder.relativeLayoutRoot.setOnLongClickListener {
            Toast.makeText(it.context, audioDataList[position].title, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
        
        // Options
        // 选项
        holder.imageButtonOpts.setOnClickListener {
            PopupMenu(it.context, holder.imageButtonOpts.imageButtonOpts).apply {
                inflate(R.menu.menu_audio_opt)
                setOnMenuItemClickListener {
                    
                    File(activity.getExternalFilesDir(AudioDataDir), IgnoredFile).appendText(audioDataList[position].id + "\n")

                    audioDataList.removeAt(position)
                    File(activity.getExternalFilesDir(AudioDataDir), AudioDataListFile).apply {
                        //if (!exists()) {
                        //    createNewFile()
                        //}
                        //writeText("")
                        delete()
                        createNewFile()
        
                        // Write
                        // 写入
                        outputStream().use { os ->
                            ObjectOutputStream(os).use { oos ->
                                oos.writeObject(audioDataList)
                                oos.flush()
                            }
                            os.flush()
                        }
                    }

                    //mainActivity.updateList()
                    notifyDataSetChanged()
                    return@setOnMenuItemClickListener true
                }
            }.show()
        }
        
    }
    
    /**
     * [updateList]
     * @author 1552980358
     * @since 0.1
     **/
    fun updateList(list: ArrayList<AudioData>) {
        audioDataList = list
        notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
    }
    
    /**
     * [ViewHolder]
     * @param view [View]
     * @author 1552980358
     * @since 0.1
     **/
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        /**
         * [textViewNo]
         * @author 1552980358
         * @since 0.1
         **/
        var textViewNo: TextView = view.findViewById(R.id.textViewNo)
        /**
         * [textViewTitle]
         * @author 1552980358
         * @since 0.1
         **/
        var textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
        /**
         * [textViewSubtitle]
         * @author 1552980358
         * @since 0.1
         **/
        var textViewSubtitle: TextView = view.findViewById(R.id.textViewSubtitle)
        /**
         * [relativeLayoutRoot]
         * @author 1552980358
         * @since 0.1
         **/
        var relativeLayoutRoot: RelativeLayout = view.findViewById(R.id.relativeLayoutRoot)
        /**
         * [imageButtonOpts]
         * @author 1552980358
         * @since 0.1
         **/
        var imageButtonOpts: ImageButton = view.findViewById(R.id.imageButtonOpts)
    }
    
}