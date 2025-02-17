package projekt.cloud.piece.music.player.util

import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
import android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
import android.media.AudioDeviceInfo.TYPE_USB_HEADSET
import android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES
import android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import projekt.cloud.piece.music.player.R

object AudioUtil {

    @JvmStatic
    val AudioManager.deviceDrawableId get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) outputDeviceApi23Impl()
        else outputDeviceImpl

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.M)
    private fun AudioManager.outputDeviceApi23Impl(): Int {
        var currentDevice = TYPE_BUILTIN_SPEAKER
        getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { audioDeviceInfo ->
            when (val type = audioDeviceInfo.type) {
                TYPE_BUILTIN_SPEAKER -> currentDevice = type
                TYPE_WIRED_HEADSET -> currentDevice = type
                TYPE_WIRED_HEADPHONES -> currentDevice = type
                TYPE_BLUETOOTH_A2DP -> currentDevice = type
                TYPE_USB_HEADSET -> currentDevice = type
            }
        }
        return when (currentDevice) {
            TYPE_BUILTIN_SPEAKER -> R.drawable.ic_speaker
            TYPE_WIRED_HEADSET -> R.drawable.ic_headset
            TYPE_WIRED_HEADPHONES -> R.drawable.ic_headset
            TYPE_BLUETOOTH_A2DP -> R.drawable.ic_bluetooth
            TYPE_USB_HEADSET -> R.drawable.ic_usb_headset
            else -> R.drawable.ic_speaker
        }
    }

    @JvmStatic
    private val AudioManager.outputDeviceImpl get() = when {
        @Suppress("DEPRECATION")
        isBluetoothA2dpOn -> R.drawable.ic_bluetooth
        @Suppress("DEPRECATION")
        isWiredHeadsetOn -> R.drawable.ic_headset
        isSpeakerphoneOn -> R.drawable.ic_speaker
        else -> R.drawable.ic_speaker
    }

}