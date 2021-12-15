package com.example.myapplication.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import com.example.myapplication.BindingActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : BindingActivity<ActivityMainBinding>() {
    override fun getLayoutIds(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private class ClickListener : View.OnClickListener{
        override fun onClick(v: View?) {
            when(v.id){
                R.id.button_previous ->
            }
        }
    }

    private inner class MediaBrowserListener : MediaControllerCompat.Callback(){
        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            mIsplaying = playbackState != null && playbackState.state == PlaybackStateCompat.STATE_PLAYING
            mMediaControlsImage.setPressed(mIsplaying)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.run {
                binding.songTitle.text = getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                binding.songArtist.text = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                binding.albumArt.setImageBitmap(MusicLibrary.getAlbumBitmap(
                    this@MainActivity,
                    getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                ))
            }
        }
    }
}