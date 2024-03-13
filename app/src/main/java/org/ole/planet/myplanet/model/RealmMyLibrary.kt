package org.ole.planet.myplanet.model

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.FileUtils
import org.ole.planet.myplanet.utilities.JsonUtils
import org.ole.planet.myplanet.utilities.NetworkUtils
import java.util.Calendar
import java.util.Date

open class RealmMyLibrary : RealmObject() {
    @JvmField
    @PrimaryKey
    var id: String? = null
    private var _id: String? = null
    var userId: RealmList<String>? = null
        private set
    @JvmField
    var resourceRemoteAddress: String? = null
    @JvmField
    var resourceLocalAddress: String? = null
    @JvmField
    var resourceOffline = false
    @JvmField
    var resourceId: String? = null
    private var _rev: String? = null
    @JvmField
    var downloadedRev: String? = null
    @JvmField
    var isNeed_optimization = false
    @JvmField
    var publisher: String? = null
    @JvmField
    var linkToLicense: String? = null
    @JvmField
    var addedBy: String? = null
    @JvmField
    var uploadDate: String? = null
    @JvmField
    var createdDate: String? = null
    @JvmField
    var openWith: String? = null
    @JvmField
    var articleDate: String? = null
    @JvmField
    var kind: String? = null
    @JvmField
    var language: String? = null
    @JvmField
    var author: String? = null
    @JvmField
    var year: String? = null
    @JvmField
    var medium: String? = null
    @JvmField
    var title: String? = null
    @JvmField
    var averageRating: String? = null
    @JvmField
    var filename: String? = null
    @JvmField
    var mediaType: String? = null
    @JvmField
    var resourceType: String? = null
    @JvmField
    var description: String? = null
    @JvmField
    var sendOnAccept: String? = null
    @JvmField
    var translationAudioPath: String? = null
    @JvmField
    var sum = 0
    @JvmField
    var timesRated = 0
    @JvmField
    var resourceFor: RealmList<String>? = null
    @JvmField
    var subject: RealmList<String>? = null
    @JvmField
    var level: RealmList<String>? = null
    @JvmField
    var tag: RealmList<String>? = null
    @JvmField
    var languages: RealmList<String>? = null
    @JvmField
    var courseId: String? = null
    @JvmField
    var stepId: String? = null
    @JvmField
    var downloaded: String? = null
    @JvmField
    var isPrivate = false
    fun get_id(): String? {
        return _id
    }

    fun set_id(_id: String?) {
        this._id = _id
    }

    fun serializeResource(): JsonObject {
        val `object` = JsonObject()
        `object`.addProperty("_id", _id)
        `object`.addProperty("_rev", _rev)
        `object`.addProperty("_rev", _rev)
        `object`.addProperty("need_optimization", isNeed_optimization)
        `object`.add("resourceFor", getArray(resourceFor))
        `object`.addProperty("publisher", publisher)
        `object`.addProperty("linkToLicense", linkToLicense)
        `object`.addProperty("addedBy", addedBy)
        `object`.addProperty("uploadDate", uploadDate)
        `object`.addProperty("openWith", openWith)
        `object`.add("subject", getArray(subject))
        `object`.addProperty("kind", kind)
        `object`.addProperty("medium", medium)
        `object`.addProperty("language", language)
        `object`.addProperty("author", author)
        `object`.addProperty("sum", sum)
        `object`.addProperty("createdDate", uploadDate)
        `object`.add("level", getArray(level))
        `object`.add("languages", getArray(languages))
        `object`.add("tag", getArray(tag))
        `object`.addProperty("timesRated", timesRated)
        `object`.addProperty("year", year)
        `object`.addProperty("title", title)
        `object`.addProperty("averageRating", averageRating)
        `object`.addProperty("filename", filename)
        `object`.addProperty("mediaType", mediaType)
        `object`.addProperty("description", description)
        val ob = JsonObject()
        ob.add(resourceLocalAddress, JsonObject())
        `object`.add("_attachments", ob)
        return `object`
    }

    fun getArray(ar: RealmList<String>?): JsonArray {
        val sub = JsonArray()
        if (ar != null) {
            for (s in ar) {
                sub.add(s)
            }
        }
        return sub
    }

    fun setUserId(userId: String?) {
        if (this.userId == null) {
            this.userId = RealmList()
        }
        if (!this.userId!!.contains(userId) && !TextUtils.isEmpty(userId)) this.userId!!.add(userId)
    }

    fun isResourceOffline(): Boolean {
        return resourceOffline && TextUtils.equals(_rev, downloadedRev)
    }

    fun setResourceFor(array: JsonArray, resource: RealmMyLibrary?) {
        for (s in array) {
            if (s !is JsonNull && !resource!!.resourceFor!!.contains(s.asString)) resource.resourceFor!!.add(s.asString)
        }
    }

    fun setSubject(array: JsonArray, resource: RealmMyLibrary?) {
        for (s in array) {
            if (s !is JsonNull && !resource!!.subject!!.contains(s.asString)) resource.subject!!.add(s.asString)
        }
    }

    fun setLevel(array: JsonArray, resource: RealmMyLibrary?) {
        for (s in array) {
            if (s !is JsonNull && !resource!!.level!!.contains(s.asString)) resource.level!!.add(s.asString)
        }
    }

    fun setTag(array: JsonArray, resource: RealmMyLibrary?) {
        for (s in array) {
            if (s !is JsonNull && !resource!!.tag!!.contains(s.asString)) resource.tag!!.add(s.asString)
        }
    }

    fun setLanguages(array: JsonArray, resource: RealmMyLibrary?) {
        for (s in array) {
            if (s !is JsonNull && !resource!!.languages!!.contains(s.asString)) resource.languages!!.add(s.asString)
        }
    }

    fun setUserId(userId: RealmList<String>?) {
        this.userId = userId
    }

    val subjectsAsString: String
        get() {
            if (subject!!.isEmpty()) {
                return ""
            }
            var str = ""
            for (s in subject!!) {
                str += "$s, "
            }
            return str.substring(0, str.length - 1)
        }

    fun get_rev(): String? {
        return _rev
    }

    fun set_rev(_rev: String?) {
        this._rev = _rev
    }

    fun getPublisher(): String? {
        return publisher
    }

    fun setPublisher(publisher: String?) {
        var publisher = publisher
        publisher = publisher
    }

    override fun toString(): String {
        return title!!
    }

    fun removeUserId(id: String?) {
        userId!!.remove(id)
    }

    fun needToUpdate(): Boolean {
        return resourceLocalAddress != null && !resourceOffline || !TextUtils.equals(
            get_rev(),
            downloadedRev
        )
    }

    companion object {
        fun getMyLibraryByUserId(mRealm: Realm, settings: SharedPreferences): List<RealmMyLibrary> {
            val libs = mRealm.where(RealmMyLibrary::class.java).findAll()
            return getMyLibraryByUserId(settings.getString("userId", "--"), libs, mRealm)
        }

        fun getMyLibraryByUserId(userId: String?, libs: List<RealmMyLibrary>, mRealm: Realm): List<RealmMyLibrary> {
            val libraries: MutableList<RealmMyLibrary> = ArrayList()
            val ids = RealmMyTeam.getResourceIdsByUser(userId, mRealm)
            for (item in libs) {
                if (item.userId!!.contains(userId) || ids.contains(item.resourceId)) {
                    libraries.add(item)
                }
            }
            return libraries
        }

        @JvmStatic
        fun getMyLibraryByUserId(userId: String?, libs: List<RealmMyLibrary>): List<RealmMyLibrary> {
            val libraries: MutableList<RealmMyLibrary> = ArrayList()
            for (item in libs) {
                if (item.userId!!.contains(userId)) {
                    libraries.add(item)
                }
            }
            return libraries
        }

        @JvmStatic
        fun getOurLibrary(userId: String?, libs: List<RealmMyLibrary>): List<RealmMyLibrary> {
            val libraries: MutableList<RealmMyLibrary> = ArrayList()
            for (item in libs) {
                if (!item.userId!!.contains(userId)) {
                    libraries.add(item)
                }
            }
            return libraries
        }

        fun getIds(mRealm: Realm): Array<String?> {
            val list: List<RealmMyLibrary> = mRealm.where(RealmMyLibrary::class.java).findAll()
            val ids = arrayOfNulls<String>(list.size)
            var i = 0
            for (library in list) {
                ids[i] = library.resourceId
                i++
            }
            return ids
        }

        @JvmStatic
        fun removeDeletedResource(newIds: List<String?>, mRealm: Realm) {
            val ids = getIds(mRealm)
            for (id in ids) {
                if (!newIds.contains(id)) {
                    mRealm.where(RealmMyLibrary::class.java).equalTo("resourceId", id).findAll().deleteAllFromRealm()
                }
            }
        }

        @JvmStatic
        fun serialize(personal: RealmMyLibrary, user: RealmUserModel?): JsonObject {
            val `object` = JsonObject()
            `object`.addProperty("title", personal.title)
            `object`.addProperty("uploadDate", Date().time)
            `object`.addProperty("createdDate", personal.createdDate)
            `object`.addProperty("filename", FileUtils.getFileNameFromUrl(personal.resourceLocalAddress))
            `object`.addProperty("author", user?.name)
            `object`.addProperty("addedBy", user?.id)
            `object`.addProperty("medium", personal.medium)
            `object`.addProperty("description", personal.description)
            `object`.addProperty("year", personal.year)
            `object`.addProperty("language", personal.language)
            `object`.add("subject", JsonUtils.getAsJsonArray(personal.subject))
            `object`.add("level", JsonUtils.getAsJsonArray(personal.level))
            `object`.addProperty("resourceType", personal.resourceType)
            `object`.addProperty("openWith", personal.openWith)
            `object`.add("resourceFor", JsonUtils.getAsJsonArray(personal.resourceFor))
            `object`.addProperty("private", false)
            `object`.addProperty("isDownloadable", "")
            `object`.addProperty("sourcePlanet", user?.planetCode)
            `object`.addProperty("resideOn", user?.planetCode)
            `object`.addProperty("updatedDate", Calendar.getInstance().timeInMillis)
            `object`.addProperty("createdDate", personal.createdDate)
            `object`.addProperty("androidId", NetworkUtils.getUniqueIdentifier())
            `object`.addProperty("deviceName", NetworkUtils.getDeviceName())
            `object`.addProperty("customDeviceName", NetworkUtils.getCustomDeviceName(MainApplication.context))
            return `object`
        }

        fun insertResources(doc: JsonObject, mRealm: Realm) {
            insertMyLibrary("", doc, mRealm)
        }

        @JvmStatic
        fun createStepResource(mRealm: Realm, res: JsonObject, myCoursesID: String?, stepId: String?) {
            insertMyLibrary("", stepId, myCoursesID, res, mRealm)
        }

        @JvmStatic
        fun insertMyLibrary(userId: String?, doc: JsonObject, mRealm: Realm) {
            insertMyLibrary(userId, "", "", doc, mRealm)
        }

        @JvmStatic
        fun createFromResource(resource: RealmMyLibrary, mRealm: Realm, userId: String?) {
            if (!mRealm.isInTransaction) mRealm.beginTransaction()
            resource.setUserId(userId)
            mRealm.commitTransaction()
        }

        @JvmStatic
        fun insertMyLibrary(userId: String?, stepId: String?, courseId: String?, doc: JsonObject, mRealm: Realm) {
            val resourceId = JsonUtils.getString("_id", doc)
            val settings = MainApplication.context.getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE)
            var resource = mRealm.where(RealmMyLibrary::class.java).equalTo("id", resourceId).findFirst()
            if (resource == null) {
                resource = mRealm.createObject(RealmMyLibrary::class.java, resourceId)
            }
            resource!!.setUserId(userId)
            resource.set_id(resourceId)
            if (!TextUtils.isEmpty(stepId)) {
                resource.stepId = stepId
            }
            if (!TextUtils.isEmpty(courseId)) {
                resource.courseId = courseId
            }
            resource.set_rev(JsonUtils.getString("_rev", doc))
            resource.resourceId = resourceId
            resource.title = JsonUtils.getString("title", doc)
            resource.description = JsonUtils.getString("description", doc)
            if (doc.has("_attachments")) {
                val attachments = doc["_attachments"].asJsonObject
                val element = JsonParser.parseString(attachments.toString())
                val obj = element.asJsonObject
                val entries = obj.entrySet()
                for ((key) in entries) {
                    if (key.indexOf("/") < 0) {
                        resource.resourceRemoteAddress = settings.getString("couchdbURL", "http://") + "/resources/" + resourceId + "/" + key
                        resource.resourceLocalAddress = key
                        resource.resourceOffline = FileUtils.checkFileExist(resource.resourceRemoteAddress)
                    }
                }
            }
            resource.filename = JsonUtils.getString("filename", doc)
            resource.averageRating = JsonUtils.getString("averageRating", doc)
            resource.uploadDate = JsonUtils.getString("uploadDate", doc)
            resource.year = JsonUtils.getString("year", doc)
            resource.addedBy = JsonUtils.getString("addedBy", doc)
            resource.setPublisher(JsonUtils.getString("publisher", doc))
            resource.linkToLicense = JsonUtils.getString("linkToLicense", doc)
            resource.openWith = JsonUtils.getString("openWith", doc)
            resource.articleDate = JsonUtils.getString("articleDate", doc)
            resource.kind = JsonUtils.getString("kind", doc)
            resource.createdDate = JsonUtils.getString("createdDate", doc)
            resource.language = JsonUtils.getString("language", doc)
            resource.author = JsonUtils.getString("author", doc)
            resource.mediaType = JsonUtils.getString("mediaType", doc)
            resource.resourceType = JsonUtils.getString("resourceType", doc)
            resource.timesRated = JsonUtils.getInt("timesRated", doc)
            resource.medium = JsonUtils.getString("medium", doc)
            resource.setResourceFor(JsonUtils.getJsonArray("resourceFor", doc), resource)
            resource.setSubject(JsonUtils.getJsonArray("subject", doc), resource)
            resource.setLevel(JsonUtils.getJsonArray("level", doc), resource)
            resource.setTag(JsonUtils.getJsonArray("tags", doc), resource)
            resource.isPrivate = JsonUtils.getBoolean("private", doc)
            resource.setLanguages(JsonUtils.getJsonArray("languages", doc), resource)
        }

        @JvmStatic
        fun getListAsArray(db_myLibrary: RealmResults<RealmMyLibrary>): Array<CharSequence?> {
            val array = arrayOfNulls<CharSequence>(db_myLibrary.size)
            for (i in db_myLibrary.indices) {
                array[i] = db_myLibrary[i]!!.title
            }
            return array
        }

        @JvmStatic
        fun listToString(list: RealmList<String>?): String {
            val s = StringBuilder()
            if (list != null) {
                for (tag in list) {
                    s.append(tag).append(", ")
                }
            }
            return s.toString()
        }

        @JvmStatic
        fun save(allDocs: JsonArray, mRealm: Realm): List<String> {
            val list: MutableList<String> = ArrayList()
            for (i in 0 until allDocs.size()) {
                var doc = allDocs[i].asJsonObject
                doc = JsonUtils.getJsonObject("doc", doc)
                val id = JsonUtils.getString("_id", doc)
                if (!id.startsWith("_design")) {
                    list.add(id)
                    insertResources(doc, mRealm)
                }
            }
            return list
        }

        @JvmStatic
        fun getMyLibIds(realm: Realm?, userId: String?): JsonArray {
            val myLibraries: RealmResults<RealmMyLibrary>? = realm?.where(RealmMyLibrary::class.java)?.contains("userId", userId)?.findAll()
            val ids = JsonArray()
            for (lib in myLibraries ?: emptyList()) {
                ids.add(lib.id)
            }
            return ids
        }
        @JvmStatic
        fun getLevels(libraries: List<RealmMyLibrary>): Set<String> {
            val list: MutableSet<String> = HashSet()
            for (li in libraries) {
                list.addAll(li.level!!)
            }
            return list
        }

        @JvmStatic
        fun getArrayList(libraries: List<RealmMyLibrary>, type: String): Set<String?> {
            val list: MutableSet<String?> = HashSet()
            for (li in libraries) {
                val s = if (type == "mediums") li.mediaType else li.language
                if (!TextUtils.isEmpty(s)) list.add(s)
            }
            return list
        }

        @JvmStatic
        fun getSubjects(libraries: List<RealmMyLibrary>): Set<String> {
            val list: MutableSet<String> = HashSet()
            for (li in libraries) {
                list.addAll(li.subject!!)
            }
            return list
        }
    }
}