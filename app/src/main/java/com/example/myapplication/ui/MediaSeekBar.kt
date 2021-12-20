package com.example.myapplication.ui

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaController2
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class MediaSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?= null,
    defStyle: Int = 0
) : AppCompatSeekBar(context, attrs, defStyle) {
    private var mMediaController : MediaControllerCompat? = null
    private var mControllerCallback : ControllerCallback? =null

    private var mIsTracking = false
    private val mOnSeekBarChangeListener = object : OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            mIsTracking = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            mMediaController?.transportControls?.seekTo(progress.toLong())
            mIsTracking = false
        }
    }

    init{
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    }

    private var mProgressAnimator : ValueAnimator? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        throw UnsupportedOperationException("Cannot add listeners to a MediaSeekBar")
    }

    fun setMediaController(mediaController : MediaControllerCompat?){
        if(mediaController != null){
            mControllerCallback = ControllerCallback()
            mediaController.registerCallback(mControllerCallback!!)
        }else{
            mMediaController?.unregisterCallback(mControllerCallback!!)
            mControllerCallback = null
        }
        mMediaController = mediaController
    }

    fun disconnectController(){
        if(mMediaController != null){
            mMediaController?.unregisterCallback(mControllerCallback!!)
            mControllerCallback = null
            mMediaController = null
        }
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback(), ValueAnimator.AnimatorUpdateListener{
        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            if(mProgressAnimator != null){
                mProgressAnimator?.cancel()
                mProgressAnimator = null
            }

            val progress = state?.position ?: 0
            setProgress(progress.toInt())

            if(state != null && state.state == PlaybackStateCompat.STATE_PLAYING){
                val timeToEnd = ((max - progress) / state.playbackSpeed).toInt()

                mProgressAnimator = ValueAnimator.ofInt(progress.toInt(),max).apply {
                    duration = timeToEnd.toLong()
                    interpolator = LinearInterpolator()
                    addUpdateListener(this@ControllerCallback)
                }
                mProgressAnimator?.start()
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            val max = metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
            progress = 0
            setMax(max.toInt())
        }

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if(mIsTracking){
                animation?.cancel()
                return
            }

            val animatedIntValue = animation?.animatedValue as Int
            progress = animatedIntValue
        }
    }
}