package org.ole.planet.myplanet

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ole.planet.myplanet.base.BaseResourceFragment.Companion.backgroundDownload
import org.ole.planet.myplanet.base.BaseResourceFragment.Companion.getAllLibraryList
import org.ole.planet.myplanet.base.BaseResourceFragment.Companion.settings
import org.ole.planet.myplanet.callback.TeamPageListener
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmApkLog
import org.ole.planet.myplanet.service.AutoSyncWorker
import org.ole.planet.myplanet.service.StayOnlineWorker
import org.ole.planet.myplanet.service.TaskNotificationWorker
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.Constants.KEY_AUTO_DOWNLOAD
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import org.ole.planet.myplanet.utilities.DownloadUtils.downloadAllFiles
import org.ole.planet.myplanet.utilities.LocaleHelper
import org.ole.planet.myplanet.utilities.NetworkUtils.initialize
import org.ole.planet.myplanet.utilities.NetworkUtils.isNetworkConnectedFlow
import org.ole.planet.myplanet.utilities.NetworkUtils.startListenNetworkState
import org.ole.planet.myplanet.utilities.NotificationUtil.cancelAll
import org.ole.planet.myplanet.utilities.ThemeMode
import org.ole.planet.myplanet.utilities.Utilities
import org.ole.planet.myplanet.utilities.VersionUtils.getVersionName
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        private const val AUTO_SYNC_WORK_TAG = "autoSyncWork"
        private const val STAY_ONLINE_WORK_TAG = "stayOnlineWork"
        private const val TASK_NOTIFICATION_WORK_TAG = "taskNotificationWork"
        lateinit var context: Context
        lateinit var mRealm: Realm
        lateinit var service: DatabaseService
        var preferences: SharedPreferences? = null
        @JvmField
        var syncFailedCount = 0
        @JvmField
        var isCollectionSwitchOn = false
        @JvmField
        var showDownload = false
        @JvmField
        var isSyncRunning = false
        var showHealthDialog = true
        @JvmField
        var listener: TeamPageListener? = null
        val androidId: String get() {
            try {
                return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "0"
        }
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        lateinit var defaultPref: SharedPreferences

        fun createLog(type: String) {
            service = DatabaseService(context)
            val mRealm = service.realmInstance
            if (!mRealm.isInTransaction) {
                mRealm.beginTransaction()
            }
            val log = mRealm.createObject(RealmApkLog::class.java, "${UUID.randomUUID()}")
            val model = UserProfileDbHandler(context).userModel
            if (model != null) {
                log.parentCode = model.parentCode
                log.createdOn = model.planetCode
            }
            log.time = "${Date().time}"
            log.page = ""
            log.version = getVersionName(context)
            log.type = type
            mRealm.commitTransaction()
        }

        private fun applyThemeMode(themeMode: String?) {
            when (themeMode) {
                ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        fun setThemeMode(themeMode: String) {
            val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("theme_mode", themeMode)
                apply()
            }
            applyThemeMode(themeMode)
        }

        suspend fun isServerReachable(urlString: String): Boolean {
            return try {
                val url = URL(urlString)
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                withContext(Dispatchers.IO) {
                    connection.connect()
                }
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode in 200..299

            } catch (e: Exception) {
                false
            }
        }
    }

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private var isFirstLaunch = true

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()
        initialize(CoroutineScope(Dispatchers.IO))

        context = this
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        service = DatabaseService(context)
        mRealm = service.realmInstance
        defaultPref = PreferenceManager.getDefaultSharedPreferences(this)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        if (preferences?.getBoolean("autoSync", false) == true && preferences?.contains("autoSyncInterval") == true) {
            val syncInterval = preferences?.getInt("autoSyncInterval", 60 * 60)
            scheduleAutoSyncWork(syncInterval)
        } else {
            cancelAutoSyncWork()
        }
        scheduleStayOnlineWork()
        scheduleTaskNotificationWork()

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable ->
            handleUncaughtException(e)
        }
        registerActivityLifecycleCallbacks(this)
        startListenNetworkState()
        onAppStarted()

        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getString("theme_mode", ThemeMode.FOLLOW_SYSTEM)

        applyThemeMode(themeMode)
        Log.d("MainApplication", "onCreate: themeMode: ${settings?.getBoolean("beta_auto_download", false)}")

        isNetworkConnectedFlow.onEach { isConnected ->
            if (isConnected) {
                val serverUrl = settings?.getString("serverURL", "")
                if (!serverUrl.isNullOrEmpty()) {
                    applicationScope.launch {
                        val canReachServer = withContext(Dispatchers.IO) {
                            isServerReachable(serverUrl)
                        }
                        if (canReachServer) {
                            if (defaultPref.getBoolean("beta_auto_download", false)) {
                                backgroundDownload(downloadAllFiles(getAllLibraryList(mRealm)))
                            }
                        }
                    }
                }
            }
        }.launchIn(applicationScope)
    }

    private fun scheduleAutoSyncWork(syncInterval: Int?) {
        val autoSyncWork: PeriodicWorkRequest? = syncInterval?.let { PeriodicWorkRequest.Builder(AutoSyncWorker::class.java, it.toLong(), TimeUnit.SECONDS).build() }
        val workManager = WorkManager.getInstance(this)
        if (autoSyncWork != null) {
            workManager.enqueueUniquePeriodicWork(AUTO_SYNC_WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, autoSyncWork)
        }
    }

    private fun cancelAutoSyncWork() {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelUniqueWork(AUTO_SYNC_WORK_TAG)
    }

    private fun scheduleStayOnlineWork() {
        val stayOnlineWork: PeriodicWorkRequest = PeriodicWorkRequest.Builder(StayOnlineWorker::class.java, 900, TimeUnit.SECONDS).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(STAY_ONLINE_WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, stayOnlineWork)
    }

    private fun scheduleTaskNotificationWork() {
        val taskNotificationWork: PeriodicWorkRequest = PeriodicWorkRequest.Builder(TaskNotificationWorker::class.java, 900, TimeUnit.SECONDS).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(TASK_NOTIFICATION_WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, taskNotificationWork)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"))
        Utilities.setContext(base)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.onAttach(this)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            onAppForegrounded()
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            onAppBackgrounded()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        cancelAll(this)
    }

    private fun onAppForegrounded() {
        if (isFirstLaunch) {
            isFirstLaunch = false
        } else {
            val fromForeground = "foreground"
            createLog(fromForeground)
        }
    }
    
    private fun onAppBackgrounded() {}

    private fun onAppStarted() {
        val newStart = "new launch"
        createLog(newStart)
    }

    private fun onAppClosed() {}

    private fun handleUncaughtException(e: Throwable) {
        e.printStackTrace()
        if (!mRealm.isInTransaction) {
            mRealm.beginTransaction()
        }
        val log = mRealm.createObject(RealmApkLog::class.java, "${UUID.randomUUID()}")
        val model = UserProfileDbHandler(this).userModel
        if (model != null) {
            log.parentCode = model.parentCode
            log.createdOn = model.planetCode
        }
        log.time = "${Date().time}"
        log.page = ""
        log.version = getVersionName(this)
        log.type = RealmApkLog.ERROR_TYPE_CRASH
        log.setError(e)
        mRealm.commitTransaction()
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }

    override fun onTerminate() {
        super.onTerminate()
        onAppClosed()
        applicationScope.cancel()
    }
}
