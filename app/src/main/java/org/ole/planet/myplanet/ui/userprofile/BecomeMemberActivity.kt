package org.ole.planet.myplanet.ui.userprofile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.realm.Realm
import org.ole.planet.myplanet.MainApplication
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.base.BaseActivity
import org.ole.planet.myplanet.databinding.ActivityBecomeMemberBinding
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.datamanager.Service
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.sync.LoginActivity
import org.ole.planet.myplanet.ui.sync.SyncActivity
import org.ole.planet.myplanet.utilities.NetworkUtils
import org.ole.planet.myplanet.utilities.Utilities
import org.ole.planet.myplanet.utilities.VersionUtils
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class BecomeMemberActivity : BaseActivity() {
    private lateinit var activityBecomeMemberBinding: ActivityBecomeMemberBinding
    var dob: String = "";
    lateinit var settings: SharedPreferences
    var guest: Boolean = false
    private fun showDatePickerDialog() {
        val now = Calendar.getInstance()
        val dpd = DatePickerDialog(
            this, { _, i, i1, i2 ->
                dob = String.format(Locale.US, "%04d-%02d-%02d", i, i1 + 1, i2)
                activityBecomeMemberBinding.txtDob.text = dob
            }, now[Calendar.YEAR], now[Calendar.MONTH], now[Calendar.DAY_OF_MONTH]
        )
        dpd.datePicker.maxDate = now.timeInMillis
        dpd.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBecomeMemberBinding = ActivityBecomeMemberBinding.inflate(layoutInflater)
        setContentView(activityBecomeMemberBinding.root)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var mRealm: Realm = DatabaseService(this).realmInstance;
        var user = UserProfileDbHandler(this).userModel;
        val languages = resources.getStringArray(R.array.language)
        val adapter = ArrayAdapter<String>(this, R.layout.become_a_member_spinner_layout, languages)
        activityBecomeMemberBinding.spnLang.adapter = adapter
        activityBecomeMemberBinding.txtDob.setOnClickListener {
            showDatePickerDialog()
        }

        val username = intent.getStringExtra("username");
        guest = intent.getBooleanExtra("guest", false);

        settings = getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE)
        textChangedListener(mRealm)

        if (guest) {
            activityBecomeMemberBinding.etUsername.setText(username)
            activityBecomeMemberBinding.etUsername.isFocusable = false
        }

        activityBecomeMemberBinding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()

                val firstChar = if (input.isNotEmpty()) input[0] else '\u0000'
                var hasInvalidCharacters = false
                var hasSpecialCharacters = false
                var hasDiacriticCharacters = false

                val normalizedText = Normalizer.normalize(s, Normalizer.Form.NFD)

                for (element in input) {
                    if (element != '_' && element != '.' && element != '-'
                        && !Character.isDigit(element) && !Character.isLetter(element)) {
                        hasInvalidCharacters = true
                        break
                    }
                }

                val regex = ".*[ßäöüéèêæÆœøØ¿àìòùÀÈÌÒÙáíóúýÁÉÍÓÚÝâîôûÂÊÎÔÛãñõÃÑÕëïÿÄËÏÖÜŸåÅŒçÇðÐ].*"
                val pattern = Pattern.compile(regex)
                val matcher = pattern.matcher(input)

                hasSpecialCharacters = matcher.matches()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    hasDiacriticCharacters = !normalizedText.codePoints().allMatch { codePoint: Int ->
                        Character.isLetterOrDigit(codePoint) || codePoint == '.'.code || codePoint == '-'.code || codePoint == '_'.code
                    }
                }

                if (!Character.isDigit(firstChar) && !Character.isLetter(firstChar)) {
                    activityBecomeMemberBinding.etUsername.error = getString(R.string.must_start_with_letter_or_number)
                } else if (hasInvalidCharacters || hasDiacriticCharacters || hasSpecialCharacters) {
                        activityBecomeMemberBinding.etUsername.error = getString(R.string.only_letters_numbers_and_are_allowed)
                } else {
                    val lowercaseText = input.lowercase()
                    if (input != lowercaseText) {
                        activityBecomeMemberBinding.etUsername.setText(lowercaseText)
                        activityBecomeMemberBinding.etUsername.setSelection(lowercaseText.length)
                    }
                    activityBecomeMemberBinding.etUsername.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        activityBecomeMemberBinding.btnCancel.setOnClickListener {
            finish()
        }

        activityBecomeMemberBinding.btnSubmit.setOnClickListener {
            var username: String? = activityBecomeMemberBinding.etUsername.text.toString()
            var password: String? = activityBecomeMemberBinding.etPassword.text.toString()
            var repassword: String? = activityBecomeMemberBinding.etRePassword.text.toString()
            var fname: String? = activityBecomeMemberBinding.etFname.text.toString()
            var lname: String? = activityBecomeMemberBinding.etLname.text.toString()
            var mname: String? = activityBecomeMemberBinding.etMname.text.toString()
            var email: String? = activityBecomeMemberBinding.etEmail.text.toString()
            var language: String? = activityBecomeMemberBinding.spnLang.selectedItem.toString()
            var phoneNumber: String? = activityBecomeMemberBinding.etPhone.text.toString()
            var birthDate: String? = dob
            var level: String? = activityBecomeMemberBinding.spnLevel.selectedItem.toString()
            var gender: String? = null
          
            val firstChar = if (username!!.isNotEmpty()) username[0] else null
            val hasInvalidCharacters = username.any { char ->
                char != '_' && char != '.' && char != '-' && !Character.isDigit(char) && !Character.isLetter(char)
            }

            val normalizedText = Normalizer.normalize(username, Normalizer.Form.NFD)

            val regex = ".*[ßäöüéèêæÆœøØ¿àìòùÀÈÌÒÙáíóúýÁÉÍÓÚÝâîôûÂÊÎÔÛãñõÃÑÕëïÿÄËÏÖÜŸåÅŒçÇðÐ].*"
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(username)

            val hasSpecialCharacters = matcher.matches()
            val hasDiacriticCharacters = !normalizedText.codePoints().allMatch { codePoint: Int ->
                Character.isLetterOrDigit(codePoint) || codePoint == '.'.code || codePoint == '-'.code || codePoint == '_'.code
            }

            if (TextUtils.isEmpty(username)) {
                activityBecomeMemberBinding.etUsername.error = getString(R.string.please_enter_a_username)
            } else if (username.contains(" ")) {
                activityBecomeMemberBinding.etUsername.error = getString(R.string.invalid_username)
            } else if (firstChar != null && !Character.isDigit(firstChar) && !Character.isLetter(firstChar)) {
                activityBecomeMemberBinding.etUsername.error = getString(R.string.must_start_with_letter_or_number)
            } else if (hasInvalidCharacters || hasSpecialCharacters || hasDiacriticCharacters) {
               activityBecomeMemberBinding.etUsername.error = getString(R.string.only_letters_numbers_and_are_allowed)
            } else if (TextUtils.isEmpty(password)) {
                activityBecomeMemberBinding.etPassword.error = getString(R.string.please_enter_a_password)
            } else if (password != repassword) {
                activityBecomeMemberBinding.etRePassword.error = getString(R.string.password_doesn_t_match)
            } else if (!TextUtils.isEmpty(email) && !Utilities.isValidEmail(email)) {
                activityBecomeMemberBinding.etEmail.error = getString(R.string.invalid_email)
            } else if (activityBecomeMemberBinding.rbGender.checkedRadioButtonId == -1) {
                Utilities.toast(this, getString(R.string.please_select_gender))
            } else if (level == null) {
                Utilities.toast(this, getString(R.string.level_is_required));
            } else {
                if (activityBecomeMemberBinding.male.isChecked) {
                    gender = "male"
                } else if (activityBecomeMemberBinding.female.isChecked) {
                    gender = "female"
                }

                if (TextUtils.isEmpty(password) && !TextUtils.isEmpty(phoneNumber)) {
                    activityBecomeMemberBinding.etRePassword.setText(phoneNumber)
                    password = phoneNumber
                    ///Add dialog that using phone as password , Agree / disagree
                }

                checkMandatoryFieldsAndAddMember(
                    username,
                    password!!,
                    repassword,
                    fname,
                    lname,
                    mname,
                    email,
                    language,
                    level,
                    phoneNumber,
                    birthDate,
                    gender,
                    mRealm
                )
            }
        }
    }

    private fun checkMandatoryFieldsAndAddMember(
        username: String,
        password: String,
        repassword: String?,
        fname: String?,
        lname: String?,
        mname: String?,
        email: String?,
        language: String?,
        level: String?,
        phoneNumber: String?,
        birthDate: String?,
        gender: String?,
        mRealm: Realm
    ) {
        /**
         * Creates and adds a new member if the username and password
         * are not empty and password matches repassword.
         */
        if (username.isNotEmpty() && password.isNotEmpty() && repassword == password) {
            var obj = JsonObject()
            obj.addProperty("name", username)
            obj.addProperty("firstName", fname)
            obj.addProperty("lastName", lname)
            obj.addProperty("middleName", mname)
            obj.addProperty("password", password)
            obj.addProperty("isUserAdmin", false)
            obj.addProperty("joinDate", Calendar.getInstance().timeInMillis)
            obj.addProperty("email", email)
            obj.addProperty("planetCode", settings.getString("planetCode", ""))
            obj.addProperty("parentCode", settings.getString("parentCode", ""))
            obj.addProperty("language", language)
            obj.addProperty("level", level)
            obj.addProperty("phoneNumber", phoneNumber)
            obj.addProperty("birthDate", birthDate)
            obj.addProperty("gender", gender)
            obj.addProperty("type", "user")
            obj.addProperty("betaEnabled", false)
            obj.addProperty("androidId", NetworkUtils.getUniqueIdentifier())
            obj.addProperty("uniqueAndroidId", VersionUtils.getAndroidId(MainApplication.context))
            obj.addProperty(
                "customDeviceName", NetworkUtils.getCustomDeviceName(MainApplication.context)
            )
            var roles = JsonArray()
            roles.add("learner")
            obj.add("roles", roles)
            activityBecomeMemberBinding.pbar.visibility = View.VISIBLE
            Service(this).becomeMember(mRealm, obj) { res ->
                runOnUiThread {
                    activityBecomeMemberBinding.pbar.visibility = View.GONE
                    Utilities.toast(this, res)
                }
                finish()
            }

            if (guest){
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("username", username)
                intent.putExtra("guest", guest)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun textChangedListener(mRealm: Realm) {
        activityBecomeMemberBinding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (RealmUserModel.isUserExists(mRealm, activityBecomeMemberBinding.etUsername.text.toString())) {
                    activityBecomeMemberBinding.etUsername.error = getString(R.string.username_taken)
                    return
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        activityBecomeMemberBinding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                activityBecomeMemberBinding.etRePassword.isEnabled = activityBecomeMemberBinding.etPassword.text.toString().isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })
    }
}
