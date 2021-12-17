package com.example.myapplication.service.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.drm.DrmStore
import android.graphics.Color
import android.media.session.PlaybackState
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.myapplication.R
import com.example.myapplication.service.MusicService
import com.example.myapplication.service.contentcatalogs.MusicLibrary
import com.example.myapplication.ui.MainActivity
import org.jetbrains.annotations.NotNull

class MediaNotificationManager(private val service: MusicService) {
    companion object{
        val NOTIFICATION_ID = 412
        val TAG = "jms8732"
        val CHANNEL_ID = "com.example.android.musicplayer.channel"
        val REQUEST_CODE = 501
    }

    private var mPlayAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_play_arrow_white_24dp,
        service.getString(R.string.label_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )
    private val mPauseAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_pause_white_24dp,
        service.getString(R.string.label_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )
    private val mNextAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_next_white_24dp,
        service.getString(R.string.label_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )

    private val mPrevAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white_24dp,
        service.getString(R.string.label_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    val mNotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        mNotificationManager.cancelAll()
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
    }

    fun getNotification(
        metadata: MediaMetadataCompat, @NotNull state: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ) : Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val description = metadata.description

        return buildNotification(state, token, isPlaying, description).build()
    }

    private fun buildNotification(
        @NotNull state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        description: MediaDescriptionCompat
    ): NotificationCompat.Builder {
        if (isAndroidOOrHigher()) {
            createChannel()
        }
        return NotificationCompat.Builder(service,CHANNEL_ID).apply {
            setStyle(
                MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0,1,2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            service,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
                .setColor(ContextCompat.getColor(service,R.color.notification_bg))
                .setSmallIcon(R.drawable.ic_audiotrack_white_24dp)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setLargeIcon(MusicLibrary.getAlbumBitmap(service,description.mediaId!!))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service,PlaybackStateCompat.ACTION_STOP
                ))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            if((state.actions.and(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) != 0L){
                addAction(mPrevAction)
            }
            addAction(if(isPlaying) mPauseAction else mPlayAction)

            if((state.actions.and(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) != 0L){
                addAction(mNextAction)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "MediaSession"
            val description = "MediaSession and Mediaplayer"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                setDescription(description)
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }

            mNotificationManager.createNotificationChannel(mChannel)
            Log.d(TAG, "createChannel: New Channel created")
        } else
            Log.d(TAG, "createChannel: Existing channel reused")
    }

    private fun isAndroidOOrHigher() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createContentIntent() = PendingIntent.getActivity(service, REQUEST_CODE,
        Intent(service, MainActivity::class.java).apply
        {
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }, PendingIntent.FLAG_CANCEL_CURRENT
    )
}