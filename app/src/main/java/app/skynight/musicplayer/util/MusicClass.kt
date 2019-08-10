package app.skynight.musicplayer.util

import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.provider.MediaStore
import android.util.Log
import app.skynight.musicplayer.MainApplication
import app.skynight.musicplayer.util.FileUtil.Companion.checkAndCreateExternalStorageFile
import java.io.*
import kotlin.Exception

/**
 * @File    : MusicClass
 * @Author  : 1552980358
 * @Date    : 9 Aug 2019
 * @TIME    : 4:27 PM
 **/
class MusicClass private constructor() {
    var fullList: MutableList<MusicInfo>
    init {
        fullList = scanMusicByDataBase()
    }

    companion object {
        private const val MusicSaveDir = "app.skynight.musicplayer"
        private const val FullList = "MusicSaveFullList"
        val TargetDir = Environment.getExternalStorageDirectory().absolutePath + File.separator + MusicSaveDir

        val getMusicClass by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { MusicClass() }

        private fun saveMusicList(list : MutableList<MusicInfo>, path: String): Boolean {
            if (!checkAndCreateExternalStorageFile(path)) {
                return false
            }
            return try {
                val fileOutputStream = FileOutputStream(path, false)
                ObjectOutputStream(fileOutputStream).apply {
                    writeObject(list)
                    flush()
                    fileOutputStream.close()
                    close()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        private fun loadFullMusicList(): MutableList<MusicInfo> {
            val target = File(TargetDir, FullList)
            if (checkAndCreateExternalStorageFile(target)) {
                return mutableListOf()
            }
            // 防止无法实例化时抛出异常或类型无法转换而闪退
            return try {
                val inputStream = FileInputStream(target)
                val list: Any

                ObjectInputStream(inputStream).apply {
                    list = readObject()
                    inputStream.close()
                    close()
                }

                @Suppress("UNCHECKED_CAST")
                list as MutableList<MusicInfo>
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf()
            }
        }

        fun scanMusicByDataBase(): MutableList<MusicInfo> {
            val list =  mutableListOf<MusicInfo>()
             MainApplication.getMainApplication().contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.AudioColumns.IS_MUSIC).apply {
                if (this == null)
                    return list
                if (moveToFirst()) {
                    do {
                        val path = getString(getColumnIndex(MediaStore.Audio.Media.DATA))
                        if (path == "-1") {
                            //Log.e("MusicClass", "-1")
                            continue
                        }
                        //list.add(MusicInfo(path))
                        list.add(MusicInfo(path,
                            getString(getColumnIndex(MediaStore.Audio.Media.TITLE)),
                            getString(getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                            getString(getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                            getInt(getColumnIndex(MediaStore.Audio.Media.DURATION))
                            ))
                        //Log.e("MusicClass", path)
                    } while (moveToNext())
                }
                close()
            }

            Log.e("list", list.size.toString())
            return list
        }

        //private fun loadPlayLists():
    }
}