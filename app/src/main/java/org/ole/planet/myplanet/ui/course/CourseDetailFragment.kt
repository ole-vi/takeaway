package org.ole.planet.myplanet.ui.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import io.realm.RealmResults
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.base.BaseContainerFragment
import org.ole.planet.myplanet.callback.OnRatingChangeListener
import org.ole.planet.myplanet.databinding.FragmentCourseDetailBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmCourseStep.Companion.getSteps
import org.ole.planet.myplanet.model.RealmMyCourse
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmRating.Companion.getRatingsById
import org.ole.planet.myplanet.model.RealmStepExam.Companion.getNoOfExam
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.Markdown.setMarkdownText

class CourseDetailFragment : BaseContainerFragment(), OnRatingChangeListener {
    private var fragmentCourseDetailBinding: FragmentCourseDetailBinding? = null
    var dbService: DatabaseService? = null
    var mRealm: Realm? = null
    var courses: RealmMyCourse? = null
    var user: RealmUserModel? = null
    var id: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            id = requireArguments().getString("courseId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentCourseDetailBinding = FragmentCourseDetailBinding.inflate(inflater, container, false)
        dbService = DatabaseService(requireActivity())
        mRealm = dbService!!.realmInstance
        courses = mRealm.where(RealmMyCourse::class.java).equalTo("courseId", id).findFirst()
        user = UserProfileDbHandler(activity).userModel
        return fragmentCourseDetailBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRatingView("course", courses!!.courseId, courses!!.courseTitle, this)
        setCourseData()
    }

    private fun setCourseData() {
        setTextViewVisibility(fragmentCourseDetailBinding!!.subjectLevel, courses!!.subjectLevel, fragmentCourseDetailBinding!!.ltSubjectLevel)
        setTextViewVisibility(fragmentCourseDetailBinding!!.method, courses!!.method, fragmentCourseDetailBinding!!.ltMethod)
        setTextViewVisibility(fragmentCourseDetailBinding!!.gradeLevel, courses!!.gradeLevel, fragmentCourseDetailBinding!!.ltGradeLevel)
        setTextViewVisibility(fragmentCourseDetailBinding!!.language, courses!!.languageOfInstruction, fragmentCourseDetailBinding!!.ltLanguage)
        val markdownContentWithLocalPaths = CourseStepFragment.prependBaseUrlToImages(courses!!.description, "file://" + MainApplication.context.getExternalFilesDir(null) + "/ole/")
        setMarkdownText(fragmentCourseDetailBinding!!.description, markdownContentWithLocalPaths)
        fragmentCourseDetailBinding!!.noOfExams.text = getNoOfExam(mRealm, id).toString() + ""
        val resources: RealmResults<*> = mRealm.where(RealmMyLibrary::class.java).equalTo("courseId", id).equalTo("resourceOffline", false).isNotNull("resourceLocalAddress").findAll()
        setResourceButton(resources, fragmentCourseDetailBinding!!.btnResources)
        val downloadedResources: List<RealmMyLibrary> = mRealm.where(RealmMyLibrary::class.java).equalTo("resourceOffline", true).equalTo("courseId", id).isNotNull("resourceLocalAddress").findAll()
        setOpenResourceButton(downloadedResources, fragmentCourseDetailBinding!!.btnOpen)
        onRatingChanged()
        setStepsList()
    }

    private fun setTextViewVisibility(textView: TextView, content: String?, layout: View) {
        if (content!!.isEmpty()) {
            layout.visibility = View.GONE
        } else {
            textView.text = content
        }
    }

    private fun setStepsList() {
        val steps = getSteps(mRealm, courses!!.courseId)
        fragmentCourseDetailBinding!!.stepsList.layoutManager = LinearLayoutManager(activity)
        fragmentCourseDetailBinding!!.stepsList.adapter = AdapterSteps(requireActivity(), steps, mRealm)
    }

    override fun onRatingChanged() {
        val `object` = getRatingsById(mRealm, "course", courses!!.courseId, user!!.id)
        setRatings(`object`)
    }

    override fun onDownloadComplete() {
        super.onDownloadComplete()
        setCourseData()
    }
}
