package sakuraba.saki.player.music.ui.audioDetail.lyricView.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import lib.github1552980358.ktExtension.android.view.createViewHolder
import sakuraba.saki.player.music.R
import sakuraba.saki.player.music.util.Lyric
import sakuraba.saki.player.music.util.LyricUtil.timeStr
import sakuraba.saki.player.music.util.ViewHolderUtil.bindHolder

class RecyclerViewAdapterUtil(recyclerView: RecyclerView, private val lyric: Lyric, private val listener: (index: Int) -> Unit) {

    private class RecyclerViewHolder(view: View): ViewHolder(view) {
        val linearLayout: LinearLayout = view.findViewById(R.id.linear_layout)
        val textViewTitle: TextView = view.findViewById(R.id.text_view_title)
        val textViewSummary: TextView = view.findViewById(R.id.text_view_summary)
    }

    private inner class RecyclerViewAdapter: RecyclerView.Adapter<RecyclerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            parent.createViewHolder<RecyclerViewHolder>(R.layout.layout_lyric_view_recycler_view)

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) = holder.bindHolder {
            textViewTitle.text = lyric.timeAt(position).timeStr
            textViewSummary.text = lyric.lyricAt(position)
            linearLayout.setOnClickListener { listener(position) }
        }

        override fun getItemCount() = lyric.size
    }

    private val adapter = RecyclerViewAdapter()

    init {
        recyclerView.adapter = adapter
    }

    @Suppress("NotifyDatasetChanged")
    fun notifyDataSetChanged() = adapter.notifyDataSetChanged()

}