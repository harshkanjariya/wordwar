package com.harshkanjariya.wordwar.assets

import android.content.Context
import android.media.MediaPlayer
import com.harshkanjariya.wordwar.R

object SoundManager {
    private var matchFoundPlayer: MediaPlayer? = null
    private var playerClaimedWordPlayer: MediaPlayer? = null
    private var gameEndedPlayer: MediaPlayer? = null

    fun preload(context: Context) {
        if (matchFoundPlayer == null) {
            matchFoundPlayer = MediaPlayer.create(context, R.raw.match_found)
        }
        if (playerClaimedWordPlayer == null) {
            playerClaimedWordPlayer = MediaPlayer.create(context, R.raw.match_found)
        }
        if (gameEndedPlayer == null) {
            gameEndedPlayer = MediaPlayer.create(context, R.raw.match_found)
        }
    }

    fun matchFound() {
        matchFoundPlayer?.start()
    }

    fun playerClaimedWord() {
        playerClaimedWordPlayer?.start()
    }

    fun gameEnded() {
        gameEndedPlayer?.start()
    }

    fun release() {
        matchFoundPlayer?.release()
        matchFoundPlayer = null

        playerClaimedWordPlayer?.release()
        playerClaimedWordPlayer = null

        gameEndedPlayer?.release()
        gameEndedPlayer = null
    }
}
