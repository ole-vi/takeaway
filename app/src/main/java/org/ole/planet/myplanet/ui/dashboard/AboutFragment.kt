package org.ole.planet.myplanet.ui.dashboard

import android.R
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import org.ole.planet.myplanet.databinding.FragmentAboutBinding
import org.ole.planet.myplanet.utilities.Constants

class AboutFragment : Fragment() {
    private lateinit var fragmentAboutBinding: FragmentAboutBinding
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentAboutBinding = FragmentAboutBinding.inflate(inflater, container, false)
        val versionString = "${resources.getText(org.ole.planet.myplanet.R.string.version)}: ${resources.getText(org.ole.planet.myplanet.R.string.app_version)}"
        val aboutText = getString(Constants.ABOUT)

        val newAboutText: String = aboutText.replace("<h3>MyPlanet</h3>", "<h3>MyPlanet</h3>\n<h4>$versionString</h4>")
        fragmentAboutBinding.tvDisclaimer.text = Html.fromHtml(newAboutText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return fragmentAboutBinding.root
    }
}
