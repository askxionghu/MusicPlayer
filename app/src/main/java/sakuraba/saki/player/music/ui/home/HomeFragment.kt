package sakuraba.saki.player.music.ui.home

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri.fromParts
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import lib.github1552980358.ktExtension.androidx.coordinatorlayout.widget.makeShortSnack
import lib.github1552980358.ktExtension.androidx.fragment.app.findActivityViewById
import lib.github1552980358.ktExtension.jvm.keyword.tryOnly
import sakuraba.saki.player.music.BuildConfig.APPLICATION_ID
import sakuraba.saki.player.music.MainActivity.Companion.INTENT_ACTIVITY_FRAGMENT_INTERFACE
import sakuraba.saki.player.music.R
import sakuraba.saki.player.music.database.AudioDatabaseHelper
import sakuraba.saki.player.music.database.AudioDatabaseHelper.Companion.TABLE_ALBUM
import sakuraba.saki.player.music.database.AudioDatabaseHelper.Companion.TABLE_AUDIO
import sakuraba.saki.player.music.databinding.FragmentHomeBinding
import sakuraba.saki.player.music.service.util.AudioInfo
import sakuraba.saki.player.music.ui.home.util.DividerItemDecoration
import sakuraba.saki.player.music.ui.home.util.HomeFragmentData
import sakuraba.saki.player.music.ui.home.util.RecyclerViewAdapterUtil
import sakuraba.saki.player.music.util.ActivityFragmentInterface
import sakuraba.saki.player.music.util.BitmapUtil.loadAlbumArt
import sakuraba.saki.player.music.util.MediaAlbum
import sakuraba.saki.player.music.util.SettingUtil.getBooleanSetting
import sakuraba.saki.player.music.util.SettingUtil.getIntSettingOrThrow
import sakuraba.saki.player.music.util.SettingUtil.getStringSettingOrThrow

class HomeFragment: Fragment() {
    
    companion object {
        private const val TAG = "HomeFragment"
    }
    
    private lateinit var viewModel: HomeViewModel
    
    private var _fragmentHomeBinding: FragmentHomeBinding? = null
    private val fragmentHome get() = _fragmentHomeBinding!!
    
    private lateinit var registerRequestReadPermission: ActivityResultLauncher<String>
    
    private lateinit var audioDatabaseHelper: AudioDatabaseHelper
    
    private val isReadExternalStoragePermissionGained get() =
        ActivityCompat.checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    
    private var _activityFragmentInterface: ActivityFragmentInterface? = null
    private val activityFragmentInterface get() = _activityFragmentInterface!!
    
    private lateinit var mediaStoreObserver: ContentObserver
    
    private var updatingJob: Job? = null
    
    private lateinit var recyclerViewAdapter: RecyclerViewAdapterUtil
    
    private var homeFragmentData: HomeFragmentData? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        
        _fragmentHomeBinding = FragmentHomeBinding.inflate(inflater)
        
        _activityFragmentInterface = requireActivity().intent.getSerializableExtra(INTENT_ACTIVITY_FRAGMENT_INTERFACE) as ActivityFragmentInterface
        
        fragmentHome.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerViewAdapter = RecyclerViewAdapterUtil(homeFragmentData) { pos ->
            activityFragmentInterface.onFragmentListItemClick(pos, recyclerViewAdapter.audioInfoList[pos], recyclerViewAdapter.audioInfoList)
        }
        recyclerViewAdapter.setAdapterToRecyclerView(fragmentHome.recyclerView)
        findNavController().currentDestination
        fragmentHome.recyclerView.addItemDecoration(DividerItemDecoration())
        
        fragmentHome.root.isRefreshing = true
        fragmentHome.root.setOnRefreshListener {
            findActivityViewById<CoordinatorLayout>(R.id.coordinator_layout)?.makeShortSnack(R.string.home_snack_waiting_for_media_scanner)?.show()
            
            @Suppress("DEPRECATION")
            MediaScannerConnection.scanFile(requireContext(), arrayOf(Environment.getExternalStorageDirectory().absolutePath), arrayOf("audio/*")) { _, _ ->
                updatingJob?.cancel()
                updatingJob = CoroutineScope(Dispatchers.IO).launch {
                    recyclerViewAdapter.audioInfoList.clear()
                    launch(Dispatchers.Main) {
                        findActivityViewById<CoordinatorLayout>(R.id.coordinator_layout)?.makeShortSnack(R.string.home_snack_scanning)?.show()
                        recyclerViewAdapter.notifyDataSetChanged()
                    }
                    readAudioSystemDatabase(recyclerViewAdapter.audioInfoList)
                    if (recyclerViewAdapter.audioInfoList.isNotEmpty()) {
                        loadBitmaps(recyclerViewAdapter.audioInfoList, recyclerViewAdapter.bitmapMap)
                    }
                    launch(Dispatchers.Main) { fragmentHome.root.isRefreshing = false }
                }
            }
        }
        
        audioDatabaseHelper = AudioDatabaseHelper(requireContext())
        
        setHasOptionsMenu(true)
        
        return fragmentHome.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isReadExternalStoragePermissionGained) {
            registerRequestReadPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGained ->
                if (isGained) {
                    //viewModel.snackbar.dismiss()
                    updatingJob = CoroutineScope(Dispatchers.IO).launch {
                        readAudioSystemDatabase(recyclerViewAdapter.audioInfoList)
                        if (recyclerViewAdapter.audioInfoList.isNotEmpty()) {
                            loadBitmaps(recyclerViewAdapter.audioInfoList, recyclerViewAdapter.bitmapMap)
                        }
                        launch(Dispatchers.Main) { fragmentHome.root.isRefreshing = false }
                    }
                } else {
                    viewModel.initSnackBar(requireActivity()) {
                        requireContext().startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(fromParts("package", APPLICATION_ID, null)))
                    }
                    viewModel.snackbar.show()
                }
            }
            registerRequestReadPermission.launch(READ_EXTERNAL_STORAGE)
        } else {
            if (homeFragmentData?.hasData != true) {
                updatingJob = CoroutineScope(Dispatchers.IO).launch {
                    readDatabase(recyclerViewAdapter.audioInfoList)
                    if (recyclerViewAdapter.audioInfoList.isNotEmpty()) {
                        loadBitmaps(recyclerViewAdapter.audioInfoList, recyclerViewAdapter.bitmapMap)
                    }
                    launch(Dispatchers.Main) { fragmentHome.root.isRefreshing = false }
                }
            } else {
                fragmentHome.root.isRefreshing = false
            }
        }
        
        mediaStoreObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                recyclerViewAdapter.audioInfoList.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                fragmentHome.root.isRefreshing = true
                updatingJob?.cancel()
                updatingJob = CoroutineScope(Dispatchers.IO).launch {
                    readAudioSystemDatabase(recyclerViewAdapter.audioInfoList)
                    if (recyclerViewAdapter.audioInfoList.isNotEmpty()) {
                        loadBitmaps(recyclerViewAdapter.audioInfoList, recyclerViewAdapter.bitmapMap)
                    }
                }
            }
        }
    
        // Register content observer
        requireContext().contentResolver.apply {
            registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            // registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, observer)
        }
    }
    
    private fun readAudioSystemDatabase(audioInfoList: ArrayList<AudioInfo>) {
        Log.e(TAG, "readAudioSystemDatabase")
        // Delete all data in table
        audioDatabaseHelper.clearTable(TABLE_AUDIO)
        
        audioInfoList.clear()
        
        requireContext().contentResolver
            .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.AudioColumns.IS_MUSIC)?.apply {
                var id: String?
                var title: String?
                var artist: String?
                var album: String?
                var albumId: Long?
                var duration: Long?
                var size: Long?
                while (moveToNext()) {
                    id = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns._ID))
                    title = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
                    @Suppress("InlinedApi")
                    artist = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST))
                    @Suppress("InlinedApi")
                    album = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM))
                    albumId = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
                    @Suppress("InlinedApi")
                    duration = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
                    size = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.SIZE))
                    if (id != null && title != null && artist != null && album != null && albumId != null && duration != null && size != null) {
                        audioInfoList.add(AudioInfo(id, title, artist, album, albumId, duration, size))
                    }
                }
                close()
            }
        // Re-arrange
        if (audioInfoList.isNotEmpty()) {
            // audioInfoList.removeAll { audioInfo -> audioInfo.audioDuration < 3000 }
            audioInfoList.sortBy { it.audioId }
            audioDatabaseHelper.insertAudio(TABLE_AUDIO, audioInfoList)
            audioInfoList.sortBy { it.audioTitlePinyin }
            audioInfoList.forEachIndexed { index, audioInfo -> audioInfo.index = index }
            CoroutineScope(Dispatchers.Main).launch { recyclerViewAdapter.notifyDataSetChanged() }
            
            val mediaAlbumList = arrayListOf<MediaAlbum>()
            audioInfoList.forEach { audioInfo ->
                when (val index = mediaAlbumList.indexOfFirst { mediaAlbum -> mediaAlbum.albumId == audioInfo.audioAlbumId }) {
                    -1 -> mediaAlbumList.add(MediaAlbum(audioInfo.audioAlbumId, audioInfo.audioAlbum, audioInfo.audioAlbumPinyin))
                    else -> mediaAlbumList[index].numberOfAudio++
                }
            }
            mediaAlbumList.sortBy { mediaAlbum -> mediaAlbum.titlePinyin }
            audioDatabaseHelper.insertMediaAlbum(TABLE_ALBUM, mediaAlbumList)
        }
    }
    
    private fun readDatabase(audioInfoList: ArrayList<AudioInfo>) {
        Log.e(TAG, "readDatabase")
        audioInfoList.clear()
        audioDatabaseHelper.queryAllAudio(audioInfoList) {
            if (getBooleanSetting(R.string.key_audio_filter_size_enable)) {
                it[0] = (getIntSettingOrThrow(R.string.key_audio_filter_size_value) * 1000).toString()
            }
            if (getBooleanSetting(R.string.key_audio_filter_duration_enable)) {
                it[1] = getStringSettingOrThrow(R.string.key_audio_filter_duration_value)
            }
        }
        audioInfoList.sortBy { it.audioTitlePinyin }
        audioInfoList.forEachIndexed { index, audioInfo -> audioInfo.index = index }
        CoroutineScope(Dispatchers.Main).launch { recyclerViewAdapter.notifyDataSetChanged() }
    }
    
    private fun loadBitmaps(audioInfoList: ArrayList<AudioInfo>, bitmaps: MutableMap<Long, Bitmap?>) {
        Log.e(TAG, "loadBitmaps")
        bitmaps.clear()
        
        audioInfoList.forEach { audioInfo ->
            tryOnly {
                bitmaps[audioInfo.audioAlbumId] = loadAlbumArt(audioInfo.audioAlbumId)
            }
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            tryOnly { recyclerViewAdapter.notifyDataSetChanged() }
        }
    }
    
    fun setHomeFragmentData(homeFragmentData: HomeFragmentData) {
        this.homeFragmentData = homeFragmentData
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_home, menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (fragmentHome.root.isRefreshing) {
            return super.onOptionsItemSelected(item)
        }
        when (item.itemId) {
            R.id.action_settings -> findNavController().navigate(R.id.nav_setting)
            R.id.action_search -> findNavController().navigate(R.id.nav_search)
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onPause() {
        activityFragmentInterface.onHomeFragmentPaused(recyclerViewAdapter.audioInfoList, recyclerViewAdapter.bitmapMap)
        super.onPause()
    }
    
    override fun onDestroy() {
        requireContext().contentResolver.unregisterContentObserver(mediaStoreObserver)
        super.onDestroy()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentHomeBinding = null
        homeFragmentData = null
    }
    
}