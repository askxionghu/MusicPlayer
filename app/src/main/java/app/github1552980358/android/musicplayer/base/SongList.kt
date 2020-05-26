package app.github1552980358.android.musicplayer.base

import java.io.Serializable

/**
 * [SongList]
 * @author  : 1552980328
 * @since   : 0.1
 * @date    : 2020/5/20
 * @time    : 11:40
 **/

class SongList: Serializable {

    /**
     * [listName]
     **/
    var listName = ""

    /**
     * [audioList]
     **/
    val audioList = arrayListOf<AudioData>()
    
}