package app.github1552980358.android.musicplayer.base

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import app.github1552980358.android.musicplayer.service.PlayService

/**
 * @file    : [BaseAppCompatActivity]
 * @author  : 1552980358
 * @since   : 0.1
 * @date    : 2020/5/9
 * @time    : 12:04
 **/

abstract class BaseAppCompatActivity: AppCompatActivity() {
    /**
     * [mediaBrowserCompat] <[MediaBrowserCompat]>
     * @author 1552980358
     * @since 0.1
     **/
    lateinit var mediaBrowserCompat: MediaBrowserCompat
    
    /**
     * [connectionCallback] <[MediaBrowserCompat.ConnectionCallback]>
     * @author 1552980358
     * @since 0.1
     **/
    lateinit var connectionCallback: MediaBrowserCompat.ConnectionCallback
    
    /**
     * [subscriptionCallback] <[MediaBrowserCompat.SubscriptionCallback]>
     * @author 1552980358
     * @since 0.1
     **/
    lateinit var subscriptionCallback: MediaBrowserCompat.SubscriptionCallback
    
    /**
     * [callback] <[MediaControllerCompat.Callback]>
     * @author 1552980358
     * @since 0.1
     **/
    lateinit var callback: MediaControllerCompat.Callback
    
    /**
     * [mediaControllerCompat] <[MediaControllerCompat]>
     * @author 1552980358
     * @since 0.1
     **/
    lateinit var mediaControllerCompat: MediaControllerCompat
    
    /**
     * [onCreate]
     * @param savedInstanceState [Bundle]?
     * @author 1552980358
     * @since 0.1
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        callback = object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                this@BaseAppCompatActivity.onMetadataChanged(metadata)
            }
        
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                this@BaseAppCompatActivity.onPlaybackStateChanged(state)
            }
        }
    
        connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        
            override fun onConnectionSuspended() {
                Log.e("connectionCallback", "onConnectionSuspended")
            }
        
            override fun onConnected() {
                Log.e("connectionCallback", "onConnected")
                if (mediaBrowserCompat.isConnected) {
                
                    // Subscription
                    mediaBrowserCompat.unsubscribe("root")
                    mediaBrowserCompat.subscribe("root", subscriptionCallback)
                
                    // Update controller
                    mediaControllerCompat = MediaControllerCompat(this@BaseAppCompatActivity, mediaBrowserCompat.sessionToken)
                    MediaControllerCompat.setMediaController(this@BaseAppCompatActivity, mediaControllerCompat)
                    mediaControllerCompat.registerCallback(callback)
                }
                
            }
        
            override fun onConnectionFailed() {
                Log.e("connectionCallback", "onConnectionFailed")
            }
        
        }
    
        subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                Log.e("SubscriptionCallback", "onChildrenLoaded")
                this@BaseAppCompatActivity.onChildrenLoaded(parentId, children)
            }
        
            override fun onError(parentId: String) {
                super.onError(parentId)
                Log.e("SubscriptionCallback", "onError")
            }
        
            override fun onError(parentId: String, options: Bundle) {
                super.onError(parentId, options)
                Log.e("SubscriptionCallback", "onError")
            }
        }
    
        mediaBrowserCompat = MediaBrowserCompat(
            this@BaseAppCompatActivity,
            ComponentName(this@BaseAppCompatActivity, PlayService::class.java),
            connectionCallback,
            null
        )
        
    }
    
    /**
     * [onPause]
     * @author 1552980358
     * @since 0.1
     **/
    override fun onPause() {
        super.onPause()
        mediaBrowserCompat.disconnect()
    }
    
    /**
     * [onResume]
     * @author 1552980358
     * @since 0.1
     **/
    override fun onResume() {
        super.onResume()
        mediaBrowserCompat.connect()
    }
    
    /**
     * [onMetadataChanged]
     * @param metadata [MediaMetadataCompat]?
     * @author 1552980358
     * @since 0.1
     **/
    abstract fun onMetadataChanged(metadata: MediaMetadataCompat?)
    
    /**
     * [onPlaybackStateChanged]
     * @param state [PlaybackStateCompat]?
     * @author 1552980358
     * @since 0.1
     **/
    abstract fun onPlaybackStateChanged(state: PlaybackStateCompat?)
    
    /**
     * [onChildrenLoaded]
     * @param parentId [String]
     * @param children [MutableList]<[MediaBrowserCompat.MediaItem]>
     * @author 1552980358
     * @since 0.1
     **/
    abstract fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>)
    
}