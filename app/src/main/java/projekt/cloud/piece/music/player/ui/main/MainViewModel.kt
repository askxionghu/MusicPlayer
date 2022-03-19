package projekt.cloud.piece.music.player.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import projekt.cloud.piece.music.player.database.item.AlbumItem
import projekt.cloud.piece.music.player.database.item.ArtistItem
import projekt.cloud.piece.music.player.ui.main.album.AlbumFragment
import projekt.cloud.piece.music.player.ui.main.artist.ArtistFragment
import projekt.cloud.piece.music.player.ui.main.home.HomeFragment

class MainViewModel: ViewModel() {

    var isExtended = false

    var isDestroyed = false

    var isAlbumListLoaded = false
    lateinit var albumList: List<AlbumItem>

    var isArtistListLoaded = false
    lateinit var artistList: List<ArtistItem>

    lateinit var defaultAlbumCover: Bitmap
    lateinit var defaultArtistArt: Bitmap

    val fragments = listOf(
        HomeFragment(),
        AlbumFragment(),
        ArtistFragment()
    )

}