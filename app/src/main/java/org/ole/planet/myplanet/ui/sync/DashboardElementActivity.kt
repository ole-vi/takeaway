package org.ole.planet.myplanet.ui.sync

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.callback.OnRatingChangeListener
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.SettingActivity
import org.ole.planet.myplanet.ui.community.CommunityTabFragment
import org.ole.planet.myplanet.ui.course.CourseFragment
import org.ole.planet.myplanet.ui.dashboard.BellDashboardFragment
import org.ole.planet.myplanet.ui.dashboard.DashboardFragment
import org.ole.planet.myplanet.ui.feedback.FeedbackFragment
import org.ole.planet.myplanet.ui.library.LibraryFragment
import org.ole.planet.myplanet.ui.rating.RatingFragment.Companion.newInstance
import org.ole.planet.myplanet.ui.survey.SurveyFragment
import org.ole.planet.myplanet.ui.team.TeamFragment
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import org.ole.planet.myplanet.utilities.Constants.showBetaFeature
import org.ole.planet.myplanet.utilities.SharedPrefManager
import org.ole.planet.myplanet.utilities.Utilities

abstract class DashboardElementActivity : SyncActivity(), FragmentManager.OnBackStackChangedListener {
    lateinit var navigationView: BottomNavigationView
    var doubleBackToExitPressedOnce = false
    private lateinit var goOnline: MenuItem
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileDbHandler = UserProfileDbHandler(this)
        settings = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefData = SharedPrefManager(this)
    }

    fun onClickTabItems(position: Int) {
        when (position) {
            0 -> openCallFragment(BellDashboardFragment(), "dashboard")
            1 -> openCallFragment(LibraryFragment(), "library")
            2 -> openCallFragment(CourseFragment(), "course")
            4 -> openEnterpriseFragment()
            3 -> openCallFragment(TeamFragment(), "survey")
            5 -> openCallFragment(CommunityTabFragment(), "community")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        goOnline = menu.findItem(R.id.menu_goOnline)
        return true
    }

    fun openCallFragment(newfragment: Fragment, tag: String?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, newfragment, tag)
        supportFragmentManager.addOnBackStackChangedListener(this)
        fragmentTransaction.addToBackStack("")
        fragmentTransaction.commit()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        goOnline.setVisible(showBetaFeature(Constants.KEY_SYNC, this))
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_goOnline -> {
                wifiStatusSwitch()
                return true
            }
            R.id.menu_logout -> {
                logout()
            }
            R.id.action_feedback -> {
                openCallFragment(FeedbackFragment(), getString(R.string.menu_feedback))
            }
            R.id.action_setting -> {
                startActivity(Intent(this, SettingActivity::class.java))
            }
            R.id.action_sync -> {
                isServerReachable(Utilities.getUrl())
                startUpload()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("RestrictedApi")
    fun wifiStatusSwitch() {
        val resIcon = ContextCompat.getDrawable(this, R.drawable.goonline)
        val connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
        if (mWifi?.isConnected == true) {
            wifi.setWifiEnabled(false)
            if (resIcon != null) {
                DrawableCompat.setTintMode(resIcon.mutate(), PorterDuff.Mode.SRC_ATOP)
                DrawableCompat.setTint(resIcon, ContextCompat.getColor(this, R.color.green))
            }
            goOnline.setIcon(resIcon)
            Toast.makeText(this, getString(R.string.wifi_is_turned_off_saving_battery_power), Toast.LENGTH_LONG).show()
        } else {
            wifi.setWifiEnabled(true)
            Toast.makeText(this, getString(R.string.turning_on_wifi_please_wait), Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({ connectToWifi() }, 5000)
            if (resIcon != null) {
                DrawableCompat.setTintMode(resIcon.mutate(), PorterDuff.Mode.SRC_ATOP)
                DrawableCompat.setTint(resIcon, ContextCompat.getColor(this, R.color.accent))
            }
            goOnline.setIcon(resIcon)
        }
    }

    private fun connectToWifi() {
        val id = settings.getInt("LastWifiID", -1)
        Utilities.log("LAST SSID $id")
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val netId: Int
        for (tmp in wifiManager.configuredNetworks) {
            if (tmp.networkId > -1 && tmp.networkId == id) {
                netId = tmp.networkId
                wifiManager.enableNetwork(netId, true)
                Toast.makeText(this, R.string.you_are_now_connected + netId, Toast.LENGTH_SHORT).show()
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("ACTION_NETWORK_CHANGED"))
                break
            }
            Utilities.log("SSID " + tmp.SSID)
        }
    }

    fun logout() {
        profileDbHandler.onLogout()
        settings.edit().putBoolean(Constants.KEY_LOGIN, false).apply()
        settings.edit().putBoolean(Constants.KEY_NOTIFICATION_SHOWN, false).apply()
        val loginscreen = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(loginscreen)
        doubleBackToExitPressedOnce = true
        finish()
    }

    override fun finish() {
        if (doubleBackToExitPressedOnce) {
            super.finish()
        } else {
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    fun showRatingDialog(type: String?, resource_id: String?, title: String?, listener: OnRatingChangeListener?) {
        val f = newInstance(type, resource_id, title)
        f.setListener(listener)
        f.show(supportFragmentManager, "")
    }

    override fun onBackStackChanged() {
        val f = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val fragmentTag = f?.tag
        if (f is CourseFragment) {
            if ("shelf" == fragmentTag) navigationView.menu.findItem(R.id.menu_mycourses)
                .setChecked(true) else navigationView.menu.findItem(R.id.menu_courses)
                .setChecked(true)
        } else if (f is LibraryFragment) {
            if ("shelf" == fragmentTag) navigationView.menu.findItem(R.id.menu_mylibrary)
                .setChecked(true) else navigationView.menu.findItem(R.id.menu_library)
                .setChecked(true)
        } else if (f is DashboardFragment) {
            navigationView.menu.findItem(R.id.menu_home).setChecked(true)
        } else if (f is SurveyFragment) {
        }
    }

    fun openEnterpriseFragment() {
        val fragment: Fragment = TeamFragment()
        val b = Bundle()
        b.putString("type", "enterprise")
        fragment.arguments = b
        openCallFragment(fragment, "Enterprise")
    }
}
