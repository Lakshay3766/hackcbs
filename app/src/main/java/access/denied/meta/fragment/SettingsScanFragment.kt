package access.denied.meta.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import access.denied.meta.R
import access.denied.meta.adapter.SettingsAdapter
import access.denied.meta.models.SysSettings
import access.denied.meta.utillies.SettingsHelper


class SettingsScanFragment : Fragment() {

    private var settingsList: MutableList<SysSettings> = mutableListOf()
    private lateinit var settingsAdapter: SettingsAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_settings_scan, container, false)

        settingsList = SettingsHelper(context).scan()

        settingsAdapter = SettingsAdapter(settingsList, view.context)
        progressBar = view.findViewById(R.id.loading_spinner_settings)
        progressBar.visibility = View.GONE

        view.findViewById<RecyclerView>(R.id.recyclerViewSettings).apply {
            settingsAdapter = SettingsAdapter(settingsList, view.context)
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            adapter = settingsAdapter
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        settingsList = SettingsHelper(context).scan()
        view?.findViewById<RecyclerView>(R.id.recyclerViewSettings)?.apply {
            settingsAdapter = SettingsAdapter(settingsList, requireView().context)
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            adapter = settingsAdapter
        }
    }
}