package com.tricktrack.tricktrack

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.lang.UCharacter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntFunction
import java.util.function.Predicate

class SpotReviewActivity : AppCompatActivity() {

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var title: String
    private lateinit var description: String
    private var images = arrayListOf<Bitmap>()
    private lateinit var type: String
    private lateinit var condition: String
    private lateinit var proponent: String
    private lateinit var date: String

    private lateinit var titleTV: TextView
    private lateinit var descriptionTV: TextView
    private lateinit var typeIcon: ImageView
    private lateinit var typeTV: TextView
    private lateinit var conditionTV: TextView
    private lateinit var mainScrollView: ScrollView
    private lateinit var loading: ProgressBar
    private val db = Firebase.firestore
    private lateinit var viewPager2: ViewPager2
    private var imagesList = mutableListOf<Bitmap>()
    private lateinit var goLayout: LinearLayout
    private lateinit var editBtn: MaterialButton
    private lateinit var routeBtn: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var dialogGoogleSignIn: Dialog
    private lateinit var bookmarkBtn: ImageView
    private var spotInFavorites = false
    private var onLeftTab = true
    private lateinit var dialogSignIn: Dialog
    private val conditions = DownloadConditions.Builder().requireWifi().build()
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spot_review)

        pref = getSharedPreferences("Settings", Context.MODE_PRIVATE)


        titleTV = findViewById(R.id.spot_title)
        descriptionTV = findViewById(R.id.spot_description)
        typeIcon = findViewById(R.id.spot_type_icon)
        typeTV = findViewById(R.id.spot_type)
        conditionTV = findViewById(R.id.spot_condition)
        mainScrollView = findViewById(R.id.main_scroll_view)
        loading = findViewById(R.id.loading_progress_bar)
        viewPager2 = findViewById(R.id.view_pager2)
        goLayout = findViewById(R.id.go_layout)
        editBtn = findViewById(R.id.edit_button)
        routeBtn = findViewById(R.id.route_button)
        bookmarkBtn = findViewById(R.id.bookmark_image_view)


        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        title = intent.getStringExtra("title").toString()
        type = intent.getStringExtra("type").toString()

        val spotsCollectionRef = db.collection("Spots")
        val documentName = latitude.toString() + longitude.toString()

        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //TODO: bookmark initialization:
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            db.collection("Users").document(auth.currentUser!!.uid).collection("FavoriteSpots").document(latitude.toString()+longitude.toString()).get().addOnCompleteListener(
                OnCompleteListener<DocumentSnapshot> { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document.exists()) {
                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_filled_icon))
                            spotInFavorites = true
                        }
                        bookmarkBtnListenerSetup()
                    }
            })
        } else {
            bookmarkBtnListenerSetup()
        }


        spotsCollectionRef.document(documentName).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Extract data from the Firestore document
                    description = documentSnapshot.getString("description") ?: ""
                    condition = documentSnapshot.getString("condition") ?: ""
                    proponent = documentSnapshot.getString("proponent") ?: ""
                    date = documentSnapshot.getString("date") ?: ""

                    val spotTypeTexts = arrayOf(
                        getString(R.string.choose_spot_type),
                        getString(R.string.skatepark),
                        getString(R.string.covered_skatepark),
                        getString(R.string.street_spot),
                        getString(R.string.diy_spot),
                        getString(R.string.shop),
                        getString(R.string.dirt)
                    )
                    val spotTypeFirebaseTexts = arrayOf(
                        getString(R.string.choose_spot_type),
                        "Открытый скейтпарк",
                        "Крытый скейтпарк",
                        "Стрит",
                        "D.I.Y",
                        "Шоп",
                        "Dirt"
                    )

                    val spotTypeImages =
                        intArrayOf(
                            R.drawable.not_chosen_mark,
                            R.drawable.park_spot_mark,
                            R.drawable.covered_park_mark,
                            R.drawable.street_spot_mark,
                            R.drawable.diy_spot_mark,
                            R.drawable.shop_mark,
                            R.drawable.dirt_spot_mark
                        )
                    val conditionTexts = arrayOf(
                        getString(R.string.choose_spot_condition),
                        getString(R.string.excelent),
                        getString(R.string.good),
                        getString(R.string.normal),
                        getString(R.string.bad),
                        getString(R.string.very_bad)
                    )
                    val conditionFirebaseTexts = arrayOf(
                        getString(R.string.choose_spot_condition),
                        "Отличное",
                        "Хорошее",
                        "Нормальное",
                        "Плохое",
                        "Ужасное"
                    )

                    var conditionIndex = 0
                    for (i in 0 until conditionTexts.size) {
                        if (condition.equals(conditionFirebaseTexts[i]))
                            conditionIndex = i
                    }

                    condition = conditionTexts[conditionIndex]


                    // Create an array to store image URLs
                    val imageUrls = ArrayList<String>()

                    // Add image URLs to the array (image1, image2, image3, ...)
                    for (i in 1..5) {
                        val imageUrl = documentSnapshot.getString("image$i")
                        if (!imageUrl.isNullOrEmpty()) {
                            imageUrls.add(imageUrl)
                        }
                    }

                    titleTV.setText(title)

                    if (pref.getBoolean("Translation", false)) {
                        var correctLanguageCode = ""
                        for (c in title.toCharArray()) {
                            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                                correctLanguageCode = "en"
                                break
                            }
                        }
                        if (correctLanguageCode.isEmpty()) {
                            correctLanguageCode = ""
                            for (c in title.toCharArray()) {
                                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                                    correctLanguageCode = "ru"
                                    break
                                }
                            }
                        }

                        if (!correctLanguageCode.isEmpty()) {
                            val actionBarTitle = findViewById<TextView>(R.id.actionbar_title)
                            actionBarTitle.setText(getString(R.string.translator_initialization))
                            val options = TranslatorOptions.Builder()
                                .setSourceLanguage("ru")
                                .setTargetLanguage(Locale.getDefault().language)
                                .build()

                            val translator = Translation.getClient(options)
                            translator.downloadModelIfNeeded(conditions).addOnSuccessListener {
                                actionBarTitle.setText(getString(R.string.translating))
                                translator.translate(title).addOnSuccessListener {
                                    actionBarTitle.setText(getString(R.string.spot_review))
                                    titleTV.setText(it)
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@SpotReviewActivity,
                                        getString(R.string.error_translating),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    actionBarTitle.setText(getString(R.string.spot_review))
                                }
                                translator.translate(description).addOnSuccessListener {
                                    actionBarTitle.setText(getString(R.string.spot_review))
                                    descriptionTV.setText(it)
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@SpotReviewActivity,
                                        getString(R.string.error_translating),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    actionBarTitle.setText(getString(R.string.spot_review))
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@SpotReviewActivity,
                                    getString(R.string.error_downloading_model),
                                    Toast.LENGTH_SHORT
                                ).show()
                                actionBarTitle.setText(getString(R.string.spot_review))
                            }
                        }
                    }

                    descriptionTV.setText(description)
                    conditionTV.setText(condition)
                    val dateTV = findViewById<TextView>(R.id.date_text_view)
                    dateTV.setText(date)
                    val proponentTV = findViewById<TextView>(R.id.proponent_text_view)
                    proponentTV.setText(proponent)

                    var correctIconId: Int = R.drawable.not_chosen_mark
                    val typeImages = arrayOf(
                        R.drawable.park_spot_mark,
                        R.drawable.covered_park_mark,
                        R.drawable.street_spot_mark,
                        R.drawable.diy_spot_mark,
                        R.drawable.shop_mark,
                        R.drawable.dirt_spot_mark
                    )
                    val typeTexts = arrayOf(
                        "Открытый скейтпарк",
                        "Крытый скейтпарк",
                        "Стрит",
                        "D.I.Y",
                        "Шоп",
                        "Dirt"
                    )
                    for (i in typeImages.indices) {
                        if (type == typeTexts[i])
                            correctIconId = typeImages[i]
                    }
                    typeIcon.setImageDrawable(getDrawable(correctIconId))

                    var spotTypeIndex = 0
                    for (i in 0 until spotTypeTexts.size) {
                        if (type.equals(spotTypeFirebaseTexts[i]))
                            spotTypeIndex = i
                    }
                    type = spotTypeTexts[spotTypeIndex]
                    typeTV.setText(type)


                    // Download images from Firebase Storage and populate ImageSwitcher

                    downloadImages(imageUrls)

                } else {
                    // Handle the case where the document does not exist
                }
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }

        routeBtn.setOnClickListener {
            val geoUri = "geo:$latitude,$longitude"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            intent.setPackage("ru.yandex.yandexmaps") // You can set a preferred map app, like Google Maps

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
// Yandex Maps is not installed, so you can handle this case here.
                // For example, you can open a web URL with a map service, like Google Maps, as a fallback.
                val fallbackUri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri))

                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    // If no map app is available, you can show a message to the user.
                    Toast.makeText(this, getString(R.string.no_map_app_on_device), Toast.LENGTH_LONG).show()
                }
            }
        }

        if (pref.getBoolean("NightRideMode", false)) {
            findViewById<LinearLayout>(R.id.main_background_spot_review).setBackgroundColor(getColor(R.color.dark_theme))
            titleTV.setTextColor(getColor(R.color.lighter_grey))
            typeTV.setTextColor(getColor(R.color.lighter_grey))
            conditionTV.setTextColor(getColor(R.color.lighter_grey))
            descriptionTV.setTextColor(getColor(R.color.lighter_grey))
            findViewById<TextView>(R.id.condition_condition_tv).setTextColor(getColor(R.color.lighter_grey))
            findViewById<TextView>(R.id.updated_updated_tv).setTextColor(getColor(R.color.lighter_grey))
            findViewById<TextView>(R.id.date_text_view).setTextColor(getColor(R.color.lighter_grey))
            findViewById<TextView>(R.id.proponent_text_view).setTextColor(getColor(R.color.lighter_grey))
            loading.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
        }
    }


    private fun bookmarkBtnListenerSetup() {
        bookmarkBtn.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                if (!spotInFavorites) {
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_filled_icon))

                    val typesHardcode = resources.getStringArray(R.array.types_hardcode)
                    val types = resources.getStringArray(R.array.types)

                    for (i in 0 until types.size) {
                        if (type.equals(types.get(i)))
                            type = typesHardcode.get(i)
                    }

                    val data = hashMapOf(
                        "title" to title,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "type" to type
                    )
                    db.collection("Users").document(auth.currentUser!!.uid).collection("FavoriteSpots")
                        .document(latitude.toString()+longitude.toString()).set(data as Map<String, Any>).addOnSuccessListener {
                        spotInFavorites = true
                    }.addOnFailureListener {
                        bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_empty_icon))
                        Toast.makeText(baseContext, getString(R.string.bookmark_addition_error), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_empty_icon))
                    db.collection("Users").document(auth.currentUser!!.uid).collection("FavoriteSpots")
                        .document(latitude.toString()+longitude.toString()).delete().addOnSuccessListener {
                            spotInFavorites = false
                        }.addOnFailureListener {
                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_filled_icon))
                            Toast.makeText(baseContext, getString(R.string.bookmark_removal_error), Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this@SpotReviewActivity, getString(R.string.you_need_to_sign_in_to_bookmark), Toast.LENGTH_SHORT).show()
                signInDialogShowup()
            }
        }
    }

    private fun signInDialogShowup() {
        onLeftTab = true
        dialogSignIn = Dialog(this)
        dialogSignIn.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogSignIn.setCancelable(true)
        dialogSignIn.setContentView(R.layout.dialog_signin)
        dialogSignIn.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val nightRideModeOn = pref.getBoolean("NightRideMode", false)

        val dialogLoginButton =
            dialogSignIn.findViewById<AppCompatButton>(R.id.login_button)
        val dialogSignUpButton = dialogSignIn.findViewById<AppCompatButton>(R.id.sign_up_button)
        dialogLoginButton.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            val emailField = dialogSignIn.findViewById<EditText>(R.id.login_email)
            val passwordField = dialogSignIn.findViewById<EditText>(R.id.login_password)
            val loginErrorMessage = dialogSignIn.findViewById<TextView>(R.id.login_error_message)

            val emailQuery = emailField.text.toString()
            val passwordQuery = passwordField.text.toString()

            emailField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // This method is called before the text is changed.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text is changed. You can perform actions here.
                    loginErrorMessage.setText("")
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })

            passwordField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // This method is called before the text is changed.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text is changed. You can perform actions here.
                    loginErrorMessage.setText("")
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })

            if (emailQuery.isEmpty() || passwordQuery.isEmpty())
                loginErrorMessage.setText(getString(R.string.all_fields_are_compulsory))
            else {
                val loadingLogin = dialogSignIn.findViewById<ProgressBar>(R.id.loading_login)
                loadingLogin.visibility = View.VISIBLE
                dialogLoginButton.setText("")

                //Signing the user in:
                auth.signInWithEmailAndPassword(emailQuery, passwordQuery)
                    .addOnCompleteListener(
                        OnCompleteListener { task: Task<AuthResult?> ->
                            if (task.isSuccessful) {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    if (user.isEmailVerified) {
                                        dialogSignIn.dismiss()
                                        db.collection("Users").document(auth.currentUser!!.uid).get()
                                            .addOnSuccessListener { document ->
                                                if (document.get("username") == null || document.get("username")
                                                        .toString().isEmpty()
                                                ) {
                                                    dialogUsernameSetUp()
                                                } else
                                                    Toast.makeText(this@SpotReviewActivity, getString(R.string.login_completed) + document.get("username").toString(), Toast.LENGTH_SHORT).show()
                                            }.addOnFailureListener {
                                                Toast.makeText(this@SpotReviewActivity, getString(R.string.username_check_error), Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        user.sendEmailVerification()
                                            .addOnCompleteListener {
                                                loadingLogin.setVisibility(View.GONE)
                                                loginErrorMessage.setText(getString(R.string.you_need_to_verify_email))
                                                dialogLoginButton.setText(getString(R.string.sign_in))
                                            }
                                    }
                                }
                            } else {
                                loadingLogin.setVisibility(View.GONE)
                                loginErrorMessage.setText(getString(R.string.login_error))
                                dialogLoginButton.setText(getString(R.string.sign_in))
                            }
                        })
            }

        }

        val dialogLoginTabTitle = dialogSignIn.findViewById<TextView>(R.id.signin_tab_title)
        val dialogSignUpTabTitle = dialogSignIn.findViewById<TextView>(R.id.signup_tab_title)

        val dialogLoginTab = dialogSignIn.findViewById<LinearLayout>(R.id.signin_tab)
        val dialogSignupTab = dialogSignIn.findViewById<LinearLayout>(R.id.signup_tab)

        val slideLeftInAnimation =
            AnimationUtils.loadAnimation(baseContext, R.anim.slide_left_in_anim)
        val slideLeftOutAnimation =
            AnimationUtils.loadAnimation(baseContext, R.anim.slide_left_out_anim)
        val slideRightInAnimation =
            AnimationUtils.loadAnimation(baseContext, R.anim.slide_right_in_anim)
        val slideRightOutAnimation =
            AnimationUtils.loadAnimation(baseContext, R.anim.slide_right_out_anim)

        val signinTabTitleStr: String = getString(R.string.signin)
        val signInContentTab = SpannableString(signinTabTitleStr)
        signInContentTab.setSpan(UnderlineSpan(), 0, signinTabTitleStr.length, 0)

        val signUpTabTitleStr: String = getString(R.string.signup)
        val signUpContentTab = SpannableString(signUpTabTitleStr)
        signUpContentTab.setSpan(UnderlineSpan(), 0, signUpTabTitleStr.length, 0)

        if (nightRideModeOn) {
            val colorSpan = ForegroundColorSpan(getColor(R.color.light_grey))
            dialogSignIn.findViewById<CardView>(R.id.main_background_signin_dialog).setBackgroundResource(R.drawable.round_back_dark_20)
            val loginEmailEditText = dialogSignIn.findViewById<EditText>(R.id.login_email)
            loginEmailEditText.setTextColor(getColor(R.color.lighter_grey))
            loginEmailEditText.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            val loginPasswordEditText = dialogSignIn.findViewById<EditText>(R.id.login_password)
            loginPasswordEditText.setTextColor(getColor(R.color.lighter_grey))
            loginPasswordEditText.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            val signUpEmailEditText = dialogSignIn.findViewById<EditText>(R.id.signup_email)
            signUpEmailEditText.setTextColor(getColor(R.color.lighter_grey))
            signUpEmailEditText.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            val signUpPasswordEditText = dialogSignIn.findViewById<EditText>(R.id.signup_password)
            signUpPasswordEditText.setTextColor(getColor(R.color.lighter_grey))
            signUpPasswordEditText.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            val signUpConfirmPasswordEditText = dialogSignIn.findViewById<EditText>(R.id.signup_confirm_password)
            signUpConfirmPasswordEditText.setTextColor(getColor(R.color.lighter_grey))
            signUpConfirmPasswordEditText.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            dialogLoginTabTitle.setTextColor(getColor(R.color.cool_teal))
        }

        dialogLoginTabTitle.setText(signInContentTab)

        dialogLoginTabTitle.setOnClickListener {
            if (!onLeftTab) {
                dialogLoginTabTitle.setText(signInContentTab)
                dialogSignUpTabTitle.setText(signUpTabTitleStr)
                if (nightRideModeOn)
                    dialogLoginTabTitle.setTextColor(getColor(R.color.cool_teal))
                else
                    dialogLoginTabTitle.setTextColor(getColor(R.color.black))
                dialogSignUpTabTitle.setTextColor(getColor(R.color.light_grey))
                dialogSignupTab.startAnimation(slideRightOutAnimation)
                dialogLoginTab.visibility = View.VISIBLE
                dialogLoginTab.startAnimation(slideRightInAnimation)
                dialogSignupTab.visibility = View.GONE
            }
            onLeftTab = true
        }

        dialogSignUpTabTitle.setOnClickListener {
            if (onLeftTab) {
                dialogSignUpTabTitle.setText(signUpContentTab)
                dialogLoginTabTitle.setText(signinTabTitleStr)
                dialogLoginTabTitle.setTextColor(getColor(R.color.light_grey))
                if (nightRideModeOn)
                    dialogSignUpTabTitle.setTextColor(getColor(R.color.cool_teal))
                else
                    dialogSignUpTabTitle.setTextColor(getColor(R.color.black))
                dialogLoginTab.startAnimation(slideLeftOutAnimation)
                dialogSignupTab.visibility = View.VISIBLE
                dialogSignupTab.startAnimation(slideLeftInAnimation)
                dialogLoginTab.visibility = View.GONE
            }
            onLeftTab = false
        }

        dialogSignUpButton.setOnClickListener {
            val signUpEmailField = dialogSignIn.findViewById<EditText>(R.id.signup_email)
            val signUpPasswordField = dialogSignIn.findViewById<EditText>(R.id.signup_password)
            val signUpConfirmPasswordField =
                dialogSignIn.findViewById<EditText>(R.id.signup_confirm_password)

            val emailQuery = signUpEmailField.text.toString()
            val passwordQuery = signUpPasswordField.text.toString()
            val confirmPasswordQuery = signUpConfirmPasswordField.text.toString()

            val signUpErrorMessage = dialogSignIn.findViewById<TextView>(R.id.sign_up_error_message)
            signUpErrorMessage.setTextColor(getColor(R.color.red))

            //Edit text query change listeners:
            signUpEmailField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // This method is called before the text is changed.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text is changed. You can perform actions here.
                    signUpErrorMessage.setText("")
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })

            signUpPasswordField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // This method is called before the text is changed.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text is changed. You can perform actions here.
                    signUpErrorMessage.setText("")
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })

            signUpConfirmPasswordField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // This method is called before the text is changed.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text is changed. You can perform actions here.
                    signUpErrorMessage.setText("")
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })


            //Password check:
            if (emailQuery.isEmpty() || passwordQuery.isEmpty() || confirmPasswordQuery.isEmpty())
                signUpErrorMessage.setText(getString(R.string.all_fields_are_compulsory))
            else {
                if (!passwordQuery.equals(confirmPasswordQuery)) {
                    signUpErrorMessage.setText(getString(R.string.passwords_dont_match))
                } else {
                    var passwordCyrillicFound = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        passwordCyrillicFound = passwordQuery.chars()
                            .mapToObj<UCharacter.UnicodeBlock>(IntFunction { ch: Int ->
                                UCharacter.UnicodeBlock.of(
                                    ch
                                )
                            })
                            .anyMatch(Predicate { b: UCharacter.UnicodeBlock -> b == UCharacter.UnicodeBlock.CYRILLIC })
                    }

                    var passwordNoNumbersFound = true
                    for (i in 0 until passwordQuery.length) {
                        if (Character.isDigit(passwordQuery.get(i))) {
                            passwordNoNumbersFound = false
                            break
                        }
                    }

                    var passwordDoesntContainLetters = true
                    for (i in 0 until passwordQuery.length) {
                        if (Character.isLetter(passwordQuery.get(i))) {
                            passwordDoesntContainLetters = false
                            break
                        }
                    }

                    val passwordWrongLength: Boolean = passwordQuery.length < 7

                    if (passwordCyrillicFound || passwordNoNumbersFound || passwordDoesntContainLetters || passwordWrongLength) {
                        signUpErrorMessage.setText(getString(R.string.password_restrictions))
                    } else {
                        val loadingSignUp =
                            dialogSignIn.findViewById<ProgressBar>(R.id.loading_sign_up)
                        loadingSignUp.visibility = View.VISIBLE
                        //show loading
                        dialogSignUpButton.setText("")
                        //Works pretty slow, so that further code starts working without isLoginTaken change

                        //Email availability check:
                        val isEmailTaken = booleanArrayOf(false)
                        db.collection("Users").get()
                            .addOnCompleteListener(OnCompleteListener { task11: Task<QuerySnapshot> ->
                                if (task11.isSuccessful) {
                                    for (document in task11.result) {
                                        if (document["email"] == emailQuery) {
                                            isEmailTaken[0] = true
                                            loadingSignUp.setVisibility(View.GONE)
                                            dialogSignUpButton.setText(getString(R.string.sign_up))
                                            signUpErrorMessage.setText(getString(R.string.email_taken))
                                        }
                                    }
                                }
                                if (!isEmailTaken[0]) {
                                    if (!isEmailTaken[0]) {
                                        auth.createUserWithEmailAndPassword(
                                            emailQuery,
                                            passwordQuery
                                        ).addOnCompleteListener(
                                            OnCompleteListener { task1111: Task<AuthResult?> ->
                                                if (task1111.isSuccessful) {
                                                    val user = hashMapOf(
                                                        "email" to emailQuery
                                                    )
                                                    db.collection("Users")
                                                        .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                                        .set(user)
                                                        .addOnSuccessListener(
                                                            OnSuccessListener { documentReference: Void? -> sendEmailVerification() })
                                                        .addOnFailureListener(OnFailureListener { e: Exception? ->
                                                            dialogSignUpButton.setText(getString(R.string.sign_up))
                                                            signUpErrorMessage.setText(getString(R.string.signup_error))
                                                            loadingSignUp.setVisibility(View.GONE)
                                                        })
                                                } else {
                                                    dialogSignUpButton.setText(getString(R.string.sign_up))
                                                    signUpErrorMessage.setText(getString(R.string.signup_error))
                                                    loadingSignUp.setVisibility(View.GONE)
                                                }
                                            })
                                    }
                                }
                            })
                    }
                }
            }

        }

        val forgotPasswordClickable = dialogSignIn.findViewById<TextView>(R.id.forgot_password_clickable)
        val forgotPasswordSpannable: Spannable =
            SpannableString(forgotPasswordClickable.text.toString())
        val clickSpan: ClickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                if (nightRideModeOn)
                    ds.color = getColor(R.color.light_grey)
                else
                    ds.color = getColor(R.color.black)
            }

            override fun onClick(view: View) {
                val emailField = dialogSignIn.findViewById<EditText>(R.id.login_email)
                val emailQuery = emailField.text.toString()
                val loginErrorMessage = dialogSignIn.findViewById<TextView>(R.id.login_error_message)

                emailField.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // This method is called before the text is changed.
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        // This method is called when the text is changed. You can perform actions here.
                        loginErrorMessage.setText("")
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // This method is called after the text is changed.
                    }
                })

                if (emailQuery.isEmpty())
                    loginErrorMessage.setText(getString(R.string.enter_email))
                else {
                    loginErrorMessage.setTextColor(getColor(R.color.red))
                    val loadingForgotPassword = dialogSignIn.findViewById<ProgressBar>(R.id.loading_forgot_password)
                    loadingForgotPassword.visibility = View.VISIBLE
                    forgotPasswordClickable.visibility = View.GONE
                    // Perform the query asynchronously
                    val queryTask = db.collection("Users").get()

                    queryTask.addOnSuccessListener { documents ->
                        var emailExists = false

                        for (document in documents) {
                            if (document.getString("email") == emailQuery) {
                                emailExists = true
                                break
                            }
                        }

                        if (emailExists) {
                            auth.sendPasswordResetEmail(emailQuery)
                                .addOnCompleteListener { task ->
                                    loadingForgotPassword.setVisibility(View.GONE)
                                    forgotPasswordClickable.visibility = View.VISIBLE
                                    if (task.isSuccessful) {
                                        loginErrorMessage.setText(getString(R.string.recovery_sent))
                                        loginErrorMessage.setTextColor(getColor(R.color.green))
                                    } else {
                                        loginErrorMessage.setText(getString(R.string.error_occurred))
                                    }
                                }
                        } else {
                            loadingForgotPassword.setVisibility(View.GONE)
                            forgotPasswordClickable.visibility = View.VISIBLE
                            loginErrorMessage.setText(getString(R.string.password_reset_invalid_email))
                        }
                    }

                }
            }
        }
        forgotPasswordSpannable.setSpan(
            clickSpan,
            0,
            getString(R.string.forgot_password).length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        forgotPasswordClickable.setText(forgotPasswordSpannable, TextView.BufferType.SPANNABLE)
        forgotPasswordClickable.movementMethod = LinkMovementMethod.getInstance()


        dialogSignIn.show()
        val window = dialogSignIn.window
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialogSignIn.window!!.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window!!.attributes = lp
    }

    private fun sendEmailVerification() {
        val firebaseUser: FirebaseUser = auth.getCurrentUser()!!
        firebaseUser.sendEmailVerification().addOnCompleteListener { task: Task<Void?>? ->
            val dialogSignUpButton = dialogSignIn.findViewById<AppCompatButton>(R.id.sign_up_button)
            val dialogSignUpLoading = dialogSignIn.findViewById<ProgressBar>(R.id.loading_sign_up)
            val dialogSignUpErrorMessage =
                dialogSignIn.findViewById<TextView>(R.id.sign_up_error_message)
            dialogSignUpButton.setText(getString(R.string.sign_up))
            dialogSignUpErrorMessage.setText(getString(R.string.verification_sent))
            dialogSignUpErrorMessage.setTextColor(getColor(R.color.green))
            //add the password recovery email sent animation
            auth.signOut()
            dialogSignUpLoading.setVisibility(View.GONE)
        }
    }

    /*private fun googleSignInDialogShowup(bookmarkClicked: Boolean, deleteClicked: Boolean) {
        dialogGoogleSignIn = Dialog(this)
        dialogGoogleSignIn.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogGoogleSignIn.setCancelable(true)
        dialogGoogleSignIn.setContentView(R.layout.dialog_google_sign_in)
        if (bookmarkClicked) {
            dialogGoogleSignIn.findViewById<TextView>(R.id.dialog_content).setText(getString(R.string.you_need_to_sign_in_to_bookmark))
        }
        if (deleteClicked) {
            dialogGoogleSignIn.findViewById<TextView>(R.id.dialog_content).setText(getString(R.string.you_need_to_sign_in_to_delete))
        }
        dialogGoogleSignIn.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogGoogleSignInBtn = dialogGoogleSignIn.findViewById<MaterialButton>(R.id.login_button)
        dialogGoogleSignInBtn.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            val options =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            var client = GoogleSignIn.getClient(this, options)
            val intent = client.signInIntent
            startActivityForResult(intent, 10001)
        }
        dialogGoogleSignIn.show()
    }*/


    private fun downloadImages(imageUrls: List<String>) {
        val imageDownloadCount = AtomicInteger(0)

        for (imageUrl in imageUrls) {
            Thread {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val inputStream: InputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    // Add the downloaded image to the imagesList
                    imagesList.add(bitmap)

                    // Check if all images are downloaded
                    if (imageDownloadCount.incrementAndGet() == imageUrls.size) {
                        runOnUiThread {
                            viewPager2.adapter = ViewPagerAdapter(imagesList)
                            loading.visibility = View.GONE
                            mainScrollView.visibility = View.VISIBLE
                            goLayout.visibility = View.VISIBLE

                            editBtn.setOnClickListener {
                                //TODO: Check if the user's signed in:
                                if (FirebaseAuth.getInstance().currentUser == null) {
                                    Toast.makeText(this@SpotReviewActivity, getString(R.string.you_need_to_sign_in_to_edit), Toast.LENGTH_LONG).show()
                                    signInDialogShowup()
                                } else {
                                    val intent =
                                        Intent(this@SpotReviewActivity, AddSpotActivity::class.java)
                                    intent.putExtra("fromSpotReviewActivity", true)
                                    intent.putExtra("title", title)
                                    intent.putExtra("description", description)
                                    intent.putExtra("condition", condition)
                                    intent.putExtra("type", type)
                                    intent.putExtra("date", date)
                                    intent.putExtra("proponent", proponent)
                                    intent.putExtra("imagesList.size", imagesList.size)
                                    intent.putExtra("latitude", latitude)
                                    intent.putExtra("longitude", longitude)

// Save images to temporary files and pass their URIs
                                    val imageUris = ArrayList<Uri>()
                                    for (i in 0 until imagesList.size) {
                                        val imageFile = File(getCacheDir(), "image${i + 1}.png")
                                        val stream = FileOutputStream(imageFile)
                                        imagesList[i].compress(
                                            Bitmap.CompressFormat.PNG,
                                            100,
                                            stream
                                        )
                                        stream.close()
                                        imageUris.add(
                                            FileProvider.getUriForFile(
                                                this,
                                                "${packageName}.provider",
                                                imageFile
                                            )
                                        )
                                    }

                                    intent.putParcelableArrayListExtra("imageUris", imageUris)

                                    startActivity(intent)
                                }
                            }

                            val deleteBtn = findViewById<MaterialButton>(R.id.delete_button)
                            deleteBtn.setOnClickListener {
                                if (FirebaseAuth.getInstance().currentUser == null) {
                                    Toast.makeText(this@SpotReviewActivity, getString(R.string.you_need_to_sign_in_to_delete), Toast.LENGTH_LONG).show()
                                    signInDialogShowup()
                                } else {
                                    val dialog = Dialog(this)
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setCancelable(true)
                                    dialog.setContentView(R.layout.dialog_delete_confirm)
                                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                    val dialogContent = dialog.findViewById<TextView>(R.id.dialog_content)

                                    if (pref.getBoolean("NightRideMode", false)) {
                                        dialogContent.setTextColor(getColor(R.color.lighter_grey))
                                        dialog.findViewById<CardView>(R.id.main_background_delete_confirm).setBackgroundResource(R.drawable.round_back_dark_lighter_20)
                                    }

                                    val dialogLoading = dialog.findViewById<ProgressBar>(R.id.loading_progress_bar)
                                    val buttonYes = dialog.findViewById<AppCompatButton>(R.id.button_yes)
                                    val buttonNo = dialog.findViewById<AppCompatButton>(R.id.button_no)
                                    val buttonOk = dialog.findViewById<AppCompatButton>(R.id.button_ok)
                                    buttonNo.setOnClickListener {
                                        dialog.dismiss()
                                    }
                                    buttonYes.setOnClickListener {
                                        dialogLoading.visibility = View.VISIBLE
                                        buttonYes.visibility = View.GONE
                                        val spotTitle = hashMapOf(
                                            "title" to title
                                        )
                                        db.collection("ToDelete").document(latitude.toString() + longitude.toString())
                                            .set(spotTitle).addOnSuccessListener {
                                                buttonYes.visibility = View.GONE
                                                buttonNo.visibility = View.GONE
                                                buttonOk.visibility = View.VISIBLE
                                                dialogLoading.visibility = View.GONE
                                                dialogContent.setText(getString(R.string.delete_suggestion_sent))
                                                buttonOk.setOnClickListener { dialog.dismiss() }
                                            }
                                    }
                                    dialog.show()
                                }
                            }

                        }
                    }
                } catch (e: Exception) {
                    // Handle image download failure
                    e.printStackTrace()
                }
            }.start()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == 10001) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection("Users").document(auth.currentUser!!.uid).get()
                                .addOnSuccessListener { document ->
                                    val username = document.get("username")
                                    if (username == null || username
                                            .toString().isEmpty()
                                    ) {
                                        dialogGoogleSignIn.dismiss()
                                        dialogUsernameSetUp()
                                    } else {
                                        dialogGoogleSignIn.dismiss()
                                        Toast.makeText(baseContext, getString(R.string.you_successfully_signed_in_as)+username, Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener {
                                    dialogGoogleSignIn.dismiss()
                                    dialogUsernameSetUp()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "onActivity result issue" + task.exception?.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        } catch (_: ApiException) {
        }
    }


    private fun dialogUsernameSetUp() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_enter_username)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogBtnConfirm = dialog.findViewById<AppCompatButton>(R.id.button_confirm)
        val dialogUsernameField = dialog.findViewById<EditText>(R.id.username_field)
        val dialogLoading = dialog.findViewById<ProgressBar>(R.id.loading_progress_bar)
        val dialogErrorMessage = dialog.findViewById<TextView>(R.id.error_message)
        val mainLayout = dialog.findViewById<LinearLayout>(R.id.main_layout)
        val successLayout = dialog.findViewById<LinearLayout>(R.id.success_layout)
        val okBtn = dialog.findViewById<AppCompatButton>(R.id.button_ok)

        if (pref.getBoolean("NightRideMode", false)) {
            dialog.findViewById<TextView>(R.id.dialog_content).setTextColor(getColor(R.color.lighter_grey))
            dialog.findViewById<CardView>(R.id.main_background_username_setup).setBackgroundResource(R.drawable.round_back_dark_20)
            dialogUsernameField.setTextColor(getColor(R.color.lighter_grey))
            dialogUsernameField.setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            dialog.findViewById<TextView>(R.id.success_text_view).setTextColor(getColor(R.color.lighter_grey))
        }

        okBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialogBtnConfirm.setOnClickListener {
            dialogErrorMessage.text = ""
            if (dialogUsernameField.text.isBlank()) {
                dialogErrorMessage.text = getString(R.string.field_cannot_be_empty)
            } else if (dialogUsernameField.text.length > 20) {
                dialogErrorMessage.text = getString(R.string.username_length_exceeded)
            } else if (dialogUsernameField.text.toString().equals("?")) {
                dialogErrorMessage.text = getString(R.string.username_cannot_be_question_mark)
            } else {
                var usernameEntered = dialogUsernameField.text.toString()
                var usernameTaken = false
                dialogBtnConfirm.visibility = View.GONE
                dialogLoading.visibility = View.VISIBLE
                db.collection("Users").get().addOnSuccessListener { documents ->
                    Log.d("asasassin", "checking if the name is taken")
                    for (document in documents) {
                        if (usernameEntered.equals(document.get("username"))) {
                            usernameTaken = true
                            break
                        }
                    }
                    if (usernameTaken) {
                        dialogErrorMessage.text = getString(R.string.given_username_already_taken)
                        dialogBtnConfirm.visibility = View.VISIBLE
                        dialogLoading.visibility = View.GONE
                    } else {
                        val data = hashMapOf("username" to usernameEntered)
                        db.collection("Users").document(auth.currentUser!!.uid).update(data as Map<String, Any>)
                            .addOnSuccessListener {
                                mainLayout.visibility = View.GONE
                                successLayout.visibility = View.VISIBLE
                            }.addOnFailureListener {
                                //TODO: Failed to get access to firestore Users collection
                            }
                    }
                }.addOnFailureListener {
                    if (usernameTaken) {
                        dialogErrorMessage.text = getString(R.string.given_username_already_taken)
                    } else {
                        Log.d("asasassin", "asfega")
                        dialogBtnConfirm.visibility = View.GONE
                        dialogLoading.visibility = View.VISIBLE
                        val data = hashMapOf("username" to usernameEntered)
                        db.collection("Users").document(auth.currentUser!!.uid).update(data as Map<String, Any>)
                            .addOnSuccessListener {
                                mainLayout.visibility = View.GONE
                                successLayout.visibility = View.VISIBLE
                            }.addOnFailureListener {
                                //TODO: Failed to get access to firestore Users collection
                            }
                    }
                }
            }
        }
        Log.d("iamhere", "it should appear now")
        dialog.show()
    }

}