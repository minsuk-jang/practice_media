package com.example.myapplication.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import com.example.myapplication.BindingActivity
import com.example.myapplication.R
import com.example.myapplication.client.MediaBrowserHelper
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.service.MusicService
import com.example.myapplication.service.contentcatalogs.MusicLibrary

class MainActivity : BindingActivity<ActivityMainBinding>() {
    override fun getLayoutIds(): Int = R.layout.activity_main

    private var mMediaBrowserHelper : MediaBrowserHelper? =null
    private var mIsPlaying = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val clickListener = ClickListener()
        binding.buttonNext.setOnClickListener(clickListener)
        binding.buttonPlay.setOnClickListener(clickListener)
        binding.buttonPrevious.setOnClickListener(clickListener)

        mMediaBrowserHelper = MediaBrowserConnection(this)
        mMediaBrowserHelper?.registerCallback(MediaBrowserListener())
    }

    override fun onStart() {
        super.onStart()
        mMediaBrowserHelper?.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.seekbarAudio.disconnectController()
        mMediaBrowserHelper?.onStop()
    }

    private inner class ClickListener : View.OnClickListener{
        override fun onClick(v: View?) {
            when(v?.id){
                R.id.button_previous -> mMediaBrowserHelper?.getTransportControls()?.skipToPrevious()
                R.id.button_play ->{
                  if(mIsPlaying){
                      mMediaBrowserHelper?.getTransportControls()?.pause()
                  }else
                      mMediaBrowserHelper?.getTransportControls()?.play()
                }
                R.id.button_next -> mMediaBrowserHelper?.getTransportControls()?.skipToNext()
            }
        }
    }

    private inner class MediaBrowserConnection(private val context : Context) : MediaBrowserHelper(context, MusicService::class.java){
        override fun onConnected(mediaController: MediaControllerCompat) {
            binding.seekbarAudio.setMediaController(mediaController)
        }

        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)

            val mediaController = getMediaController()

            for(item : MediaBrowserCompat.MediaItem in children){
                mediaController.addQueueItem(item.description)
            }

            mediaController.transportControls.prepare()
        }
    }

    private inner class MediaBrowserListener : MediaControllerCompat.Callback(){
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }


        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            mIsPlaying = state != null && state.state == PlaybackStateCompat.STATE_PLAYING
            binding.mediaControls.isPressed = mIsPlaying
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.run {
                binding.songTitle.text = getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                binding.songArtist.text = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                binding.albumArt.setImageBitmap(
                    MusicLibrary.getAlbumBitmap(
                    this@MainActivity,
                    getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                ))
            } ?: return

        }
    }
}