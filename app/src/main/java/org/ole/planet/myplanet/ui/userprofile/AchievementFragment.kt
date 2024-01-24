package org.ole.planet.myplanet.ui.userprofile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.realm.Realm
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.base.BaseContainerFragment
import org.ole.planet.myplanet.callback.OnHomeItemClickListener
import org.ole.planet.myplanet.databinding.FragmentAchievementBinding
import org.ole.planet.myplanet.databinding.LayoutButtonPrimaryBinding
import org.ole.planet.myplanet.databinding.RowAchievementBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmAchievement
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.JsonUtils.getString
import org.ole.planet.myplanet.utilities.Utilities

class AchievementFragment : BaseContainerFragment() {
    private lateinit var fragmentAchievementBinding: FragmentAchievementBinding
    private var rowAchievementBinding: RowAchievementBinding? = null
    private var layoutButtonPrimaryBinding: LayoutButtonPrimaryBinding? = null
    var mRealm: Realm? = null
    var user: RealmUserModel? = null
    var listener: OnHomeItemClickListener? = null
    private var achievement: RealmAchievement? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnHomeItemClickListener) listener = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentAchievementBinding = FragmentAchievementBinding.inflate(inflater, container, false)
        mRealm = DatabaseService(MainApplication.context).realmInstance
        user = UserProfileDbHandler(MainApplication.context).userModel
        fragmentAchievementBinding.btnEdit.setOnClickListener {
            if (listener != null) listener!!.openCallFragment(EditAchievementFragment())
        }
        return fragmentAchievementBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        achievement = mRealm.where(RealmAchievement::class.java).equalTo("_id", user!!.id + "@" + user!!.planetCode).findFirst()
        fragmentAchievementBinding.tvFirstName.text = user!!.firstName
        fragmentAchievementBinding.tvName.text = String.format("%s %s %s", user!!.firstName, user!!.middleName, user!!.lastName)
        if (achievement != null) {
            fragmentAchievementBinding.tvGoals.text = achievement!!.goals
            fragmentAchievementBinding.tvPurpose.text = achievement!!.purpose
            fragmentAchievementBinding.tvAchievementHeader.text = achievement!!.achievementsHeader
            fragmentAchievementBinding.llAchievement.removeAllViews()
            for (s in achievement!!.achievements!!) {
                rowAchievementBinding = RowAchievementBinding.inflate(LayoutInflater.from(MainApplication.context))
                val ob = Gson().fromJson(s, JsonElement::class.java)
                if (ob is JsonObject) {
                    rowAchievementBinding!!.tvDescription.text = getString("description", ob.getAsJsonObject())
                    rowAchievementBinding!!.tvDate.text = getString("date", ob.getAsJsonObject())
                    rowAchievementBinding!!.tvTitle.text = getString("title", ob.getAsJsonObject())
                    val libraries = getList(ob.getAsJsonArray("resources"))
                    if (getString("description", ob.getAsJsonObject()).isNotEmpty() && libraries.size > 0) {
                        rowAchievementBinding!!.llRow.setOnClickListener {
                            rowAchievementBinding!!.llDesc.visibility = if (rowAchievementBinding!!.llDesc.visibility == View.GONE) View.VISIBLE else View.GONE
                            rowAchievementBinding!!.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0,
                                if (rowAchievementBinding!!.llDesc.visibility == View.GONE) R.drawable.ic_down
                                else R.drawable.ic_up, 0)
                        }
                        for (lib in libraries) {
                            layoutButtonPrimaryBinding = LayoutButtonPrimaryBinding.inflate(LayoutInflater.from(MainApplication.context))
                            layoutButtonPrimaryBinding!!.root.text = lib.title
                            layoutButtonPrimaryBinding!!.root.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                if (lib.isResourceOffline()) R.drawable.ic_eye
                                else R.drawable.ic_download, 0)
                            layoutButtonPrimaryBinding!!.root.setOnClickListener {
                                if (lib.isResourceOffline()) {
                                    openResource(lib)
                                } else {
                                    val a = ArrayList<String>()
                                    a.add(Utilities.getUrl(lib, settings))
                                    startDownload(a)
                                }
                            }
                            rowAchievementBinding!!.flexboxResources.addView(layoutButtonPrimaryBinding!!.root)
                        }
                    } else {
                        rowAchievementBinding!!.tvTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        createAchievementList()
                        fragmentAchievementBinding.rvOtherInfo.layoutManager = LinearLayoutManager(MainApplication.context)
                        fragmentAchievementBinding.rvOtherInfo.adapter = achievement!!.getreferences()?.let { AdapterOtherInfo(MainApplication.context, it) }
                    }
                    mRealm.addChangeListener {
                        if (fragmentAchievementBinding.llAchievement != null) fragmentAchievementBinding.llAchievement.removeAllViews()
                        createAchievementList()
                    }
                } else {
                    rowAchievementBinding!!.root.visibility = View.GONE
                }
                fragmentAchievementBinding.llAchievement.addView(rowAchievementBinding!!.root)
            }
            fragmentAchievementBinding.rvOtherInfo.layoutManager = LinearLayoutManager(MainApplication.context)
            fragmentAchievementBinding.rvOtherInfo.adapter = achievement!!.getreferences()?.let { AdapterOtherInfo(MainApplication.context, it) }
        }
    }

    private fun createAchievementList() {
        for (s in achievement!!.achievements!!) {
            rowAchievementBinding = RowAchievementBinding.inflate(LayoutInflater.from(MainApplication.context))
            val ob = Gson().fromJson(s, JsonElement::class.java)
            if (ob is JsonObject) {
                rowAchievementBinding!!.tvDescription.text = getString("description", ob.getAsJsonObject())
                rowAchievementBinding!!.tvDate.text = getString("date", ob.getAsJsonObject())
                rowAchievementBinding!!.tvTitle.text = getString("title", ob.getAsJsonObject())
                val libraries = getList(ob.getAsJsonArray("resources"))
                rowAchievementBinding!!.llRow.setOnClickListener {
                    rowAchievementBinding!!.llDesc.visibility = if (rowAchievementBinding!!.llDesc.visibility == View.GONE) View.VISIBLE else View.GONE
                    rowAchievementBinding!!.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        if (rowAchievementBinding!!.llDesc.visibility == View.GONE) R.drawable.ic_down else R.drawable.ic_up, 0
                    )
                }
                for (lib in libraries) {
                    layoutButtonPrimaryBinding = LayoutButtonPrimaryBinding.inflate(LayoutInflater.from(MainApplication.context))
                    layoutButtonPrimaryBinding!!.root.text = lib.title
                    layoutButtonPrimaryBinding!!.root.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        if (lib.isResourceOffline()) R.drawable.ic_eye else R.drawable.ic_download, 0
                    )
                    layoutButtonPrimaryBinding!!.root.setOnClickListener {
                        if (lib.isResourceOffline()) {
                            openResource(lib)
                        } else {
                            val a = ArrayList<String>()
                            a.add(Utilities.getUrl(lib, settings))
                            startDownload(a)
                        }
                    }
                    rowAchievementBinding!!.flexboxResources.addView(layoutButtonPrimaryBinding!!.root)
                }
            } else {
                rowAchievementBinding!!.root.visibility = View.GONE
            }
            fragmentAchievementBinding.llAchievement.addView(rowAchievementBinding!!.root)
        }
    }

    private fun getList(array: JsonArray): ArrayList<RealmMyLibrary> {
        val libraries = ArrayList<RealmMyLibrary>()
        for (e in array) {
            val id = e.asJsonObject["_id"].asString
            val li = mRealm.where(RealmMyLibrary::class.java).equalTo("id", id).findFirst()
            if (li != null) libraries.add(li)
        }
        return libraries
    }
}