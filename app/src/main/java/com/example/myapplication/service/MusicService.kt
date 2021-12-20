package com.example.myapplication.service

import android.content.Intent
import android.drm.DrmStore
import android.media.MediaSession2Service
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.myapplication.service.contentcatalogs.MusicLibrary
import com.example.myapplication.service.notification.MediaNotificationManager
import com.example.myapplication.service.players.MediaPlayerAdapter

class MusicService : MediaBrowserServiceCompat() {
    private val TAG = "jms8732"

    private lateinit var mSession: MediaSessionCompat
    private lateinit var mPlayback: PlayerAdapter
    private lateinit var mMediaNotificationManager: MediaNotificationManager
    private var mServiceInStartedState = false

    override fun onCreate() {
        super.onCreate()

        mSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(MediaSessionCallback())
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
            setSessionToken(sessionToken)
        }

        mMediaNotificationManager = MediaNotificationManager(this)
        mPlayback = MediaPlayerAdapter(this, MediaPlayerListener())
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotificationManager")
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mMediaNotificationManager.onDestroy()
        mPlayback.stop()
        mSession.release()
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and mediaSession released")
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(MusicLibrary.root, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(MusicLibrary.getMediaItems().toMutableList())
    }


    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        private val mPlayList = ArrayList<MediaSessionCompat.QueueItem>()
        private var mQueueIndex = -1
        private var mPreparedMedia: MediaMetadataCompat? = null

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            mPlayList.add(MediaSessionCompat.QueueItem(description, description.hashCode().toLong()))
            mQueueIndex = if (mQueueIndex == -1) 0 else mQueueIndex
            mSession.setQueue(mPlayList)
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            mPlayList.remove(MediaSessionCompat.QueueItem(description, description.hashCode().toLong()))
            mQueueIndex = if (mPlayList.isEmpty()) -1 else mQueueIndex
            mSession.setQueue(mPlayList)
        }

        override fun onPrepare() {
            if (mQueueIndex < 0 && mPlayList.isEmpty()) return

            val mediaId = mPlayList[mQueueIndex].description.mediaId
            mPreparedMedia = MusicLibrary.getMetadata(this@MusicService, mediaId!!)
            mSession.setMetadata(mPreparedMedia)

            if (!mSession.isActive) {
                mSession.isActive = true
            }
        }

        override fun onPause() {
            mPlayback.pause()
        }

        override fun onPlay() {
            if (!isReadyToPlay()) return

            if (mPreparedMedia == null) {
                onPrepare()
            }

            mPlayback.playFromMedia(mPreparedMedia!!)
            Log.d(TAG, "onPlayFromMediaId: MediaSession active")
        }

        override fun onStop() {
            mPlayback.stop()
            mSession.isActive = false
        }

        override fun onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlayList.size)
            mPreparedMedia = null
            onPlay()
        }

        override fun onSkipToPrevious() {
            mQueueIndex = if (mQueueIndex > 0) mQueueIndex - 1 else mPlayList.size - 1
            mPreparedMedia = null
            onPlay()
        }

        override fun onSeekTo(pos: Long) {
            mPlayback.seekTo(pos)
        }

        private fun isReadyToPlay() = mPlayList.isNotEmpty()
    }

    inner class MediaPlayerListener : PlaybackInfoListener() {
        private val mServiceManager: ServiceManager = ServiceManager()


        override fun onPlaybackStateChange(state: PlaybackStateCompat) {
            mSession.setPlaybackState(state)

            Log.d(TAG, "onPlaybackStateChange: ${state.state}")

            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> mServiceManager.moveServiceToStatedState(state)
                PlaybackStateCompat.STATE_PAUSED -> mServiceManager.updateNotificationForPause(state)
                PlaybackStateCompat.STATE_STOPPED -> mServiceManager.moveServiceOutOfStartedState(state)
            }
        }

        //notification를 갱신해주는 클래스
        inner class ServiceManager {
            fun moveServiceToStatedState(state: PlaybackStateCompat) {
                val notification = mMediaNotificationManager.getNotification(
                    mPlayback.getCurrentMedia()!!,
                    state,
                    sessionToken!!
                )

                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(
                        this@MusicService,
                        Intent(this@MusicService, MusicService::class.java)
                    )

                    mServiceInStartedState = true
                }

                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
            }

            fun updateNotificationForPause(state: PlaybackStateCompat) {
                stopForeground(false)
                val notification = mMediaNotificationManager.getNotification(
                    mPlayback.getCurrentMedia()!!, state, sessionToken!!
                )

                mMediaNotificationManager.mNotificationManager.notify(MediaNotificationManager.NOTIFICATION_ID,notification)
            }

            fun moveServiceOutOfStartedState(state : PlaybackStateCompat){
                stopForeground(true)
                stopSelf()
                mServiceInStartedState = false
            }
        }
    }
}

