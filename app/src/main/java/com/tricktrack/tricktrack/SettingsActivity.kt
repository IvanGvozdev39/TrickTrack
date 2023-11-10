package com.tricktrack.tricktrack

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout


class SettingsActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private var clickedPreferenceNumber = 0
    private lateinit var pref: SharedPreferences
    private lateinit var languagesHardCode: Array<String>
    private lateinit var translationClickable: LinearLayout
    private lateinit var translationSwitch: Switch
    private lateinit var nightRideModeClickable: LinearLayout
    private lateinit var nightRideModeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.back_button).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        languagesHardCode = resources.getStringArray(R.array.language_preference_hardcode)

        pref = getSharedPreferences("Settings", Context.MODE_PRIVATE)

//        val languageClickable = findViewById<AppCompatButton>(R.id.language_setting)
//        languageClickable.setOnClickListener {
//            clickedPreferenceNumber = 0
//            val dialogPreference = Dialog(this)
//            dialogPreference.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            dialogPreference.setCancelable(true)
//            dialogPreference.setContentView(R.layout.dialog_list_preference)
//            dialogPreference.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//
//            val preferenceListView = dialogPreference.findViewById<ListView>(R.id.list_preference_list_view)
//
//            val arrayAdapter = ArrayAdapter(
//                this@SettingsActivity, android.R.layout.simple_list_item_single_choice,
//                resources.getStringArray(R.array.language_preference)
//            )
//            preferenceListView?.adapter = arrayAdapter
//            preferenceListView?.choiceMode = ListView.CHOICE_MODE_SINGLE
//            preferenceListView?.onItemClickListener = this
//
//            val initLanguage = pref.getString("Language", "")
//            if (initLanguage != "") {
//                for (i in 0 until languagesHardCode.size)
//                    if (initLanguage.equals(languagesHardCode[i])) {
//                        preferenceListView?.setItemChecked(i, true)
//                    }
//            }
//
//            val dialogBtnBack = dialogPreference.findViewById<AppCompatButton>(R.id.button_back)
//            dialogBtnBack.setOnClickListener {
//                dialogPreference.dismiss()
//            }
//
//
//            dialogPreference.show()
//
//            val window = dialogPreference.window
//            val lp = WindowManager.LayoutParams()
//            lp.copyFrom(dialogPreference.window!!.attributes)
//            lp.width = WindowManager.LayoutParams.MATCH_PARENT
//            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//            window!!.attributes = lp
//        }

        translationClickable = findViewById<LinearLayout>(R.id.automatic_translation_setting)
        translationSwitch = findViewById<Switch>(R.id.translation_switch)

        val initTranslation = pref.getBoolean("Translation", false)
        if (initTranslation)
            translationSwitch.isChecked = true

        translationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                prefSaveBoolean("Translation", true)
            } else {
                prefSaveBoolean("Translation", false)
            }
        }

        translationClickable.setOnClickListener {
            if (translationSwitch.isChecked)
                translationSwitch.isChecked = false
            else
                translationSwitch.isChecked = true
        }




        nightRideModeClickable = findViewById<LinearLayout>(R.id.night_ride_mode_setting)
        nightRideModeSwitch = findViewById<Switch>(R.id.night_ride_mode_switch)
        val initNightRideMode = pref.getBoolean("NightRideMode", false)

        if (initNightRideMode) {
            nightRideModeSwitch.isChecked = true
            applyDarkTheme()
        }

        nightRideModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                prefSaveBoolean("NightRideMode", true)
                applyDarkTheme()
            } else {
                prefSaveBoolean("NightRideMode", false)
                applyLightTheme()
            }
        }

        nightRideModeClickable.setOnClickListener {
            if (nightRideModeSwitch.isChecked)
                nightRideModeSwitch.isChecked = false
            else
                nightRideModeSwitch.isChecked = true
        }

    }


    private fun applyDarkTheme() {
        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main_background)
        val moreSoonTV = findViewById<TextView>(R.id.more_customization_settings_soon)
        mainConstraintLayout.setBackgroundColor(getColor(R.color.dark_theme))
        translationClickable.setBackgroundColor(getColor(R.color.dark_theme))
        nightRideModeClickable.setBackgroundColor(getColor(R.color.dark_theme))
        moreSoonTV.setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.automatic_translation_description).setTextColor(getColor(R.color.lighter_grey))
        translationSwitch.setTextColor(getColor(R.color.lighter_grey))
        nightRideModeSwitch.setTextColor(getColor(R.color.lighter_grey))
    }


    private fun applyLightTheme() {
        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main_background)
        val moreSoonTV = findViewById<TextView>(R.id.more_customization_settings_soon)
        mainConstraintLayout.setBackgroundColor(getColor(R.color.transparent))
        translationClickable.setBackgroundColor(getColor(R.color.transparent))
        nightRideModeClickable.setBackgroundColor(getColor(R.color.transparent))
        moreSoonTV.setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.automatic_translation_description).setTextColor(getColor(R.color.black))
        translationSwitch.setTextColor(getColor(R.color.black))
        nightRideModeSwitch.setTextColor(getColor(R.color.black))
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        if (clickedPreferenceNumber == 0) {
//            val languageChosen = parent?.getItemAtPosition(position) as String
//            prefSaveString("Language", languagesHardCode[position])
//            Log.d("123er4", position.toString() + " " + languagesHardCode[position])
//            val languageCodes = resources.getStringArray(R.array.language_codes)
////            setAppLocale(languageCodes[position])
//        }
    }


    /*fun setAppLocale(languageCode: String?) {
        val locale = Locale(languageCode!!)
        Locale.setDefault(locale)

        val res: Resources = baseContext.getResources()
        val config = Configuration(res.getConfiguration())
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }*/


    fun prefSaveString(key: String?, value: String?) {
        val edit = pref.edit()
        edit.putString(key, value)
        edit.apply()
    }

    fun prefSaveBoolean(key: String?, value: Boolean) {
        val edit = pref.edit()
        edit.putBoolean(key, value)
        edit.apply()
    }
}