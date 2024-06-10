package org.ole.planet.myplanet.ui.courses

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import io.realm.Realm
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentTakeCourseBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmCourseActivity.Companion.createActivity
import org.ole.planet.myplanet.model.RealmCourseProgress.Companion.getCurrentProgress
import org.ole.planet.myplanet.model.RealmCourseStep
import org.ole.planet.myplanet.model.RealmMyCourse
import org.ole.planet.myplanet.model.RealmMyCourse.Companion.getCourseStepIds
import org.ole.planet.myplanet.model.RealmMyCourse.Companion.getCourseSteps
import org.ole.planet.myplanet.model.RealmRemovedLog.Companion.onAdd
import org.ole.planet.myplanet.model.RealmRemovedLog.Companion.onRemove
import org.ole.planet.myplanet.model.RealmSubmission.Companion.isStepCompleted
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.Constants.showBetaFeature
import org.ole.planet.myplanet.utilities.DialogUtils.getAlertDialog
import org.ole.planet.myplanet.utilities.Utilities

class TakeCourseFragment : Fragment(), ViewPager.OnPageChangeListener, View.OnClickListener {
    private lateinit var fragmentTakeCourseBinding: FragmentTakeCourseBinding
    lateinit var dbService: DatabaseService
    lateinit var mRealm: Realm
    var courseId: String? = null
    private var currentCourse: RealmMyCourse? = null
    lateinit var steps: List<RealmCourseStep?>
    var userModel: RealmUserModel ?= null
    var position = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            courseId = requireArguments().getString("id")
            if (requireArguments().containsKey("position")) {
                position = requireArguments().getInt("position")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentTakeCourseBinding = FragmentTakeCourseBinding.inflate(inflater, container, false)
        dbService = DatabaseService(requireActivity())
        mRealm = dbService.realmInstance
        userModel = UserProfileDbHandler(requireContext()).userModel
        currentCourse = mRealm.where(RealmMyCourse::class.java).equalTo("courseId", courseId).findFirst()
        return fragmentTakeCourseBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentTakeCourseBinding.tvCourseTitle.text = currentCourse?.courseTitle
        steps = getCourseSteps(mRealm, courseId)
        if (steps.isEmpty()) {
            fragmentTakeCourseBinding.nextStep.visibility = View.GONE
            fragmentTakeCourseBinding.previousStep.visibility = View.GONE
        }
        fragmentTakeCourseBinding.viewPager2.adapter = CoursesPagerAdapter(this, courseId, getCourseStepIds(mRealm, courseId))
        fragmentTakeCourseBinding.viewPager2.isUserInputEnabled = false
        if (fragmentTakeCourseBinding.viewPager2.currentItem == 0) {
            fragmentTakeCourseBinding.previousStep.visibility = View.GONE
        }
        setCourseData()
        setListeners()
        fragmentTakeCourseBinding.viewPager2.currentItem = position
    }

    private fun setListeners() {
        fragmentTakeCourseBinding.nextStep.setOnClickListener(this)
        fragmentTakeCourseBinding.previousStep.setOnClickListener(this)
        fragmentTakeCourseBinding.btnRemove.setOnClickListener(this)
        fragmentTakeCourseBinding.finishStep.setOnClickListener(this)
        fragmentTakeCourseBinding.courseProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val currentProgress = getCurrentProgress(steps, mRealm, userModel?.id, courseId)
                if (b && i <= currentProgress + 1) {
                    fragmentTakeCourseBinding.viewPager2.currentItem = i
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setCourseData() {
        if (userModel?.isGuest() != true && currentCourse?.userId?.contains(userModel?.id) != true) {
            fragmentTakeCourseBinding.btnRemove.visibility = View.VISIBLE
            fragmentTakeCourseBinding.btnRemove.text = getString(R.string.join)
            getAlertDialog(requireActivity(), getString(R.string.do_you_want_to_join_this_course), getString(R.string.join_this_course)) { _: DialogInterface?, _: Int -> addRemoveCourse() }
        } else {
            fragmentTakeCourseBinding.btnRemove.visibility = View.GONE
        }
        createActivity(mRealm, userModel, currentCourse)
        fragmentTakeCourseBinding.tvStep.text = String.format("Step %d/%d", fragmentTakeCourseBinding.viewPager2.currentItem, currentCourse?.courseSteps?.size)
        fragmentTakeCourseBinding.courseProgress.max = steps.size
        val i = getCurrentProgress(steps, mRealm, userModel?.id, courseId)
        if (i < steps.size) fragmentTakeCourseBinding.courseProgress.secondaryProgress = i + 1
        fragmentTakeCourseBinding.courseProgress.progress = i
        if (currentCourse?.userId?.contains(userModel?.id) == true) {
            fragmentTakeCourseBinding.nextStep.visibility = View.VISIBLE
            fragmentTakeCourseBinding.courseProgress.visibility = View.VISIBLE
        } else {
            fragmentTakeCourseBinding.nextStep.visibility = View.GONE
            fragmentTakeCourseBinding.previousStep.visibility = View.GONE
            fragmentTakeCourseBinding.courseProgress.visibility = View.GONE
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        if (position > 0) {
            if (position - 1 < steps.size) changeNextButtonState(position)
        } else {
            fragmentTakeCourseBinding.nextStep.isClickable = true
            fragmentTakeCourseBinding.nextStep.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_white_1000))
        }
        val i = getCurrentProgress(steps, mRealm, userModel?.id, courseId)
        if (i < steps.size) fragmentTakeCourseBinding.courseProgress.secondaryProgress = i + 1
        fragmentTakeCourseBinding.courseProgress.progress = i
        fragmentTakeCourseBinding.tvStep.text = String.format("Step %d/%d", position, steps.size)
    }

    private fun changeNextButtonState(position: Int) {
        if (isStepCompleted(mRealm, steps[position - 1]?.id, userModel?.id) || !showBetaFeature(Constants.KEY_EXAM, requireContext())) {
            fragmentTakeCourseBinding.nextStep.isClickable = true
            fragmentTakeCourseBinding.nextStep.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_white_1000))
        } else {
            fragmentTakeCourseBinding.nextStep.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_grey_500))
            fragmentTakeCourseBinding.nextStep.isClickable = false
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    private fun onClickNext() {
        fragmentTakeCourseBinding.tvStep.text = String.format("Step %d/%d", fragmentTakeCourseBinding.viewPager2.currentItem, currentCourse?.courseSteps?.size)
        if (fragmentTakeCourseBinding.viewPager2.currentItem == currentCourse?.courseSteps?.size) {
            fragmentTakeCourseBinding.nextStep.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_grey_500))
            fragmentTakeCourseBinding.nextStep.visibility = View.GONE
            fragmentTakeCourseBinding.finishStep.visibility = View.VISIBLE

        }
    }

    private fun onClickPrevious() {
        fragmentTakeCourseBinding.tvStep.text = String.format("Step %d/%d", fragmentTakeCourseBinding.viewPager2.currentItem - 1, currentCourse?.courseSteps?.size)
        if (fragmentTakeCourseBinding.viewPager2.currentItem - 1 == 0) {
            fragmentTakeCourseBinding.previousStep.visibility = View.GONE
            fragmentTakeCourseBinding.nextStep.visibility = View.VISIBLE
            fragmentTakeCourseBinding.finishStep.visibility = View.GONE
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.next_step -> {
                if (isValidClickRight) {
                    fragmentTakeCourseBinding.viewPager2.currentItem += 1
                    fragmentTakeCourseBinding.previousStep.visibility = View.VISIBLE
                }
                onClickNext()
            }

            R.id.previous_step -> {
                onClickPrevious()
                if (isValidClickLeft) {
                    fragmentTakeCourseBinding.viewPager2.currentItem -= 1
                }
            }

            R.id.finish_step -> requireActivity().supportFragmentManager.popBackStack()
            R.id.btn_remove -> addRemoveCourse()
        }
    }

    private fun addRemoveCourse() {
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        if (currentCourse?.userId?.contains(userModel?.id) == true) {
            currentCourse?.removeUserId(userModel?.id)
            onRemove(mRealm, "courses", userModel?.id, courseId)
        } else {
            currentCourse?.setUserId(userModel?.id)
            onAdd(mRealm, "courses", userModel?.id, courseId)
        }
        Utilities.toast(activity, "Course ${(if (currentCourse?.userId?.contains(userModel?.id) == true) { 
            getString(R.string.added_to) 
        } else {
            getString(R.string.removed_from)
        })} ${getString(R.string.my_courses)}")
        setCourseData()
    }

    private val isValidClickRight: Boolean
        get() = fragmentTakeCourseBinding.viewPager2.adapter != null && fragmentTakeCourseBinding.viewPager2.currentItem < fragmentTakeCourseBinding.viewPager2.adapter?.itemCount!!
    private val isValidClickLeft: Boolean
        get() = fragmentTakeCourseBinding.viewPager2.adapter != null && fragmentTakeCourseBinding.viewPager2.currentItem > 0

    companion object {
        @JvmStatic
        fun newInstance(b: Bundle?): TakeCourseFragment {
            val takeCourseFragment = TakeCourseFragment()
            takeCourseFragment.arguments = b
            return takeCourseFragment
        }
    }

    fun joinCourse() {
        fragmentTakeCourseBinding.nextStep.visibility = View.VISIBLE
        fragmentTakeCourseBinding.courseProgress.visibility = View.VISIBLE
        fragmentTakeCourseBinding.btnRemove.visibility = View.GONE
    }
}