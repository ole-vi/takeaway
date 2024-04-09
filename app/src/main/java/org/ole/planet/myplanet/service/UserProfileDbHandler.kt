package org.ole.planet.myplanet.service

import android.content.Context
import android.content.SharedPreferences
import io.realm.Realm
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmOfflineActivity
import org.ole.planet.myplanet.model.RealmOfflineActivity.Companion.getRecentLogin
import org.ole.planet.myplanet.model.RealmResourceActivity
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import org.ole.planet.myplanet.utilities.Utilities
import java.util.Date
import java.util.UUID

class UserProfileDbHandler(context: Context) {
    private val settings: SharedPreferences
    var mRealm: Realm
    private val realmService: DatabaseService
    private val fullName: String

    init {
        realmService = DatabaseService(context)
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fullName = Utilities.getUserName(settings)
        mRealm = realmService.realmInstance
    }

    val userModel: RealmUserModel?
        get() = mRealm.where(RealmUserModel::class.java)
            .equalTo("id", settings.getString("userId", "")).findFirst()

    fun onLogin() {
        if (!mRealm.isInTransaction) {
            mRealm.beginTransaction()
        }
        val offlineActivities = mRealm.copyToRealm(createUser())
        offlineActivities.type = KEY_LOGIN
        offlineActivities._rev = null
        offlineActivities._id = null
        offlineActivities.description = "Member login on offline application"
        offlineActivities.loginTime = Date().time
        mRealm.commitTransaction()
    }

    fun onLogout() {
        if (!mRealm.isInTransaction) {
            mRealm.beginTransaction()
        }
        val offlineActivities = getRecentLogin(mRealm) ?: return
        offlineActivities.logoutTime = Date().time
        mRealm.commitTransaction()
    }

    fun onDestory() {
        if (!mRealm.isClosed) {
            mRealm.close()
        }
    }

    private fun createUser(): RealmOfflineActivity {
        val offlineActivities = mRealm.createObject(RealmOfflineActivity::class.java, UUID.randomUUID().toString())
        val model = userModel
        offlineActivities.userId = model?.id
        offlineActivities.userName = model?.name
        offlineActivities.parentCode = model?.parentCode
        offlineActivities.createdOn = model?.planetCode
        return offlineActivities
    }

    val lastVisit: Long?
        get() = mRealm.where(RealmOfflineActivity::class.java).max("loginTime") as Long?
    val offlineVisits: Int
        get() = getOfflineVisits(userModel)

    fun getOfflineVisits(m: RealmUserModel?): Int {
        val db_users = mRealm.where(RealmOfflineActivity::class.java).equalTo("userName", m?.name).equalTo("type", KEY_LOGIN).findAll()
        return if (!db_users.isEmpty()) {
            db_users.size
        } else {
            0
        }
    }

    fun setResourceOpenCount(item: RealmMyLibrary) {
        setResourceOpenCount(item, KEY_RESOURCE_OPEN)
    }

    fun setResourceOpenCount(item: RealmMyLibrary, type: String?) {
        val model = userModel
        if (model?.id?.startsWith("guest") == true) {
            return
        }
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        val offlineActivities = mRealm.copyToRealm(createResourceUser(model))
        offlineActivities.type = type
        offlineActivities.title = item.title
        offlineActivities.resourceId = item.resourceId
        offlineActivities.time = Date().time
        mRealm.commitTransaction()
        Utilities.log("Set resource open")
    }

    private fun createResourceUser(model: RealmUserModel?): RealmResourceActivity {
        val offlineActivities = mRealm.createObject(
            RealmResourceActivity::class.java, UUID.randomUUID().toString()
        )
        offlineActivities.user = model?.name
        offlineActivities.parentCode = model?.parentCode
        offlineActivities.createdOn = model?.planetCode
        return offlineActivities
    }

    val numberOfResourceOpen: String
        get() {
            val count = mRealm.where(RealmResourceActivity::class.java).equalTo("user", fullName)
                .equalTo("type", KEY_RESOURCE_OPEN).count()
            return if (count == 0L) "" else "Resource opened $count times."
        }
    val maxOpenedResource: String
        get() {
            val result = mRealm.where(
                RealmResourceActivity::class.java
            ).equalTo("user", fullName).equalTo("type", KEY_RESOURCE_OPEN).findAll().where()
                .distinct("resourceId").findAll()
            var maxCount = 0L
            var maxOpenedResource = ""
            for (realm_resourceActivities in result) {
                val count =
                    mRealm.where(RealmResourceActivity::class.java).equalTo("user", fullName)
                        .equalTo("type", KEY_RESOURCE_OPEN)
                        .equalTo("resourceId", realm_resourceActivities.resourceId).count()
                if (count > maxCount) {
                    maxCount = count
                    maxOpenedResource = "${realm_resourceActivities.title}"
                }
            }
            return if (maxCount == 0L) "" else "$maxOpenedResource opened $maxCount times"
        }

    fun changeTopbarSetting(o: Boolean) {
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        userModel?.isShowTopbar = o
        mRealm.commitTransaction()
    }

    companion object {
        const val KEY_LOGIN = "login"
        const val KEY_RESOURCE_OPEN = "visit"
        const val KEY_RESOURCE_DOWNLOAD = "download"
    }
}
