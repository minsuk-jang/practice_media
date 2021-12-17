package com.example.myapplication.service.contentcatalogs

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object MusicLibrary {
    private val music = TreeMap<String, MediaMetadataCompat>()
    private val albumRes = HashMap<String, Int>()
    private val musicFileName = HashMap<String, String>()

    val root = "root"

    init {
        createMediaMetadataCompat(
            "Jazz_In_Paris",
            "Jazz in Paris",
            "Media Right Productions",
            "Jazz & Blues",
            "Jazz",
            103,
            TimeUnit.SECONDS,
            "jazz_in_paris.mp3",
            R.drawable.album_jazz_blues,
            "album_jazz_blues"
        )
        createMediaMetadataCompat(
            "The_Coldest_Shoulder",
            "The Coldest Shoulder",
            "The 126ers",
            "Youtube Audio Library Rock 2",
            "Rock",
            160,
            TimeUnit.SECONDS,
            "the_coldest_shoulder.mp3",
            R.drawable.album_youtube_audio_library_rock_2,
            "album_youtube_audio_library_rock_2"
        )
    }

    fun getAlbumUri(albumArtResName: String): String {
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName
    }

    fun getMusicFileName(mediaId: String): String? {
        return musicFileName[mediaId]
    }

    fun getAlbumRes(mediaId: String): Int {
        return albumRes[mediaId]!!
    }

    fun getAlbumBitmap(context: Context, mediaId: String): Bitmap {
        return BitmapFactory.decodeResource(context.resources, getAlbumRes(mediaId))
    }

    fun getMediaItems(): List<MediaBrowserCompat.MediaItem> {
        return ArrayList<MediaBrowserCompat.MediaItem>().apply {
            music.values.forEach {
                add(MediaBrowserCompat.MediaItem(it.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }
        }
    }

    fun getMetadata(context: Context, mediaId: String): MediaMetadataCompat {
        val metadataWithoutBitmap = music[mediaId]
        val albumArt = getAlbumBitmap(context, mediaId)

        return MediaMetadataCompat.Builder().apply {
            arrayListOf(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                MediaMetadataCompat.METADATA_KEY_GENRE,
                MediaMetadataCompat.METADATA_KEY_TITLE
            ).forEach {
                putString(it, metadataWithoutBitmap?.getString(it))
            }

            putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                metadataWithoutBitmap?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
            )
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
        }.build()
    }


    private fun createMediaMetadataCompat(
        mediaId: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        duration: Long,
        durationUnit: TimeUnit,
        musicFilename: String,
        albumArtResId: Int,
        albumArtResName: String
    ) {
        music.put(
            mediaId,
            MediaMetadataCompat.Builder().apply {
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    TimeUnit.MILLISECONDS.convert(duration, durationUnit)
                )
                putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    getAlbumUri(albumArtResName)
                )
                putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                    getAlbumUri(albumArtResName)
                )
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)

            }.build()
        )

        albumRes.put(mediaId,albumArtResId)
        musicFileName.put(mediaId, musicFilename)
    }
}