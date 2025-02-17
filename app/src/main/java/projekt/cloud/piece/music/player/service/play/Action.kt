package projekt.cloud.piece.music.player.service.play

import projekt.cloud.piece.music.player.BuildConfig.APPLICATION_ID

object Action {

    const val START_COMMAND_ACTION = "START_COMMAND_ACTION"
    const val START_COMMAND_ACTION_PLAY = "START_COMMAND_ACTION_PLAY"
    const val START_COMMAND_ACTION_PAUSE = "START_COMMAND_ACTION_PAUSE"
    
    const val BROADCAST_ACTION_PLAY = "${APPLICATION_ID}.play"
    const val BROADCAST_ACTION_PAUSE = "${APPLICATION_ID}.pause"
    const val BROADCAST_ACTION_PREV = "${APPLICATION_ID}.prev"
    const val BROADCAST_ACTION_NEXT = "${APPLICATION_ID}.next"
    
    const val ACTION_SYNC_SERVICE = "ACTION_SYNC_SERVICE"
    const val ACTION_PLAY_CONFIG_CHANGED = "ACTION_PLAY_CONFIG_CHANGED"
    const val ACTION_REQUEST_LIST = "ACTION_REQUEST_LIST"
    
}