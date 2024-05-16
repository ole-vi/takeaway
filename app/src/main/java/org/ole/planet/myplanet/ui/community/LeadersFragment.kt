package org.ole.planet.myplanet.ui.community

import android.content.*
import android.os.Bundle
//import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import org.json.*
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentMembersBinding
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME

class LeadersFragment : Fragment() {
    private lateinit var fragmentMembersBinding: FragmentMembersBinding
    lateinit var settings: SharedPreferences
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentMembersBinding = FragmentMembersBinding.inflate(inflater, container, false)
        settings = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return fragmentMembersBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val leaders = settings.getString("communityLeaders", "")
        if (leaders.isNullOrEmpty()) {
            fragmentMembersBinding.tvNodata.text = getString(R.string.no_data_available)
        } else {
            val leadersList = parseLeadersJson(leaders)
            fragmentMembersBinding.rvMember.layoutManager = GridLayoutManager(activity, 2)
            fragmentMembersBinding.rvMember.adapter = AdapterLeader(requireActivity(), leadersList)
        }
    }

    private fun parseLeadersJson(jsonString: String): List<RealmUserModel> {
        val leadersList = mutableListOf<RealmUserModel>()
        try {
            val jsonObject = JSONObject(jsonString)
            val docsArray = jsonObject.getJSONArray("docs")
            for (i in 0 until docsArray.length()) {
                val docObject = docsArray.getJSONObject(i)
                val user = RealmUserModel()
                user.name = docObject.getString("name")
                if (!docObject.isNull("firstName")) {
                    user.firstName = docObject.getString("firstName")
                }
                if (!docObject.isNull("lastName")) {
                    user.lastName = docObject.getString("lastName")
                }
                if (!docObject.isNull("email")) {
                    user.email = docObject.getString("email")
                }
                leadersList.add(user)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return leadersList
    }
}