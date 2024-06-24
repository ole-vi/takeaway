package org.ole.planet.myplanet.model

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.ole.planet.myplanet.utilities.JsonUtils
import java.util.Date
import java.util.UUID


open class RealmNews : RealmObject() {
    @JvmField
    @PrimaryKey
    var id: String? = null
    @JvmField
    var _id: String? = null
    @JvmField
    var _rev: String? = null
    @JvmField
    var userId: String? = null
    @JvmField
    var user: String? = null
    @JvmField
    var message: String? = null
    @JvmField
    var docType: String? = null
    @JvmField
    var viewableBy: String? = null
    @JvmField
    var viewableId: String? = null
    @JvmField
    var avatar: String? = null
    @JvmField
    var replyTo: String? = null
    @JvmField
    var userName: String? = null
    @JvmField
    var messagePlanetCode: String? = null
    @JvmField
    var messageType: String? = null
    @JvmField
    var updatedDate: Long = 0
    @JvmField
    var time: Long = 0
    @JvmField
    var createdOn: String? = null
    @JvmField
    var parentCode: String? = null
    @JvmField
    var imageUrls: RealmList<String>? = null
    @JvmField
    var images: String? = null
    @JvmField
    var labels: RealmList<String>? = null
    @JvmField
    var viewIn: String? = null
    var news: String? = null
    var newsId: String? = null
    var newsRev: String? = null
    var newsUser: String? = null
    var aiProvider: String? = null
    var newsTitle: String? = null
    var conversations: String? = null
    var newsCreatedDate: Long = 0
    var newsUpdatedDate: Long = 0
    var chat: Boolean = false

    val imagesArray: JsonArray get() = if (images == null) {
        JsonArray()
    } else {
        Gson().fromJson(images, JsonArray::class.java)
    }

    val labelsArray: JsonArray get() {
        val array = JsonArray()
        labels?.forEach{ s ->
            array.add(s)
        }
        return array
    }

    fun addLabel(label: String?) {
        if (label != null && !labels?.contains(label)!!) {
            labels?.add(label)
        }
    }

    fun setLabels(images: JsonArray) {
        labels = RealmList()
        for (ob in images) {
            labels?.add(ob.asString)
        }
    }

    val messageWithoutMarkdown: String? get() {
        var ms = message
        for (ob in imagesArray) {
            ms = ms?.replace(JsonUtils.getString("markdown", ob.asJsonObject), "")
        }
        return ms
    }

    val isCommunityNews: Boolean get() {
        val array = Gson().fromJson(viewIn, JsonArray::class.java)
        var isCommunity = false
        for (e in array) {
            val `object` = e.asJsonObject
            if (`object`.has("section") && `object`["section"].asString.equals("community", ignoreCase = true)) {
                isCommunity = true
                break
            }
        }
        return isCommunity
    }

    companion object {
        @JvmStatic
        fun insert(mRealm: Realm, doc: JsonObject?) {
            if (!mRealm.isInTransaction) {
                mRealm.beginTransaction()
            }
            var news = mRealm.where(RealmNews::class.java)
                .equalTo("_id", JsonUtils.getString("_id", doc))
                .findFirst()
            if (news == null) {
                news = mRealm.createObject(RealmNews::class.java, JsonUtils.getString("_id", doc))
            }
            news?._rev = JsonUtils.getString("_rev", doc)
            news?._id = JsonUtils.getString("_id", doc)
            news?.viewableBy = JsonUtils.getString("viewableBy", doc)
            news?.docType = JsonUtils.getString("docType", doc)
            news?.avatar = JsonUtils.getString("avatar", doc)
            news?.updatedDate = JsonUtils.getLong("updatedDate", doc)
            news?.viewableId = JsonUtils.getString("viewableId", doc)
            news?.createdOn = JsonUtils.getString("createdOn", doc)
            news?.messageType = JsonUtils.getString("messageType", doc)
            news?.messagePlanetCode = JsonUtils.getString("messagePlanetCode", doc)
            news?.replyTo = JsonUtils.getString("replyTo", doc)
            news?.parentCode = JsonUtils.getString("parentCode", doc)
            val user = JsonUtils.getJsonObject("user", doc)
            news?.user = Gson().toJson(JsonUtils.getJsonObject("user", doc))
            news?.userId = JsonUtils.getString("_id", user)
            news?.userName = JsonUtils.getString("name", user)
            news?.time = JsonUtils.getLong("time", doc)
            val images = JsonUtils.getJsonArray("images", doc)
            val message = JsonUtils.getString("message", doc)
            news?.message = message
            news?.images = Gson().toJson(images)
            val labels = JsonUtils.getJsonArray("labels", doc)
            news?.viewIn = Gson().toJson(JsonUtils.getJsonArray("viewIn", doc))
            news?.setLabels(labels)
            news?.chat = JsonUtils.getBoolean("chat", doc)

            val newsObj = JsonUtils.getJsonObject("news", doc)
            news?.newsId = JsonUtils.getString("_id", newsObj)
            news?.newsRev = JsonUtils.getString("_rev", newsObj)
            news?.newsUser = JsonUtils.getString("user", newsObj)
            news?.aiProvider = JsonUtils.getString("aiProvider", newsObj)
            news?.newsTitle = JsonUtils.getString("title", newsObj)
            news?.conversations = Gson().toJson(JsonUtils.getJsonArray("conversations", newsObj))
            news?.newsCreatedDate = JsonUtils.getLong("createdDate", newsObj)
            news?.newsUpdatedDate = JsonUtils.getLong("updatedDate", newsObj)
            mRealm.commitTransaction()
        }

        @JvmStatic
        fun serializeNews(news: RealmNews): JsonObject {
            val `object` = JsonObject()
            `object`.addProperty("chat", news.chat)
            `object`.addProperty("message", news.message)
            if (news._id != null) `object`.addProperty("_id", news._id)
            if (news._rev != null) `object`.addProperty("_rev", news._rev)
            `object`.addProperty("time", news.time)
            `object`.addProperty("createdOn", news.createdOn)
            `object`.addProperty("docType", news.docType)
            addViewIn(`object`, news)
            `object`.addProperty("avatar", news.avatar)
            `object`.addProperty("messageType", news.messageType)
            `object`.addProperty("messagePlanetCode", news.messagePlanetCode)
            `object`.addProperty("createdOn", news.createdOn)
            `object`.addProperty("replyTo", news.replyTo)
            `object`.addProperty("parentCode", news.parentCode)
            `object`.add("images", news.imagesArray)
            `object`.add("labels", news.labelsArray)
            `object`.add("user", Gson().fromJson(news.user, JsonObject::class.java))
            val newsObject = JsonObject()
            newsObject.addProperty("_id", news.newsId)
            newsObject.addProperty("_rev", news.newsRev)
            newsObject.addProperty("user", news.newsUser)
            newsObject.addProperty("aiProvider", news.aiProvider)
            newsObject.addProperty("title", news.newsTitle)
            newsObject.add("conversations", Gson().fromJson(news.conversations, JsonArray::class.java))
            newsObject.addProperty("createdDate", news.newsCreatedDate)
            newsObject.addProperty("updatedDate", news.newsUpdatedDate)
            `object`.add("news", newsObject)
            return `object`
        }

        private fun addViewIn(`object`: JsonObject, news: RealmNews) {
            if (!TextUtils.isEmpty(news.viewableId)) {
                `object`.addProperty("viewableId", news.viewableId)
                `object`.addProperty("viewableBy", news.viewableBy)
            }
            if (!TextUtils.isEmpty(news.viewIn)) {
                val ar = Gson().fromJson(news.viewIn, JsonArray::class.java)
                if (ar.size() > 0) `object`.add("viewIn", ar)
            }
        }

        @JvmStatic
        fun createNews(map: HashMap<String?, String>, mRealm: Realm, user: RealmUserModel?, imageUrls: RealmList<String>?): RealmNews {
            if (!mRealm.isInTransaction) {
                mRealm.beginTransaction()
            }

            val news = mRealm.createObject(RealmNews::class.java, "${UUID.randomUUID()}")
            news.message = map["message"]
            news.time = Date().time
            news.createdOn = user?.planetCode
            news.avatar = ""
            news.docType = "message"
            news.userName = user?.name
            news.parentCode = user?.parentCode
            news.messagePlanetCode = map["messagePlanetCode"]
            news.messageType = map["messageType"]
            news.viewIn = getViewInJson(map)
            news.chat = map["chat"]?.toBoolean() ?: false

            try {
                news.updatedDate = map["updatedDate"]?.toLong() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            }

            news.userId = user?.id
            news.replyTo = map["replyTo"] ?: ""
            news.user = Gson().toJson(user?.serialize())
            news.imageUrls = imageUrls

            if (map.containsKey("news")) {
                val newsObj = map["news"]
                val gson = Gson()
                try {
                    val newsJsonString = newsObj?.replace("=", ":")
                    val newsJson = gson.fromJson(newsJsonString, JsonObject::class.java)
                    news.newsId = JsonUtils.getString("_id", newsJson)
                    news.newsRev = JsonUtils.getString("_rev", newsJson)
                    news.newsUser = JsonUtils.getString("user", newsJson)
                    news.aiProvider = JsonUtils.getString("aiProvider", newsJson)
                    news.newsTitle = JsonUtils.getString("title", newsJson)
                    if (newsJson.has("conversations")) {
                        val conversationsElement = newsJson.get("conversations")
                        if (conversationsElement.isJsonPrimitive && conversationsElement.asJsonPrimitive.isString) {
                            val conversationsString = conversationsElement.asString
                            try {
                                val conversationsArray = gson.fromJson(conversationsString, JsonArray::class.java)
                                if (conversationsArray.size() > 0) {
                                    val conversationsList = ArrayList<HashMap<String, String>>()
                                    conversationsArray.forEach { conversationElement ->
                                        val conversationObj = conversationElement.asJsonObject
                                        val conversationMap = HashMap<String, String>()
                                        conversationMap["query"] = conversationObj.get("query").asString
                                        conversationMap["response"] = conversationObj.get("response").asString
                                        conversationsList.add(conversationMap)
                                    }
                                    news.conversations = Gson().toJson(conversationsList)
                                }
                            } catch (e: JsonSyntaxException) {
                                e.printStackTrace()
                            }
                        }
                    }
                    news.newsCreatedDate = JsonUtils.getLong("createdDate", newsJson)
                    news.newsUpdatedDate = JsonUtils.getLong("updatedDate", newsJson)
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace()
                }
            }

            mRealm.commitTransaction()
            return news
        }

        @JvmStatic
        fun getViewInJson(map: HashMap<String?, String>): String {
            val viewInArray = JsonArray()
            if (!TextUtils.isEmpty(map["viewInId"])) {
                val `object` = JsonObject()
                `object`.addProperty("_id", map["viewInId"])
                `object`.addProperty("section", map["viewInSection"])
                viewInArray.add(`object`)
            }
            return Gson().toJson(viewInArray)
        }
    }
}
