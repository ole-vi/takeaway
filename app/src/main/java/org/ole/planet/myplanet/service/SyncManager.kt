package org.ole.planet.myplanet.service

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import io.realm.Realm
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.callback.SyncListener
import org.ole.planet.myplanet.datamanager.ApiClient.client
import org.ole.planet.myplanet.datamanager.ApiInterface
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.datamanager.ManagerSync
import org.ole.planet.myplanet.model.RealmMeetup.Companion.insert
import org.ole.planet.myplanet.model.RealmMyCourse.Companion.insertMyCourses
import org.ole.planet.myplanet.model.RealmMyCourse.Companion.saveConcatenatedLinksToPrefs
import org.ole.planet.myplanet.model.RealmMyLibrary.Companion.insertMyLibrary
import org.ole.planet.myplanet.model.RealmMyLibrary.Companion.removeDeletedResource
import org.ole.planet.myplanet.model.RealmMyLibrary.Companion.save
import org.ole.planet.myplanet.model.RealmMyTeam.Companion.insertMyTeams
import org.ole.planet.myplanet.model.RealmResourceActivity.Companion.onSynced
import org.ole.planet.myplanet.model.Rows
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import org.ole.planet.myplanet.utilities.Constants.ShelfData
import org.ole.planet.myplanet.utilities.JsonUtils.getJsonArray
import org.ole.planet.myplanet.utilities.JsonUtils.getString
import org.ole.planet.myplanet.utilities.NotificationUtil.cancel
import org.ole.planet.myplanet.utilities.NotificationUtil.create
import org.ole.planet.myplanet.utilities.Utilities
import java.io.IOException
import java.util.Date

class SyncManager private constructor(private val context: Context) {
    private var td: Thread? = null
    private val settings: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    lateinit var mRealm: Realm
    private var isSyncing = false
    private val stringArray = arrayOfNulls<String>(4)
    private var shelfDoc: Rows? = null
    private var listener: SyncListener? = null
    private val dbService: DatabaseService = DatabaseService(context)

    fun start(listener: SyncListener?) {
        this.listener = listener
        if (!isSyncing) {
            settings.edit().remove("concatenated_links").apply()
            listener?.onSyncStarted()
            authenticateAndSync()
        }
    }

    private fun destroy() {
        cancel(context, 111)
        isSyncing = false
        ourInstance = null
        settings.edit().putLong("LastSync", Date().time).apply()
        if (listener != null) {
            listener?.onSyncComplete()
        }
        try {
            if (::mRealm.isInitialized && !mRealm.isClosed) {
                mRealm.close()
                td?.interrupt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun authenticateAndSync() {
        td = Thread {
            if (TransactionSyncManager.authenticate()) {
                startSync()
            } else {
                handleException(context.getString(R.string.invalid_configuration))
                destroy()
            }
        }
        td?.start()
    }

    private fun startSync() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
                settings.edit().putString("LastWifiSSID", wifiInfo.ssid).apply()
            }

            isSyncing = true
            create(context, R.mipmap.ic_launcher, "Syncing data", "Please wait...")

            // Incremental Sync Operations
            val syncOperations = listOf<Pair<String, (Realm) -> Unit>>(
                "tablet_users" to { realm -> TransactionSyncManager.syncDb(realm, "tablet_users") },
                "courses" to { realm -> TransactionSyncManager.syncDb(realm, "courses") },
                "exams" to { realm -> TransactionSyncManager.syncDb(realm, "exams") },
                "ratings" to { realm -> TransactionSyncManager.syncDb(realm, "ratings") },
                "courses_progress" to { realm -> TransactionSyncManager.syncDb(realm, "courses_progress") },
                "achievements" to { realm -> TransactionSyncManager.syncDb(realm, "achievements") },
                "tags" to { realm -> TransactionSyncManager.syncDb(realm, "tags") },
                "submissions" to { realm -> TransactionSyncManager.syncDb(realm, "submissions") },
                "news" to { realm -> TransactionSyncManager.syncDb(realm, "news") },
                "feedback" to { realm -> TransactionSyncManager.syncDb(realm, "feedback") },
                "teams" to { realm -> TransactionSyncManager.syncDb(realm, "teams") },
                "tasks" to { realm -> TransactionSyncManager.syncDb(realm, "tasks") },
                "login_activities" to { realm -> TransactionSyncManager.syncDb(realm, "login_activities") },
                "meetups" to { realm -> TransactionSyncManager.syncDb(realm, "meetups") },
                "health" to { realm -> TransactionSyncManager.syncDb(realm, "health") },
                "certifications" to { realm -> TransactionSyncManager.syncDb(realm, "certifications") },
                "team_activities" to { realm -> TransactionSyncManager.syncDb(realm, "team_activities") },
                "chat_history" to { realm -> TransactionSyncManager.syncDb(realm, "chat_history") }
            )

            syncIncrementally(syncOperations) {
                ManagerSync.instance?.syncAdmin()
                GlobalScope.launch {
                    resourceTransactionSync()

                    // On completion
                    mRealm = dbService.realmInstance
                    onSynced(mRealm, settings)
                    destroy()
                }
            }
        } catch (err: Exception) {
            err.printStackTrace()
            handleException(err.message)
        }
    }

    private fun syncIncrementally(operations: List<Pair<String, (Realm) -> Unit>>, onComplete: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val syncTask = object : Runnable {
            override fun run() {
                if (index < operations.size) {
                    val (table, operation) = operations[index]
                    Realm.getDefaultInstance().use { realm ->
                        try {
                            operation(realm)
                        } catch (err: Exception) {
                            err.printStackTrace()
                            handleException("Error syncing $table: ${err.message}")
                        }
                    }
                    index++
                    handler.postDelayed(this, 100)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(syncTask)
    }

    private fun handleException(message: String?) {
        if (listener != null) {
            isSyncing = false
            MainApplication.syncFailedCount++
            listener?.onSyncFailed(message)
        }
    }

    private suspend fun resourceTransactionSync() {
        val apiInterface = client?.create(ApiInterface::class.java)
        try {
            // Perform the sync operation on a background thread
            withContext(Dispatchers.IO) {
                syncResource(apiInterface)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            handleException("Error during resource transaction sync: ${e.message}")
        }
    }
    @Throws(IOException::class)
    private fun syncResource(dbClient: ApiInterface?) {
        val newIds: MutableList<String?> = ArrayList()
        val allDocs = dbClient?.getJsonObject(Utilities.header, Utilities.getUrl() + "/resources/_all_docs?include_doc=false")
        val all = allDocs?.execute()
        val rows = getJsonArray("rows", all?.body())
        val keys: MutableList<String> = ArrayList()
        for (i in 0 until rows.size()) {
            val `object` = rows[i].asJsonObject
            if (!TextUtils.isEmpty(getString("id", `object`))) keys.add(getString("key", `object`))
            if (i == rows.size() - 1 || keys.size == 1000) {
                val obj = JsonObject()
                obj.add("keys", Gson().fromJson(Gson().toJson(keys), JsonArray::class.java))
                val response = dbClient?.findDocs(Utilities.header, "application/json", Utilities.getUrl() + "/resources/_all_docs?include_docs=true", obj)?.execute()
                if (response?.body() != null) {
                    val ids: List<String?> = save(getJsonArray("rows", response.body()), mRealm)
                    newIds.addAll(ids)
                }
                keys.clear()
            }
        }
        removeDeletedResource(newIds, mRealm)
    }

    private fun myLibraryTransactionSync() {
        val apiInterface = client?.create(ApiInterface::class.java)
        try {
            val res = apiInterface?.getDocuments(Utilities.header, Utilities.getUrl() + "/shelf/_all_docs")?.execute()?.body()
            for (i in res?.rows!!.indices) {
                shelfDoc = res.rows!![i]
                populateShelfItems(apiInterface)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun populateShelfItems(apiInterface: ApiInterface) {
        try {
            val jsonDoc = apiInterface.getJsonObject(Utilities.header, Utilities.getUrl() + "/shelf/" + shelfDoc?.id).execute().body()
            for (i in Constants.shelfDataList.indices) {
                val shelfData = Constants.shelfDataList[i]
                val array = getJsonArray(shelfData.key, jsonDoc)
                memberShelfData(array, shelfData)
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }

    private fun memberShelfData(array: JsonArray, shelfData: ShelfData) {
        if (array.size() > 0) {
            triggerInsert(shelfData.categoryKey, shelfData.type)
            check(array)
        }
    }

    private fun triggerInsert(categoryId: String, categoryDBName: String) {
        stringArray[0] = shelfDoc?.id
        stringArray[1] = categoryId
        stringArray[2] = categoryDBName
    }

    private fun check(arrayCategoryIds: JsonArray) {
        for (x in 0 until arrayCategoryIds.size()) {
            if (arrayCategoryIds[x] is JsonNull) {
                continue
            }
            validateDocument(arrayCategoryIds, x)
        }
    }

    private fun validateDocument(arrayCategoryIds: JsonArray, x: Int) {
        val apiInterface = client!!.create(ApiInterface::class.java)
        try {
            val resourceDoc = apiInterface.getJsonObject(Utilities.header, Utilities.getUrl() + "/" + stringArray[2] + "/" + arrayCategoryIds[x].asString).execute().body()
            resourceDoc?.let { triggerInsert(stringArray, it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun triggerInsert(stringArray: Array<String?>, resourceDoc: JsonObject) {
        when (stringArray[2]) {
            "resources" -> insertMyLibrary(stringArray[0], resourceDoc, mRealm)
            "meetups" -> insert(mRealm, resourceDoc)
            "courses" -> {
                if (!mRealm.isInTransaction){
                    mRealm.beginTransaction()
                }
                insertMyCourses(stringArray[0], resourceDoc, mRealm)
                if (mRealm.isInTransaction){
                    mRealm.commitTransaction()
                }
            }
            "teams" -> insertMyTeams(resourceDoc, mRealm)
        }
        saveConcatenatedLinksToPrefs()
    }

    companion object {
        private var ourInstance: SyncManager? = null
        val instance: SyncManager?
            get() {
                ourInstance = SyncManager(MainApplication.context)
                return ourInstance
            }
    }
}