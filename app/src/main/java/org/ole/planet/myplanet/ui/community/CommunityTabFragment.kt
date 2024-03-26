package org.ole.planet.myplanet.ui.community

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import org.ole.planet.myplanet.databinding.FragmentTeamDetailBinding
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.TimeUtils
import java.util.Date

class CommunityTabFragment : Fragment() {
    private lateinit var fragmentTeamDetailBinding: FragmentTeamDetailBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentTeamDetailBinding = FragmentTeamDetailBinding.inflate(inflater, container, false)
        return fragmentTeamDetailBinding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = requireActivity().getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val sParentcode = settings.getString("parentCode", "")
        val user = UserProfileDbHandler(requireActivity()).userModel
        fragmentTeamDetailBinding.viewPager2?.adapter = CommunityPagerAdapter(requireActivity(), user?.planetCode + "@" + sParentcode, false)
        fragmentTeamDetailBinding.viewPager2?.let {
            TabLayoutMediator(fragmentTeamDetailBinding.tabLayout, it) { tab, position ->
                tab.text = (fragmentTeamDetailBinding.viewPager2?.adapter as CommunityPagerAdapter).getPageTitle(position)
            }.attach()
        }
        fragmentTeamDetailBinding.title.text = user?.planetCode
        fragmentTeamDetailBinding.subtitle.text = TimeUtils.getFormatedDateWithTime(Date().time)
        fragmentTeamDetailBinding.llActionButtons.visibility = View.GONE
    }
}