package org.ole.planet.myplanet.ui.library

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmList
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.ActivityAddResourceBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.CheckboxListView
import org.ole.planet.myplanet.utilities.Utilities
import java.util.Calendar
import java.util.UUID

class AddResourceActivity : AppCompatActivity() {
    private lateinit var activityAddResourceBinding: ActivityAddResourceBinding
    private lateinit var mRealm: Realm
    private lateinit var userModel: RealmUserModel
    var subjects: RealmList<String>? = null
    var levels: RealmList<String>? = null
    private var resourceFor: RealmList<String>? = null
    private var resourceUrl: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAddResourceBinding = ActivityAddResourceBinding.inflate(layoutInflater)
        setContentView(activityAddResourceBinding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        userModel = UserProfileDbHandler(this).userModel
        resourceUrl = intent.getStringExtra("resource_local_url")
        levels = RealmList()
        subjects = RealmList()
        resourceFor = RealmList()
        mRealm = DatabaseService(this).realmInstance
        initializeViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mRealm != null && !mRealm.isClosed) mRealm.close()
    }

    private fun initializeViews() {
        activityAddResourceBinding.fileUrl.text = "${getString(R.string.file)} $resourceUrl"
        activityAddResourceBinding.tvAddedBy.text = userModel.name
        activityAddResourceBinding.tvLevels.setOnClickListener { view: View ->
            showMultiSelectList(resources.getStringArray(R.array.array_levels), levels, view)
        }
        activityAddResourceBinding.tvSubject.setOnClickListener { view: View ->
            showMultiSelectList(resources.getStringArray(R.array.array_subjects), subjects, view)
        }
        activityAddResourceBinding.tvResourceFor.setOnClickListener { view: View ->
            showMultiSelectList(resources.getStringArray(R.array.array_resource_for), subjects, view)
        }
        activityAddResourceBinding.btnSubmit.setOnClickListener { saveResource() }
        activityAddResourceBinding.btnCancel.setOnClickListener { finish() }
    }

    private fun saveResource() {
        val title = activityAddResourceBinding.etTitle.text.toString().trim { it <= ' ' }
        if (!validate(title)) return
        mRealm.executeTransactionAsync(Realm.Transaction { realm: Realm ->
            val id = UUID.randomUUID().toString()
            val resource = realm.createObject(RealmMyLibrary::class.java, id)
            resource.title = title
            createResource(resource, id)
        }, Realm.Transaction.OnSuccess {
            Utilities.toast(this@AddResourceActivity, getString(R.string.resource_saved_successfully))
            finish()
        })
    }

    private fun createResource(resource: RealmMyLibrary, id: String) {
        resource.addedBy = activityAddResourceBinding.tvAddedBy.text.toString().trim { it <= ' ' }
        resource.author = activityAddResourceBinding.etAuthor.text.toString().trim { it <= ' ' }
        resource.resourceId = id
        resource.year = activityAddResourceBinding.etYear.text.toString().trim { it <= ' ' }
        resource.description = activityAddResourceBinding.etDescription.text.toString().trim { it <= ' ' }
        resource.setPublisher(activityAddResourceBinding.etPublisher.text.toString().trim { it <= ' ' })
        resource.linkToLicense = activityAddResourceBinding.etLinkToLicense.text.toString().trim { it <= ' ' }
        resource.openWith = activityAddResourceBinding.spnOpenWith.selectedItem.toString()
        resource.language = activityAddResourceBinding.spnLang.selectedItem.toString()
        resource.mediaType = activityAddResourceBinding.spnMedia.selectedItem.toString()
        resource.resourceType = activityAddResourceBinding.spnResourceType.selectedItem.toString()
        resource.subject = subjects
        resource.setUserId(RealmList())
        resource.level = levels
        resource.createdDate = Calendar.getInstance().timeInMillis.toString()
        resource.resourceFor = resourceFor
        resource.resourceLocalAddress = resourceUrl
        resource.resourceOffline = true
        resource.filename = resourceUrl!!.substring(resourceUrl!!.lastIndexOf("/"))
    }

    private fun validate(title: String): Boolean {
        if (title.isEmpty()) {
            activityAddResourceBinding.tlTitle.error = getString(R.string.title_is_required)
            return false
        }
        if (levels!!.isEmpty()) {
            Utilities.toast(this, getString(R.string.level_is_required))
            return false
        }
        if (subjects!!.isEmpty()) {
            Utilities.toast(this, getString(R.string.subject_is_required))
            return false
        }
        return true
    }

    private fun showMultiSelectList(list: Array<String>, items: MutableList<String>?, view: View) {
        val listView = CheckboxListView(this)
        val adapter = ArrayAdapter(this, R.layout.rowlayout, R.id.checkBoxRowLayout, list)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter
        AlertDialog.Builder(this).setView(listView).setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val selected = listView.selectedItemsList
            items!!.clear()
            var selection = ""
            for (index in selected) {
                val s = list[index]
                selection += "$s ,"
                items.add(s)
            }
            (view as TextView).text = selection
        }.setNegativeButton(R.string.dismiss, null).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
