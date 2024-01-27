package org.ole.planet.myplanet.ui.dashboard

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Typeface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import io.realm.Case
import io.realm.RealmObject
import io.realm.Sort
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.callback.NotificationCallback
import org.ole.planet.myplanet.callback.SyncListener
import org.ole.planet.myplanet.databinding.AlertHealthListBinding
import org.ole.planet.myplanet.databinding.ItemLibraryHomeBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMeetup
import org.ole.planet.myplanet.model.RealmMyCourse
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmMyLife
import org.ole.planet.myplanet.model.RealmMyTeam
import org.ole.planet.myplanet.model.RealmNews
import org.ole.planet.myplanet.model.RealmTeamNotification
import org.ole.planet.myplanet.model.RealmTeamTask
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.TransactionSyncManager
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.dashboard.notification.NotificationFragment
import org.ole.planet.myplanet.ui.exam.UserInformationFragment
import org.ole.planet.myplanet.ui.myhealth.UserListArrayAdapter
import org.ole.planet.myplanet.ui.team.TeamDetailFragment
import org.ole.planet.myplanet.ui.userprofile.BecomeMemberActivity
import org.ole.planet.myplanet.ui.userprofile.UserProfileFragment
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.FileUtils
import org.ole.planet.myplanet.utilities.Utilities
import java.util.Calendar
import java.util.UUID

open class BaseDashboardFragment : BaseDashboardFragmentPlugin(), NotificationCallback,
    SyncListener {
    private var fullName: String? = null
    lateinit var dbService: DatabaseService
    private var params = LinearLayout.LayoutParams(250, 100)
    private var di: ProgressDialog? = null

    fun onLoaded(v: View) {
        profileDbHandler = UserProfileDbHandler(activity)
        model = profileDbHandler.userModel
        fullName = profileDbHandler.userModel.getFullName()
        if (fullName?.trim().isNullOrBlank()) {
            fullName = profileDbHandler.userModel.name

            v.findViewById<LinearLayout>(R.id.ll_prompt).visibility = View.VISIBLE
            v.findViewById<LinearLayout>(R.id.ll_prompt).setOnClickListener {
                UserInformationFragment.getInstance("").show(childFragmentManager, "")
            }
        } else {
            v.findViewById<LinearLayout>(R.id.ll_prompt).visibility = View.GONE
        }
        v.findViewById<ImageView>(R.id.ic_close).setOnClickListener {
            v.findViewById<LinearLayout>(R.id.ll_prompt).visibility = View.GONE
        }
        val imageView = v.findViewById<ImageView>(R.id.imageView)
        if (!TextUtils.isEmpty(model.userImage)) {
            Glide.with(requireActivity())
                .load(model.userImage)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.profile)
        }

        v.findViewById<TextView>(R.id.txtVisits).text = "${profileDbHandler.offlineVisits} ${getString(R.string.visits)}"
        v.findViewById<TextView>(R.id.txtRole).text = "- ${model.getRoleAsString()}"
        v.findViewById<TextView>(R.id.txtFullName).text = fullName
    }

    override fun forceDownloadNewsImages() {
        mRealm = DatabaseService(requireContext()).realmInstance
        Utilities.toast(activity, getString(R.string.please_select_starting_date))
        val now = Calendar.getInstance()
        val dpd = DatePickerDialog(
            requireActivity(), { _: DatePicker?, i: Int, i1: Int, i2: Int ->
                now[Calendar.YEAR] = i
                now[Calendar.MONTH] = i1
                now[Calendar.DAY_OF_MONTH] = i2
                val imageList: List<RealmMyLibrary> =
                    mRealm.where(RealmMyLibrary::class.java).equalTo("isPrivate", true)
                        .greaterThan("createdDate", now.timeInMillis).equalTo("mediaType", "image")
                        .findAll()
                val urls = ArrayList<String>()
                getUrlsAndStartDownload(
                    imageList, settings, urls as ArrayList<String?>
                )
            }, now[Calendar.YEAR], now[Calendar.MONTH], now[Calendar.DAY_OF_MONTH]
        )
        dpd.setTitle(getString(R.string.read_offline_news_from))
        dpd.show()
    }

    override fun downloadDictionary() {
        val list = ArrayList<String>()
        list.add(Constants.DICTIONARY_URL)
        if (!FileUtils.checkFileExist(Constants.DICTIONARY_URL)) {
            Utilities.toast(activity, getString(R.string.downloading_started_please_check_notification))
            Utilities.openDownloadService(activity, list)
        } else {
            Utilities.toast(activity, getString(R.string.file_already_exists))
        }
    }

    private fun myLibraryDiv(view: View) {
        view.findViewById<FlexboxLayout>(R.id.flexboxLayout).flexDirection = FlexDirection.ROW
        val dbMylibrary = RealmMyLibrary.getMyLibraryByUserId(mRealm, settings!!)
        if (dbMylibrary.isEmpty()) {
            view.findViewById<TextView>(R.id.count_library).visibility = View.GONE
        } else {
            view.findViewById<TextView>(R.id.count_library).text = dbMylibrary.size.toString() + ""
        }
        for ((itemCnt, items) in dbMylibrary.withIndex()) {
            val itemLibraryHomeBinding = ItemLibraryHomeBinding.inflate(LayoutInflater.from(activity))
            val v = itemLibraryHomeBinding.root

            setTextColor(itemLibraryHomeBinding.title, itemCnt)

            val colorResId = if (itemCnt % 2 == 0) R.color.md_white_1000 else R.color.md_grey_300
            val color = context?.let { ContextCompat.getColor(it, colorResId) }
            if (color != null) {
                v.setBackgroundColor(color)
            }

            itemLibraryHomeBinding.title.text = items.title
            itemLibraryHomeBinding.detail.setOnClickListener {
                if (homeItemClickListener != null) homeItemClickListener!!.openLibraryDetailFragment(items)
            }

            myLibraryItemClickAction(itemLibraryHomeBinding.title, items)
            view.findViewById<FlexboxLayout>(R.id.flexboxLayout).addView(v, params)
        }
    }

    private fun initializeFlexBoxView(v: View, id: Int, c: Class<*>) {
        val flexboxLayout: FlexboxLayout = v.findViewById(id)
        flexboxLayout.flexDirection = FlexDirection.ROW
        setUpMyList(c, flexboxLayout, v)
    }

    private fun setUpMyList(c: Class<*>, flexboxLayout: FlexboxLayout, view: View) {
        val dbMycourses: List<RealmObject>
        val userId = settings!!.getString("userId", "--")
        setUpMyLife(userId!!)
        dbMycourses = (when (c) {
            RealmMyCourse::class.java -> {
                RealmMyCourse.getMyByUserId(mRealm, settings!!)
            }
            RealmMyTeam::class.java -> {
                val i = myTeamInit(flexboxLayout)
                setCountText(i, RealmMyTeam::class.java, view)
                return
            }
            RealmMyLife::class.java -> {
                myLifeListInit(flexboxLayout)
                return
            }
            else -> {
                mRealm.where(c as Class<RealmObject>).contains("userId", userId, Case.INSENSITIVE)
                    .findAll()
            }
        }) as List<RealmObject>
        setCountText(dbMycourses.size, c, view)
        val myCoursesTextViewArray = arrayOfNulls<TextView>(dbMycourses.size)
        for ((itemCnt, items) in dbMycourses.withIndex()) {
            setTextViewProperties(myCoursesTextViewArray, itemCnt, items)
            myCoursesTextViewArray[itemCnt]?.let { setTextColor(it, itemCnt) }
            flexboxLayout.addView(myCoursesTextViewArray[itemCnt], params)
        }
    }

    private fun myTeamInit(flexboxLayout: FlexboxLayout): Int {
        val dbMyTeam = RealmMyTeam.getMyTeamsByUserId(mRealm, settings!!)
        val userId = UserProfileDbHandler(activity).userModel.id
        for ((count, ob) in dbMyTeam.withIndex()) {
            val v = LayoutInflater.from(activity).inflate(R.layout.item_home_my_team, flexboxLayout, false)
            val name = v.findViewById<TextView>(R.id.tv_name)
            setBackgroundColor(v, count)
            if ((ob as RealmMyTeam).teamType == "sync") {
                name.setTypeface(null, Typeface.BOLD)
            }
            handleClick(ob._id, ob.name, TeamDetailFragment(), name)
            showNotificationIcons(ob, v, userId!!)
            name.text = ob.name
            flexboxLayout.addView(v, params)
        }
        return dbMyTeam.size
    }

    private fun showNotificationIcons(ob: RealmObject, v: View, userId: String) {
        val current = Calendar.getInstance().timeInMillis
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        val imgTask = v.findViewById<ImageView>(R.id.img_task)
        val imgChat = v.findViewById<ImageView>(R.id.img_chat)
        val notification: RealmTeamNotification? = mRealm.where(RealmTeamNotification::class.java)
            .equalTo("parentId", (ob as RealmMyTeam)._id).equalTo("type", "chat").findFirst()
        val chatCount: Long = mRealm.where(RealmNews::class.java).equalTo("viewableBy", "teams")
            .equalTo("viewableId", ob._id).count()
        if (notification != null) {
            imgChat.visibility = if (notification.lastCount < chatCount) View.VISIBLE else View.GONE
        }
        val tasks: List<RealmTeamTask> =
            mRealm.where(RealmTeamTask::class.java).equalTo("teamId", ob._id)
                .equalTo("completed", false).equalTo("assignee", userId)
                .between("deadline", current, tomorrow.timeInMillis).findAll()
        imgTask.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun myLifeListInit(flexboxLayout: FlexboxLayout) {
        val dbMylife: MutableList<RealmMyLife>
        val rawMylife: List<RealmMyLife> =
            RealmMyLife.getMyLifeByUserId(mRealm, settings!!)
        dbMylife = ArrayList()
        for (item in rawMylife) if (item.isVisible) dbMylife.add(item)
        for ((itemCnt, items) in dbMylife.withIndex()) {
            flexboxLayout.addView(getLayout(itemCnt, items), params)
        }
    }

    private fun setUpMyLife(userId: String) {
        val realm = DatabaseService(requireContext()).realmInstance
        val realmObjects = RealmMyLife.getMyLifeByUserId(mRealm, settings!!)
        if (realmObjects.isEmpty()) {
            if (!realm.isInTransaction) realm.beginTransaction()
            val myLifeListBase = getMyLifeListBase(userId)
            var ml: RealmMyLife
            var weight = 1
            for (item in myLifeListBase) {
                ml = realm.createObject(RealmMyLife::class.java, UUID.randomUUID().toString())
                ml.title = item.title
                ml.imageId = item.imageId
                ml.weight = weight
                ml.userId = item.userId
                ml.isVisible = true
                weight++
            }
            realm.commitTransaction()
        }
    }

    private fun myLibraryItemClickAction(textView: TextView, items: RealmMyLibrary?) {
        textView.setOnClickListener {
            items?.let {
                openResource(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        profileDbHandler.onDestory()
    }

    private fun setCountText(countText: Int, c: Class<*>, v: View) {
        when (c) {
            RealmMyCourse::class.java -> {
                updateCountText(countText, v.findViewById(R.id.count_course))
            }
            RealmMeetup::class.java -> {
                updateCountText(countText, v.findViewById(R.id.count_meetup))
            }
            RealmMyTeam::class.java -> {
                updateCountText(countText, v.findViewById(R.id.count_team))
            }
        }
    }

    private fun updateCountText(countText: Int, tv: TextView) {
        tv.text = countText.toString() + ""
        hideCountIfZero(tv, countText)
    }

    private fun hideCountIfZero(v: View, count: Int) {
        v.visibility = if (count == 0) View.GONE else View.VISIBLE
    }

    fun initView(view: View) {
        view.findViewById<View>(R.id.imageView)
            .setOnClickListener { homeItemClickListener?.openCallFragment(UserProfileFragment()) }
        view.findViewById<View>(R.id.txtFullName)
            .setOnClickListener { homeItemClickListener?.openCallFragment(UserProfileFragment()) }
        dbService = DatabaseService(requireContext())
        mRealm = dbService.realmInstance
        myLibraryDiv(view)
        initializeFlexBoxView(view, R.id.flexboxLayoutCourse, RealmMyCourse::class.java)
        initializeFlexBoxView(view, R.id.flexboxLayoutTeams, RealmMyTeam::class.java)
        initializeFlexBoxView(view, R.id.flexboxLayoutMeetups, RealmMeetup::class.java)
        initializeFlexBoxView(view, R.id.flexboxLayoutMyLife, RealmMyLife::class.java)
        if(!settings?.getBoolean(Constants.KEY_NOTIFICATION_SHOWN, false)!!) {
            showNotificationFragment()
        }
    }

    fun showNotificationFragment() {
        val fragment = NotificationFragment()
        fragment.callback = this
        fragment.resourceList = getLibraryList(mRealm) as List<RealmMyLibrary>
        fragment.show(childFragmentManager, "")
        editor?.putBoolean(Constants.KEY_NOTIFICATION_SHOWN, true)?.commit()
    }

    override fun showResourceDownloadDialog() {
        showDownloadDialog(getLibraryList(mRealm))
    }

    override fun showUserResourceDialog() {
        val userModelList: List<RealmUserModel>
        var dialog: AlertDialog? = null

        userModelList = mRealm.where(RealmUserModel::class.java).sort("joinDate", Sort.DESCENDING).findAll()

        val adapter = UserListArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, userModelList)
        val alertHealthListBinding = AlertHealthListBinding.inflate(LayoutInflater.from(activity))
        alertHealthListBinding.etSearch.visibility = View.GONE
        alertHealthListBinding.spnSort.visibility = View.GONE

        alertHealthListBinding.btnAddMember.setOnClickListener {
            startActivity(Intent(requireContext(), BecomeMemberActivity::class.java))
        }

        alertHealthListBinding.list.adapter = adapter
        alertHealthListBinding.list.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val selected = alertHealthListBinding.list.adapter.getItem(i) as RealmUserModel
            Utilities.log("On item selected")
            showDownloadDialog(getLibraryList(mRealm, selected._id))
            dialog?.dismiss()
        }

        dialog = AlertDialog.Builder(requireActivity())
                .setTitle(getString(R.string.select_member))
                .setView(alertHealthListBinding.root)
                .setCancelable(false)
                .setNegativeButton(R.string.dismiss, null)
                .create()

        dialog.show()
    }

    override fun syncKeyId() {
        di = ProgressDialog(activity)
        di?.setMessage(getString(R.string.syncing_health_please_wait))
        Utilities.log(model.getRoleAsString())
        if (model.getRoleAsString().contains("health")) {
            TransactionSyncManager.syncAllHealthData(mRealm, settings, this)
        } else {
            TransactionSyncManager.syncKeyIv(mRealm, settings, this)
        }
    }

    override fun onSyncStarted() {
        di?.show()
    }

    override fun onSyncComplete() {
        di?.dismiss()
        Utilities.toast(activity, getString(R.string.myhealth_synced_successfully))
    }

    override fun onSyncFailed(msg: String) {
        di?.dismiss()
        Utilities.toast(activity, getString(R.string.myhealth_synced_failed))
    }

    override fun showTaskListDialog() {
        val tasks: List<RealmTeamTask> =
            mRealm.where(RealmTeamTask::class.java).equalTo("assignee", model.id)
                .equalTo("completed", false)
                .greaterThan("deadline", Calendar.getInstance().timeInMillis).findAll()
        if (tasks.isEmpty()) {
            Utilities.toast(requireContext(), getString(R.string.no_due_tasks))
            return
        }
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_expandable_list_item_1, tasks
        )
        AlertDialog.Builder(requireContext()).setTitle(getString(R.string.due_tasks))
            .setAdapter(adapter) { _, p1 ->
//                var task = adapter.getItem(p1);
            }.setNegativeButton(R.string.dismiss, null).show()
    }
}
