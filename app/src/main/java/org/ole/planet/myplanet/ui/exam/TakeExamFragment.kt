package org.ole.planet.myplanet.ui.exam

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.core.widget.NestedScrollView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.Sort
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentTakeExamBinding
import org.ole.planet.myplanet.model.RealmAnswer
import org.ole.planet.myplanet.model.RealmCertification.Companion.isCourseCertified
import org.ole.planet.myplanet.model.RealmExamQuestion
import org.ole.planet.myplanet.model.RealmSubmission
import org.ole.planet.myplanet.model.RealmSubmission.Companion.createSubmission
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.CameraUtils.CapturePhoto
import org.ole.planet.myplanet.utilities.CameraUtils.ImageCaptureCallback
import org.ole.planet.myplanet.utilities.JsonParserUtils.getStringAsJsonArray
import org.ole.planet.myplanet.utilities.JsonUtils.getString
import org.ole.planet.myplanet.utilities.KeyboardUtils.hideSoftKeyboard
import org.ole.planet.myplanet.utilities.Markdown.setMarkdownText
import org.ole.planet.myplanet.utilities.Utilities
import java.util.Arrays
import java.util.Date
import java.util.Locale

class TakeExamFragment : BaseExamFragment(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener, ImageCaptureCallback {
    private var fragmentTakeExamBinding: FragmentTakeExamBinding? = null
    var isCertified = false
    var container: NestedScrollView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentTakeExamBinding = FragmentTakeExamBinding.inflate(inflater, parent, false)
        listAns = HashMap()
        val dbHandler = UserProfileDbHandler(requireActivity())
        user = dbHandler.userModel
        return fragmentTakeExamBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initExam()
        questions =
            mRealm!!.where(RealmExamQuestion::class.java).equalTo("examId", exam!!.id).findAll()
        fragmentTakeExamBinding!!.tvQuestionCount.text = getString(R.string.Q1) + questions?.size
        var q: RealmQuery<*> =
            mRealm!!.where(RealmSubmission::class.java).equalTo("userId", user!!.id).equalTo(
                "parentId", if (!TextUtils.isEmpty(
                        exam!!.courseId
                    )
                ) id + "@" + exam!!.courseId else id
            ).sort("startTime", Sort.DESCENDING)
        if (type == "exam") q = q.equalTo("status", "pending")
        sub = q.findFirst() as RealmSubmission?
        val courseId = exam!!.courseId
        isCertified = isCourseCertified(mRealm!!, courseId)
        if (questions?.size!! > 0) {
            createSubmission()
            Utilities.log("Current index $currentIndex")
            startExam(questions!![currentIndex])
        } else {
            container!!.visibility = View.GONE
            fragmentTakeExamBinding!!.btnSubmit.visibility = View.GONE
            fragmentTakeExamBinding!!.tvQuestionCount.setText(R.string.no_questions)
            Snackbar.make(
                fragmentTakeExamBinding!!.tvQuestionCount,
                R.string.no_questions_available,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun createSubmission() {
        startTransaction()
        sub = createSubmission(sub, mRealm!!)
        Utilities.log("Set parent id $id")
        if (TextUtils.isEmpty(id)) {
            sub!!.parentId =
                if (!TextUtils.isEmpty(exam!!.courseId)) exam!!.id + "@" + exam!!.courseId else exam!!.id
        } else {
            sub!!.parentId =
                if (!TextUtils.isEmpty(exam!!.courseId)) id + "@" + exam!!.courseId else id
        }
        sub!!.userId = user!!.id
        sub!!.status = "pending"
        sub!!.type = type
        sub!!.startTime = Date().time
        if (sub!!.answers != null) {
            currentIndex = sub!!.answers!!.size
        }
        if (sub!!.answers!!.size == questions!!.size && sub!!.type == "survey") {
            currentIndex = 0
        }
        mRealm!!.commitTransaction()
    }

    public override fun startExam(question: RealmExamQuestion?) {
        fragmentTakeExamBinding!!.tvQuestionCount.text =
            getString(R.string.Q) + (currentIndex + 1) + "/" + questions!!.size
        setButtonText()
        fragmentTakeExamBinding!!.groupChoices.removeAllViews()
        fragmentTakeExamBinding!!.llCheckbox.removeAllViews()
        fragmentTakeExamBinding!!.etAnswer.visibility = View.GONE
        fragmentTakeExamBinding!!.groupChoices.visibility = View.GONE
        fragmentTakeExamBinding!!.llCheckbox.visibility = View.GONE
        clearAnswer()
        if (sub!!.answers!!.size > currentIndex) {
            ans = sub!!.answers!![currentIndex]!!.value!!
        }
        if (question!!.type.equals("select", ignoreCase = true)) {
            fragmentTakeExamBinding!!.groupChoices.visibility = View.VISIBLE
            fragmentTakeExamBinding!!.etAnswer.visibility = View.GONE
            selectQuestion(question, ans)
        } else if (question.type.equals(
                "input",
                ignoreCase = true
            ) || question.type.equals("textarea", ignoreCase = true)
        ) {
            setMarkdownViewAndShowInput(fragmentTakeExamBinding!!.etAnswer, question.type!!, ans)
        } else if (question.type.equals("selectMultiple", ignoreCase = true)) {
            fragmentTakeExamBinding!!.llCheckbox.visibility = View.VISIBLE
            fragmentTakeExamBinding!!.etAnswer.visibility = View.GONE
            showCheckBoxes(question, ans)
        }
        fragmentTakeExamBinding!!.tvHeader.text = question.header
        setMarkdownText(fragmentTakeExamBinding!!.tvBody, question.body!!)
        fragmentTakeExamBinding!!.btnSubmit.setOnClickListener(this)
    }

    private fun clearAnswer() {
        ans = ""
        fragmentTakeExamBinding!!.etAnswer.setText("")
        listAns!!.clear()
    }

    fun setButtonText() {
        if (currentIndex == questions!!.size - 1) {
            fragmentTakeExamBinding!!.btnSubmit.setText(R.string.finish)
        } else {
            fragmentTakeExamBinding!!.btnSubmit.setText(R.string.submit)
        }
    }

    private fun showCheckBoxes(question: RealmExamQuestion?, oldAnswer: String) {
        val choices = getStringAsJsonArray(question!!.choices)
        for (i in 0 until choices.size()) {
            addCompoundButton(choices[i].asJsonObject, false, oldAnswer)
        }
    }

    private fun selectQuestion(question: RealmExamQuestion?, oldAnswer: String) {
        val choices = getStringAsJsonArray(question!!.choices)
        for (i in 0 until choices.size()) {
            if (choices[i].isJsonObject) {
                addCompoundButton(choices[i].asJsonObject, true, oldAnswer)
            } else {
                addRadioButton(getString(choices, i), oldAnswer)
            }
        }
    }

    fun addRadioButton(choice: String, oldAnswer: String) {
        val rdBtn =
            LayoutInflater.from(activity).inflate(R.layout.item_radio_btn, null) as RadioButton
        rdBtn.text = choice
        rdBtn.isChecked = choice == oldAnswer
        rdBtn.setOnCheckedChangeListener(this)
        fragmentTakeExamBinding!!.groupChoices.addView(rdBtn)
    }

    fun addCompoundButton(choice: JsonObject?, isRadio: Boolean, oldAnswer: String) {
        val rdBtn = LayoutInflater.from(activity).inflate(
            if (isRadio) R.layout.item_radio_btn else R.layout.item_checkbox,
            null
        ) as CompoundButton
        rdBtn.text = getString("text", choice)
        rdBtn.tag = getString("id", choice)
        rdBtn.isChecked = getString("id", choice) == oldAnswer
        rdBtn.setOnCheckedChangeListener(this)
        if (isRadio) fragmentTakeExamBinding!!.groupChoices.addView(rdBtn) else fragmentTakeExamBinding!!.llCheckbox.addView(
            rdBtn
        )
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_submit) {
            val type = questions!![currentIndex]!!.type
            showTextInput(type)
            if (showErrorMessage(getString(R.string.please_select_write_your_answer_to_continue))) {
                return
            }
            val cont = updateAnsDb()
            capturePhoto()
            hideSoftKeyboard(requireActivity())
            checkAnsAndContinue(cont)
        }
    }

    private fun capturePhoto() {
        try {
            if (isCertified && !isMySurvey) CapturePhoto(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showTextInput(type: String?) {
        if (type.equals("input", ignoreCase = true) || type.equals("textarea", ignoreCase = true)) {
            ans = fragmentTakeExamBinding!!.etAnswer.text.toString()
        }
    }

    private fun updateAnsDb(): Boolean {
        val flag: Boolean
        startTransaction()
        sub!!.status = if (currentIndex == questions!!.size - 1) "requires grading" else "pending"
        val list: RealmList<RealmAnswer>? = sub!!.answers
        val answer = createAnswer(list)
        val que = mRealm!!.copyFromRealm(questions!![currentIndex])
        answer!!.questionId = que!!.id
        answer.value = ans
        answer.setValueChoices(listAns, isLastAnsvalid)
        answer.submissionId = sub!!.id
        Submit_id = answer.submissionId!!
        if (que.getCorrectChoice()!!.size == 0) {
            answer.grade = 0
            answer.mistakes = 0
            flag = true
        } else {
            flag = checkCorrectAns(answer, que)
        }
        removeOldAnswer(list)
        list!!.add(currentIndex, answer)
        sub!!.answers = list
        mRealm!!.commitTransaction()
        return flag
    }

    private fun removeOldAnswer(list: RealmList<RealmAnswer>?) {
        if (sub!!.type == "survey" && list!!.size > currentIndex) list.removeAt(currentIndex) else if (list!!.size > currentIndex && !isLastAnsvalid) {
            list.removeAt(currentIndex)
        }
    }

    private fun checkCorrectAns(answer: RealmAnswer?, que: RealmExamQuestion?): Boolean {
        var flag = false
        answer!!.isPassed = que!!.getCorrectChoice()!!.contains(ans.lowercase(Locale.getDefault()))
        answer.grade = 1
        var mistake = answer.mistakes
        val selectedAns = listAns!!.values.toTypedArray<String>()
        val correctChoices = que.getCorrectChoice()!!.toTypedArray<String>()
        if (!isEqual(selectedAns, correctChoices)) {
            mistake++
        } else {
            flag = true
        }
        answer.mistakes = mistake
        return flag
    }

    fun isEqual(ar1: Array<String>?, ar2: Array<String>?): Boolean {
        Arrays.sort(ar1)
        Arrays.sort(ar2)
        Utilities.log(Arrays.toString(ar1) + " " + Arrays.toString(ar2))
        return Arrays.equals(ar1, ar2)
    }

    private fun startTransaction() {
        if (!mRealm!!.isInTransaction) {
            mRealm!!.beginTransaction()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm!!.close()
    }

    override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {
        if (b) {
            addAnswer(compoundButton)
        } else if (compoundButton.tag != null) {
            listAns!!.remove(compoundButton.text.toString() + "")
        }
    }
}
