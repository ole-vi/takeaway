package org.ole.planet.myplanet.datamanager

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.realm.Realm
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.callback.SyncListener
import org.ole.planet.myplanet.model.RealmUserModel.Companion.populateUsersTable
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.AndroidDecrypter.Companion.AndroidDecrypter
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import org.ole.planet.myplanet.utilities.JsonUtils
import org.ole.planet.myplanet.utilities.Utilities
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ManagerSync private constructor(context: Context) {
    private val settings: SharedPreferences
    private val dbService: DatabaseService
    private val mRealm: Realm

    init {
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbService = DatabaseService(context)
        mRealm = dbService.realmInstance
    }

    fun login(userName: String?, password: String?, listener: SyncListener) {
        listener.onSyncStarted()
        Utilities.log(Utilities.getUrl() + "/org.couchdb.user:" + userName)
        val apiInterface = ApiClient.client?.create(ApiInterface::class.java)
        apiInterface?.getJsonObject("Basic " + Base64.encodeToString("$userName:$password".toByteArray(), Base64.NO_WRAP), String.format("%s/_users/%s", Utilities.getUrl(), "org.couchdb.user:$userName"))
            ?.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful && response.body() != null) {
                        val jsonDoc = response.body()
                        if (jsonDoc?.has("derived_key") == true && jsonDoc.has("salt")) {
//                          val decrypt = AndroidDecrypter()
                            val derivedKey = jsonDoc["derived_key"].asString
                            val salt = jsonDoc["salt"].asString
                            if (AndroidDecrypter(userName, password, derivedKey, salt)) {
                                checkManagerAndInsert(jsonDoc, mRealm, listener)
                            } else {
                                listener.onSyncFailed("Name or password is incorrect.")
                            }
                        } else {
                            listener.onSyncFailed("JSON response is missing required keys.")
                        }
                    } else {
                        listener.onSyncFailed("Name or password is incorrect.")
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    listener.onSyncFailed("Server not reachable.")
                }
            })
    }

    fun syncAdmin() {
        val `object` = JsonObject()
        val selector = JsonObject()
        selector.addProperty("isUserAdmin", true)
        `object`.add("selector", selector)
        val apiInterface = ApiClient.client?.create(ApiInterface::class.java)
        apiInterface?.findDocs(Utilities.header, "application/json", Utilities.getUrl() + "/_users/_find", `object`)?.enqueue(object : Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.body() != null) {
                    val array = JsonUtils.getJsonArray("docs", response.body())
                    if (array.size() > 0) {
                        settings.edit().putString("user_admin", Gson().toJson(array[0])).apply()
                    }
                }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun checkManagerAndInsert(jsonDoc: JsonObject?, realm: Realm, listener: SyncListener) {
        Utilities.log("Check manager and insert")
        if (isManager(jsonDoc)) {
            populateUsersTable(jsonDoc, realm, settings)
            listener.onSyncComplete()
        } else {
            listener.onSyncFailed("The user is not a manager.")
        }
    }

    private fun isManager(jsonDoc: JsonObject?): Boolean {
        val roles = jsonDoc?.get("roles")?.asJsonArray
        val isManager = roles.toString().lowercase(Locale.getDefault()).contains("manager")
        return jsonDoc?.get("isUserAdmin")?.asBoolean == true || isManager
    }

    companion object {
        private var ourInstance: ManagerSync? = null
        @JvmStatic
        val instance: ManagerSync?
            get() {
                ourInstance = ManagerSync(MainApplication.context)
                return ourInstance
            }
    }
}