package org.ole.planet.myplanet.ui.community

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentTeamDetailBinding
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.TimeUtils
import java.util.Date

class HomeCommunityDialogFragment : BottomSheetDialogFragment() {
    private lateinit var fragmentTeamDetailBinding: FragmentTeamDetailBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentTeamDetailBinding = FragmentTeamDetailBinding.inflate(inflater, container, false)
        return fragmentTeamDetailBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCommunityTab()
    }

    private fun initCommunityTab() {
        fragmentTeamDetailBinding.llActionButtons.visibility = View.GONE
        val settings = requireActivity().getSharedPreferences(SyncActivity.PREFS_NAME, MODE_PRIVATE)
        val sPlanetcode = settings.getString("planetCode", "")
        val sParentcode = settings.getString("parentCode", "")
        val user = UserProfileDbHandler(requireActivity()).userModel
        fragmentTeamDetailBinding.viewPager2?.adapter = CommunityPagerAdapter(requireActivity(), user?.planetCode + "@" + sParentcode, true)
        fragmentTeamDetailBinding.viewPager2?.let {
            TabLayoutMediator(fragmentTeamDetailBinding.tabLayout, it) { tab, position ->
                tab.text = (fragmentTeamDetailBinding.viewPager2?.adapter as CommunityPagerAdapter).getPageTitle(position)
            }.attach()
        }

        fragmentTeamDetailBinding.title.text = user?.planetCode
        fragmentTeamDetailBinding.title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_black_1000))
        fragmentTeamDetailBinding.subtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_black_1000))
        fragmentTeamDetailBinding.subtitle.text = TimeUtils.getFormatedDateWithTime(Date().time)
    }
}