package com.example.myapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat

abstract class PlayerAdapter(mApplicationContext: Context) {
    private val MEDIA_VOLUME_DEFAULT = 1.0F
    private val MEDIA_VOLUME_DUCK = 0.2F

    private val AUDIO_NOISY_INTENT_FILTER = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private var mAudioNoisyReceiverRegistered = false
    private val mAudioNoisyReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action){
                if(isPlaying())
                    pause()
            }
        }
    }

    private val mAudioManager: AudioManager = mApplicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mAudioFocusHelper = AudioFocusHelper()

    private var mPlayOnAudioFocus = false

    abstract fun playFromMedia(metadata : MediaMetadataCompat)
    abstract fun getCurrentMedia() : MediaMetadataCompat
    abstract fun isPlaying() : Boolean

    fun play(){
        if(mAudioFocusHelper.requestAudioFocus()){
            registerAudioNoisyReceiver()
            onPlay()
        }
    }

    protected abstract fun onPlay()

    fun pause(){
        if(!mPlayOnAudioFocus){
            mAudioFocusHelper.abandonAudioFocus()
        }

        unregisterAudioNosiyReceiver()
        onPause()
    }

    protected abstract fun onPause()
    
}