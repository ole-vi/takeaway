package org.ole.planet.myplanet.ui.course

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.gson.JsonObject
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import io.realm.Realm
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.callback.OnCourseItemSelected
import org.ole.planet.myplanet.callback.OnHomeItemClickListener
import org.ole.planet.myplanet.callback.OnRatingChangeListener
import org.ole.planet.myplanet.databinding.RowCourseBinding
import org.ole.planet.myplanet.model.RealmMyCourse
import org.ole.planet.myplanet.model.RealmTag
import org.ole.planet.myplanet.utilities.JsonUtils.getInt
import org.ole.planet.myplanet.utilities.Markdown.setMarkdownText
import org.ole.planet.myplanet.utilities.TimeUtils.formatDate
import org.ole.planet.myplanet.utilities.Utilities
import java.util.Collections
import java.util.regex.Pattern

class AdapterCourses(private val context: Context, private var courseList: List<RealmMyCourse?>, private val map: HashMap<String?, JsonObject>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var rowCourseBinding: RowCourseBinding
    private val selectedItems: MutableList<RealmMyCourse?>
    private var listener: OnCourseItemSelected? = null
    private var homeItemClickListener: OnHomeItemClickListener? = null
    private var progressMap: HashMap<String?, JsonObject>? = null
    private var ratingChangeListener: OnRatingChangeListener? = null
    private var mRealm: Realm? = null
    private val config: ChipCloudConfig
    private var isAscending = true
    private var isTitleAscending = true
    private var areAllSelected = true

    init {
        selectedItems = ArrayList()
        if (context is OnHomeItemClickListener) {
            homeItemClickListener = context
        }
        config = Utilities.getCloudConfig().selectMode(ChipCloud.SelectMode.single)
    }

    fun setmRealm(mRealm: Realm?) {
        this.mRealm = mRealm
    }

    fun setRatingChangeListener(ratingChangeListener: OnRatingChangeListener?) {
        this.ratingChangeListener = ratingChangeListener
    }

    fun getCourseList(): List<RealmMyCourse?> {
        return courseList
    }

    fun setCourseList(courseList: List<RealmMyCourse?>) {
        this.courseList = courseList
        sortCourseList()
        sortCourseListByTitle()
        notifyDataSetChanged()
    }

    private fun sortCourseListByTitle() {
        Collections.sort(courseList) { course1: RealmMyCourse?, course2: RealmMyCourse? ->
            if (isTitleAscending) {
                return@sort course1!!.courseTitle!!.compareTo(course2!!.courseTitle!!, ignoreCase = true)
            } else {
                return@sort course2!!.courseTitle!!.compareTo(course1!!.courseTitle!!, ignoreCase = true)
            }
        }
    }

    private fun sortCourseList() {
        Collections.sort(courseList) { course1, course2 ->
            if (isAscending) {
                course1?.createdDate!!.compareTo(course2?.createdDate!!)
            } else {
                course2?.createdDate!!.compareTo(course1?.createdDate!!)
            }
        }
    }

    fun toggleTitleSortOrder() {
        isTitleAscending = !isTitleAscending
        sortCourseListByTitle()
        notifyDataSetChanged()
    }

    fun toggleSortOrder() {
        isAscending = !isAscending
        sortCourseList()
        notifyDataSetChanged()
    }

    fun setProgressMap(progressMap: HashMap<String?, JsonObject>?) {
        this.progressMap = progressMap
    }

    fun setListener(listener: OnCourseItemSelected?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        rowCourseBinding = RowCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHoldercourse(rowCourseBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHoldercourse) {
            holder.bind(position)
            val course = courseList[position]
            if (course != null) {
                holder.rowCourseBinding.title.text = course.courseTitle
                holder.rowCourseBinding.description.text = course.description
                val markdownContentWithLocalPaths = prependBaseUrlToImages(
                    course.description, "file://" + MainApplication.context.getExternalFilesDir(null) + "/ole/"
                )
                setMarkdownText(holder.rowCourseBinding.description, markdownContentWithLocalPaths)
                setTextViewContent(holder.rowCourseBinding.gradLevel, course.gradeLevel, holder.rowCourseBinding.gradLevel, context.getString(R.string.grade_level_colon))
                setTextViewContent(holder.rowCourseBinding.subjectLevel, course.subjectLevel, holder.rowCourseBinding.subjectLevel, context.getString(R.string.subject_level_colon))
                holder.rowCourseBinding.checkbox.isChecked = selectedItems.contains(course)
                holder.rowCourseBinding.courseProgress.max = course.getnumberOfSteps()
                displayTagCloud(holder.rowCourseBinding.flexboxDrawable, position)
                try {
                    holder.rowCourseBinding.tvDate.text = formatDate(course.createdDate!!.trim { it <= ' ' }.toLong(), "MMM dd, yyyy")
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

                holder.rowCourseBinding.ratingBar.setOnTouchListener { _: View?, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_UP) homeItemClickListener!!.showRatingDialog("course", course.courseId, course.courseTitle, ratingChangeListener)
                    true
                }
                holder.rowCourseBinding.checkbox.setOnClickListener { view: View ->
                    Utilities.handleCheck((view as CheckBox).isChecked, position, selectedItems, courseList)
                    if (listener != null) listener!!.onSelectedListChange(selectedItems)
                }
                showProgressAndRating(position, holder)
            }
        }
    }

    private fun setTextViewContent(textView: TextView?, content: String?, layout: View?, prefix: String) {
        if (content.isNullOrEmpty()) {
            layout?.visibility = View.GONE
        } else {
            textView?.text = "$prefix$content"
        }
    }

    fun areAllSelected(): Boolean {
        areAllSelected = selectedItems.size == courseList.size
        return areAllSelected
    }

    fun selectAllItems(selectAll: Boolean) {
        if (selectAll) {
            selectedItems.clear()
            selectedItems.addAll(courseList)
        } else {
            selectedItems.clear()
        }
        notifyDataSetChanged()
        if (listener != null) {
            listener!!.onSelectedListChange(selectedItems)
        }
    }

    private fun displayTagCloud(flexboxDrawable: FlexboxLayout, position: Int) {
        flexboxDrawable.removeAllViews()
        val chipCloud = ChipCloud(context, flexboxDrawable, config)
        val tags: List<RealmTag> = mRealm!!.where(RealmTag::class.java).equalTo("db", "courses").equalTo("linkId", courseList[position]!!.id).findAll()
        showTags(tags, chipCloud)
    }

    private fun showTags(tags: List<RealmTag>, chipCloud: ChipCloud) {
        for (tag in tags) {
            val parent = mRealm!!.where(RealmTag::class.java).equalTo("id", tag.tagId).findFirst()
            parent?.let { showChip(chipCloud, it) }
        }
    }

    private fun showChip(chipCloud: ChipCloud, parent: RealmTag?) {
        chipCloud.addChip(if (parent != null) parent.name else "")
        chipCloud.setListener { _: Int, _: Boolean, b1: Boolean ->
            if (b1 && listener != null) {
                listener!!.onTagClicked(parent)
            }
        }
    }

    private fun showProgressAndRating(position: Int, holder: RecyclerView.ViewHolder) {
        val viewHolder = holder as ViewHoldercourse
        showProgress(position)
        if (map.containsKey(courseList[position]!!.courseId)) {
            val `object` = map[courseList[position]!!.courseId]
            showRating(`object`, viewHolder.rowCourseBinding.average, viewHolder.rowCourseBinding.timesRated, viewHolder.rowCourseBinding.ratingBar)
        } else {
            viewHolder.rowCourseBinding.ratingBar.rating = 0f
        }
    }

    private fun showProgress(position: Int) {
        if (progressMap?.containsKey(courseList[position]?.courseId) == true) {
            val ob = progressMap!![courseList[position]?.courseId]
            rowCourseBinding.courseProgress.max = getInt("max", ob)
            rowCourseBinding.courseProgress.progress = getInt("current", ob)
            if (getInt("current", ob) < getInt("max", ob))
                rowCourseBinding.courseProgress.secondaryProgress = getInt("current", ob) + 1
            rowCourseBinding.courseProgress.visibility = View.VISIBLE
        } else {
            rowCourseBinding.courseProgress.visibility = View.GONE
        }
    }

    private fun openCourse(realm_myCourses: RealmMyCourse?, i: Int) {
        if (homeItemClickListener != null) {
            val f: Fragment = TakeCourseFragment()
            val b = Bundle()
            b.putString("id", realm_myCourses?.courseId)
            b.putInt("position", i)
            f.arguments = b
            homeItemClickListener?.openCallFragment(f)
        }
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    internal inner class ViewHoldercourse(val rowCourseBinding: RowCourseBinding) :
        RecyclerView.ViewHolder(rowCourseBinding.root) {
        private var adapterPosition = 0

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    openCourse(courseList[adapterPosition], 0)
                }
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                rowCourseBinding.courseProgress.scaleY = 0.3f
            }
            rowCourseBinding.courseProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && position < courseList.size) {
                        if (progressMap?.containsKey(courseList[bindingAdapterPosition]?.courseId) == true) {
                            val ob = progressMap!![courseList[bindingAdapterPosition]?.courseId]
                            val current = getInt("current", ob)
                            if (b && i <= current + 1) {
                                openCourse(courseList[bindingAdapterPosition], seekBar.progress)
                            }
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }

        fun bind(position: Int) {
            adapterPosition = position
        }
    }

    companion object {
        @JvmStatic
        fun showRating(`object`: JsonObject?, average: TextView?, ratingCount: TextView?, ratingBar: AppCompatRatingBar?) {
            if (average != null) {
                average.text = String.format("%.2f", `object`?.get("averageRating")?.asFloat)
            }
            if (ratingCount != null) {
                ratingCount.text = "${`object`?.get("total")?.asInt} total"
            }
            if (`object` != null) {
                if (`object`.has("ratingByUser"))
                    if (ratingBar != null) {
                        ratingBar.rating = `object`["ratingByUser"].asInt.toFloat()
                    }
            }
        }

        fun prependBaseUrlToImages(markdownContent: String?, baseUrl: String): String {
            val pattern = "!\\[.*?\\]\\((.*?)\\)"
            val imagePattern = Pattern.compile(pattern)
            val matcher = markdownContent?.let { imagePattern.matcher(it) }
            val result = StringBuffer()
            if (matcher != null) {
                while (matcher.find()) {
                    val relativePath = matcher.group(1)
                    val modifiedPath = relativePath?.replaceFirst("resources/".toRegex(), "")
                    val fullUrl = baseUrl + modifiedPath
                    matcher.appendReplacement(result, "<img src=$fullUrl width=150 height=100/>")
                }
            }
            matcher?.appendTail(result)
            return result.toString()
        }
    }
}