package com.example.myapplication.client

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import org.jetbrains.annotations.NotNull
import java.lang.RuntimeException

open class MediaBrowserHelper(
    private val context: Context,
    private val serviceClass: Class<out MediaBrowserServiceCompat>
) {
    private val TAG = "jms8732"
    private val mCallbackList: ArrayList<MediaControllerCompat.Callback> = ArrayList()
    private val mMediaBrowserConnectionCallback = MediaBrowserConnectionCallback()
    private val mMediaControllerCallback = MediaControllerCallback()
    private val mMediaBrowserSubscriptionCallback = MediaBrowserSubscriptionCallback()


    private var mMediaBrowser: MediaBrowserCompat? = null
    private var mMediaController: MediaControllerCompat? = null

    fun onStart() {
        if (mMediaBrowser == null) {
            mMediaBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, serviceClass),
                mMediaBrowserConnectionCallback,
                null
            )

            mMediaBrowser?.connect()
        }

        Log.d(TAG, "onStart: Creating MediaBrowser, and connecting")
    }

    fun onStop() {
        if (mMediaController != null) {
            mMediaController?.unregisterCallback(mMediaControllerCallback)
            mMediaController = null
        }
        if (mMediaBrowser != null && mMediaBrowser?.isConnected == true) {
            mMediaBrowser?.disconnect()
            mMediaBrowser = null
        }

        resetState()
        Log.d(TAG, "onStop: Releasing MediaController, Disconnecting from MediaBrowser")
    }

    protected open fun onConnected(@NotNull mediaController: MediaControllerCompat) {}
    protected open fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {}
    protected open fun onDisconnected() {}

    protected fun getMediaController(): MediaControllerCompat {
        if (mMediaController == null)
            throw IllegalStateException("MediaController is null!")
        return mMediaController!!
    }

    private fun resetState() {
        performOnAllCallbacks(object : CallbackCommand {
            override fun perform(callback: MediaControllerCompat.Callback) {
                callback.onPlaybackStateChanged(null)
            }
        })

        Log.d(TAG, "resetState: ")
    }

    private fun performOnAllCallbacks(command: CallbackCommand) {
        mCallbackList.forEach {
            command.perform(it)
        }
    }

    fun getTransportControls(): MediaControllerCompat.TransportControls {
        if (mMediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!")
            throw IllegalStateException("MediaController is null!!")
        }

        return mMediaController?.transportControls!!
    }

    fun registerCallback(callback: MediaControllerCompat.Callback) {
        if (callback != null) {
            mCallbackList.add(callback)

            if (mMediaController != null) {
                val meta = mMediaController?.metadata
                if (meta != null) {
                    callback.onMetadataChanged(meta)
                }

                val playbackState = mMediaController?.playbackState
                if (playbackState != null) {
                    callback.onPlaybackStateChanged(playbackState)
                }
            }
        }
    }

    private interface CallbackCommand {
        fun perform(callback: MediaControllerCompat.Callback)
    }

    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            try {
                mMediaController = MediaControllerCompat(context, mMediaBrowser?.sessionToken!!)
                registerCallback(mMediaControllerCallback)
                mMediaControllerCallback.onMetadataChanged(mMediaController?.metadata)
                mMediaControllerCallback.onPlaybackStateChanged(mMediaController?.playbackState)

                this@MediaBrowserHelper.onConnected(mMediaController!!)
            } catch (e: RemoteException) {
                Log.d(TAG, "onConnected: Problem :$e")
                throw RuntimeException(e)
            }

            mMediaBrowser?.subscribe(mMediaBrowser?.root!!, mMediaBrowserSubscriptionCallback)
        }
    }

    inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            this@MediaBrowserHelper.onChildrenLoaded(parentId, children)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            performOnAllCallbacks(object : CallbackCommand {
                override fun perform(callback: MediaControllerCompat.Callback) {
                    if (metadata != null)
                        callback.onMetadataChanged(metadata)
                }
            })
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            performOnAllCallbacks(object : CallbackCommand {
                override fun perform(callback: MediaControllerCompat.Callback) {
                    if (state != null)
                        callback.onPlaybackStateChanged(state)
                }
            })
        }

        override fun onSessionDestroyed() {
            resetState()
            onPlaybackStateChanged(null)
        }
    }
}