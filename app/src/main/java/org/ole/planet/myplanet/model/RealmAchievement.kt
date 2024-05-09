package org.ole.planet.myplanet.model

import android.text.TextUtils
import android.widget.EditText
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.ole.planet.myplanet.utilities.JsonUtils

open class RealmAchievement : RealmObject() {
    @JvmField
    var achievements: RealmList<String>? = null
    @JvmField
    var references: RealmList<String>? = null
    @JvmField
    var purpose: String? = null
    @JvmField
    var achievementsHeader: String? = null
    @JvmField
    var sendToNation: String? = null
    @JvmField
    var _rev: String? = null
    @PrimaryKey
    @JvmField
    var _id: String? = null
    @JvmField
    var goals: String? = null

    fun getreferences(): RealmList<String>? {
        return references
    }

    val achievementsArray: JsonArray
        get() {
            val array = JsonArray()
            for (s in achievements ?: emptyList()) {
                val ob = Gson().fromJson(s, JsonElement::class.java)
                array.add(ob)
            }
            return array
        }

    fun getreferencesArray(): JsonArray {
        val array = JsonArray()
        for (s in references ?: emptyList()) {
            val ob = Gson().fromJson(s, JsonElement::class.java)
            array.add(ob)
        }
        return array
    }

    fun setAchievements(ac: JsonArray) {
        achievements = RealmList()
        for (el in ac) {
            val achi = Gson().toJson(el)
            if (!achievements?.contains(achi)!!) {
                achievements?.add(achi)
            }
        }
    }

    fun setreferences(of: JsonArray?) {
        references = RealmList()
        if (of == null) return
        for (el in of) {
            val e = Gson().toJson(el)
            if (!references?.contains(e)!!) {
                references?.add(e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun serialize(sub: RealmAchievement): JsonObject {
            val `object` = JsonObject()
            `object`.addProperty("_id", sub._id)
            if (!TextUtils.isEmpty(sub._rev)) `object`.addProperty("_rev", sub._rev)
            `object`.addProperty("goals", sub.goals)
            `object`.addProperty("purpose", sub.purpose)
            `object`.addProperty("achievementsHeader", sub.achievementsHeader)
            `object`.add("references", sub.getreferencesArray())
            `object`.add("achievements", sub.achievementsArray)
            return `object`
        }

        @JvmStatic
        fun createReference(name: String?, relation: EditText, phone: EditText, email: EditText): JsonObject {
            val ob = JsonObject()
            ob.addProperty("name", name)
            ob.addProperty("phone", phone.text.toString())
            ob.addProperty("relationship", relation.text.toString())
            ob.addProperty("email", email.text.toString())
            return ob
        }

        @JvmStatic
        fun insert(mRealm: Realm, act: JsonObject?) {
            var achievement = mRealm.where(RealmAchievement::class.java)
                .equalTo("_id", JsonUtils.getString("_id", act)).findFirst()
            if (achievement == null) achievement = mRealm.createObject(
                RealmAchievement::class.java, JsonUtils.getString("_id", act)
            )
            achievement?._rev = JsonUtils.getString("_rev", act)
            achievement?.purpose = JsonUtils.getString("purpose", act)
            achievement?.goals = JsonUtils.getString("goals", act)
            achievement?.achievementsHeader = JsonUtils.getString("achievementsHeader", act)
            achievement?.setreferences(JsonUtils.getJsonArray("references", act))
            achievement?.setAchievements(JsonUtils.getJsonArray("achievements", act))
        }
    }
}