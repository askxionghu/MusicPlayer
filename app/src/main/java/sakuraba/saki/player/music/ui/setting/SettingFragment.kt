package sakuraba.saki.player.music.ui.setting

import android.content.Context.POWER_SERVICE
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.view.Menu
import android.view.MenuInflater
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import lib.github1552980358.ktExtension.android.content.intent
import sakuraba.saki.player.music.BuildConfig.APPLICATION_ID
import sakuraba.saki.player.music.R
import sakuraba.saki.player.music.R.string.key_audio
import sakuraba.saki.player.music.R.string.key_play
import sakuraba.saki.player.music.R.string.key_web_server
import sakuraba.saki.player.music.util.PreferenceUtil.preference

class SettingFragment: PreferenceFragmentCompat() {

    private var batteryOptimization: Preference? = null
    private lateinit var powerManager: PowerManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xml_setting, rootKey)
        
        setHasOptionsMenu(true)
        
        preference(key_audio)?.setOnPreferenceClickListener {
            findNavController().navigate(SettingFragmentDirections.actionNavSettingToNavSettingAudioFilter())
            return@setOnPreferenceClickListener true
        }
        preference(key_play)?.setOnPreferenceClickListener {
            findNavController().navigate(SettingFragmentDirections.actionNavSettingToNavSettingAudioPlay())
            return@setOnPreferenceClickListener true
        }

        preference(key_web_server)?.setOnPreferenceClickListener {
            findNavController().navigate(SettingFragmentDirections.actionNavSettingToNavSettingWebServer())
            return@setOnPreferenceClickListener true
        }

        powerManager = requireContext().getSystemService(POWER_SERVICE) as PowerManager

        batteryOptimization = preference(R.string.key_other_battery_optimization)
        batteryOptimization?.setOnPreferenceClickListener {
            if (SDK_INT >= M) {
                requireContext().startActivity(intent {
                    @Suppress("BatteryLife")
                    action = ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$APPLICATION_ID")
                })
            }
            return@setOnPreferenceClickListener true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onResume() {
        super.onResume()
        if (SDK_INT >= M) {
            batteryOptimization?.isEnabled = !powerManager.isIgnoringBatteryOptimizations(APPLICATION_ID)
        }
    }
    
}