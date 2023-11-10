package com.tricktrack.tricktrack

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.lang.UCharacter
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.tricktrack.tricktrack.internet_connection.Common
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider
import java.lang.Math.*
import java.util.*
import kotlin.math.pow


class MapsActivity : AppCompatActivity(), ActivityResultCaller, AdapterView.OnItemClickListener {

    private var mapView: MapView? = null
    private var zoomValue: Float = 12f
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationOnMapKit: UserLocationLayer
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var favoriteSpotsIcon: ImageView
    private lateinit var nearbySpotsIcon: ImageView
    private lateinit var addSpotIcon: ImageView
    private lateinit var codeOfConductIcon: ImageView
    private lateinit var accountIcon: ImageView
    private lateinit var bottomSheetDialogLinearLayout: LinearLayout
    private var addIconPressed = false
    private var favoriteSpotsIconPressed = false
    private var nearbySpotsIconPressed = false
    private var codeOfConductIconPressed = false
    private var accountIconPressed = false
    private val coolTealColor = Color.rgb(0, 120, 140)
    private val lightGreyColor = Color.rgb(155, 155, 155)
    private lateinit var auth: FirebaseAuth
    private lateinit var client: GoogleSignInClient
    private lateinit var bottomSheetDialogTitle: TextView
    private lateinit var bottomSheetDialogDetails: TextView
    private lateinit var addSpotBtn: AppCompatButton
    private val db = Firebase.firestore
    private val spotsCollection = db.collection("Spots")
    private lateinit var spotsLoadingLinearLayout: LinearLayout
    private var processedDocumentCount = 0
    private lateinit var mapObjectTapListener: MapObjectTapListener
    private lateinit var spotLinkConstraintLayout: ConstraintLayout
    private lateinit var spotLinkTitle: TextView
    private lateinit var zoomFab: MaterialButton
    private lateinit var cancelSpotLinkButton: ImageButton
    private var mapObjectTapListenerPointlessArray = arrayListOf<MapObjectTapListener>()
    private lateinit var inputListener: InputListener
    private lateinit var dialogSignIn: Dialog
    private var googleSignInDialogDismiss = false
    private lateinit var nothingInFavoritesTV: TextView
    private lateinit var bottomSheetDialogNearby: LinearLayout
    private lateinit var loadingNearby: ProgressBar
    private lateinit var nearbyRecyclerView: RecyclerView
    private lateinit var searchOutputRecyclerView: RecyclerView
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var bottomSheetDialogAccount: LinearLayout
    private lateinit var bottomSheetDialogAccountTitle: TextView
    private lateinit var googleSignInButtonAccount: AppCompatButton
    private lateinit var signOutBtn: MaterialButton
    private lateinit var deleteAccountBtn: MaterialButton
    private lateinit var usernameBtn: MaterialButton
    private lateinit var loadingAccount: ProgressBar
    private lateinit var bottomSheetDialogCodeOfConduct: LinearLayout
    private var connectionRecoveredRecreate = false
    private lateinit var bottomSheetDialogFavorites: LinearLayout
    private lateinit var searchView: SearchView
    private var diceModeChosen = ""
    private lateinit var dialogDice: Dialog
    private lateinit var loadingFavorites: ProgressBar
    private lateinit var loadingDice: ProgressBar
    private lateinit var loadingDiceOutput: ProgressBar
    private lateinit var dialogBtnConfirm: AppCompatButton
    private lateinit var dialogBtnAgain: AppCompatButton
    private lateinit var diceDialogErrorMessage: TextView
    private var onLeftTab = true
    private lateinit var pref: SharedPreferences
    private lateinit var diceBtn: MaterialButton
    private lateinit var feedbackButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var iconsLinearLayout: LinearLayout
    private lateinit var searchOutputLinearLayout: LinearLayout

    //Translation:
    private val languagesTranslationCodes = arrayOf("en", "ru")
    private val conditions = DownloadConditions.Builder().requireWifi().build()

    init {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val noInternetLinear = findViewById<LinearLayout>(R.id.no_internet_linear_layout)
                if (!Common.isConnectedToInternet(baseContext)) {
                    noInternetLinear.visibility = View.VISIBLE
                    connectionRecoveredRecreate = true
                } else {
                    noInternetLinear.visibility = View.GONE
                    if (connectionRecoveredRecreate) {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.connection_recovered),
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MapsActivity, MapsActivity::class.java))
                        finish()
                    }
                    connectionRecoveredRecreate = false
                }
            }
        }
    }

    object MapKitInitializer {

        private var initialized = false

        fun initialize(apiKey: String, context: Context) {
            if (initialized) {
                return
            }

            MapKitFactory.setApiKey(apiKey)
            MapKitFactory.initialize(context)
            initialized = true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        MapKitFactory.setApiKey("c9699657-bdd5-40a8-9cfc-bc2ce1576146")
//        MapKitFactory.initialize(this)
        MapKitInitializer.initialize("c9699657-bdd5-40a8-9cfc-bc2ce1576146", this)

        setContentView(R.layout.activity_maps)

        pref = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim)
        val slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_anim)
        val slideDownOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_open_anim)
        val slideUpOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_open_anim)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_anim)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out_anim)

        iconsLinearLayout = findViewById(R.id.icons_linear_layout)

        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)

        searchView = findViewById(R.id.search_view)

        val searchIconId = resources.getIdentifier("android:id/search_mag_icon", null, null)
        val searchIcon = searchView.findViewById(searchIconId) as ImageView
        searchIcon.setImageResource(android.R.color.transparent)

        val closeIconId = resources.getIdentifier("android:id/search_close_btn", null, null)
        val closeIcon = searchView.findViewById(closeIconId) as ImageView
        closeIcon.setImageResource(android.R.color.transparent)


        bottomSheetDialogFavorites = findViewById(R.id.bottom_sheet_dialog_favorites)

        spotsLoadingLinearLayout = findViewById(R.id.spots_loading_linear_layout)
        spotLinkConstraintLayout = findViewById(R.id.spot_link_constraint_layout)
        spotLinkTitle = findViewById(R.id.spot_link_title)
        cancelSpotLinkButton = findViewById(R.id.cancel_spot_link_image_button)
        bottomSheetDialogNearby = findViewById(R.id.bottom_sheet_dialog_nearby)
        loadingNearby = findViewById(R.id.loading_nearby)
        nearbyRecyclerView = findViewById(R.id.nearby_spots_recycler_view)
        searchOutputRecyclerView = findViewById(R.id.search_output_recycler_view)
        bottomSheetDialogAccountTitle = findViewById(R.id.bottom_sheet_dialog_title_account)
        googleSignInButtonAccount = findViewById(R.id.google_sign_in_button_account)
        signOutBtn = findViewById(R.id.sign_out_button)
        deleteAccountBtn = findViewById(R.id.delete_account_button)
        bottomSheetDialogAccount = findViewById(R.id.bottom_sheet_dialog_account)
        usernameBtn = findViewById(R.id.username_button)
        loadingAccount = findViewById(R.id.loading_account)
        bottomSheetDialogCodeOfConduct = findViewById(R.id.bottom_sheet_dialog_code_of_conduct)



        signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                db.collection("Users").document(auth.currentUser!!.uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document.get("username") == null || document.get("username")
                                                .toString().isEmpty()
                                        ) {
                                            dialogUsernameSetUp(false)
                                        } else {
                                            if (googleSignInDialogDismiss) {
                                                dialogSignIn.dismiss()
                                                zoomFab.visibility = View.VISIBLE
                                            } else {
                                                val i = Intent(this, MapsActivity::class.java)
                                                startActivity(i)
                                                finish()
                                            }
                                        }
                                    }.addOnFailureListener {
                                        dialogUsernameSetUp(false)
                                    }
                            } else {
                                //here's basically where logs show up
                            }
                        }
                } else {
//                Toast.makeText(
//                    this,
//                    "Sign-in failed: ${result}",
//                    Toast.LENGTH_SHORT
//                ).show()
                }
            }


        mapView = findViewById(R.id.mapview)
        favoriteSpotsIcon = findViewById(R.id.favorite_spots_icon)
        nearbySpotsIcon = findViewById(R.id.nearby_spots_icon)
        addSpotIcon = findViewById(R.id.add_spot_icon)
        codeOfConductIcon = findViewById(R.id.code_of_conduct_icon)
        accountIcon = findViewById(R.id.account_icon)
        bottomSheetDialogTitle = findViewById(R.id.bottom_sheet_dialog_title)
        bottomSheetDialogDetails = findViewById(R.id.bottom_sheet_dialog_details)
        bottomSheetDialogLinearLayout = findViewById(R.id.bottom_sheet_dialog_linear_layout)
        addSpotBtn = findViewById(R.id.add_spot_button)
        nothingInFavoritesTV = findViewById(R.id.nothing_in_favorites_text_view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapKit: MapKit = MapKitFactory.getInstance()
        locationOnMapKit = mapView?.mapWindow?.let { mapKit.createUserLocationLayer(it) }!!
        locationOnMapKit.isVisible = true

        zoomFab = findViewById(R.id.zoom_fab)
        zoomFab.setOnClickListener {
            searchView.clearFocus()
            // Check for location permission and availability when the button is clicked
            checkLocationPermissionAndAvailability()
        }

        cancelSpotLinkButton.setOnClickListener {
            searchView.clearFocus()
            zoomFab.visibility = View.VISIBLE
            spotLinkConstraintLayout.visibility = View.GONE
            spotLinkTitle.text = ""
        }

        /*// Check if permission is not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }*/

        mapView!!.map.logo.setAlignment(Alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP))

        checkLocationPermissionAndAvailability()

        auth = FirebaseAuth.getInstance()
        val options =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        client = GoogleSignIn.getClient(this, options)

        findViewById<AppCompatButton>(R.id.sign_in_dialog_open).setOnClickListener {   //add spot sign in button
            searchView.clearFocus()
            signInDialogShowup()
        }

        bottomSheetDialogFavorites.setOnClickListener { searchView.clearFocus() }
        bottomSheetDialogNearby.setOnClickListener {
            clearSearchViewQuery()
        }
        bottomSheetDialogLinearLayout.setOnClickListener {
            clearSearchViewQuery()
        }
        bottomSheetDialogCodeOfConduct.setOnClickListener {
            clearSearchViewQuery()
        }
        bottomSheetDialogAccount.setOnClickListener {
            clearSearchViewQuery()
        }



        favoriteSpotsIcon.setOnClickListener {
            if (!favoriteSpotsIconPressed) {
                clearSearchViewQuery()
                bottomSheetDialogCodeOfConduct.visibility = View.GONE
                bottomSheetDialogAccount.visibility = View.GONE
                bottomSheetDialogNearby.visibility = View.GONE
                bottomSheetDialogLinearLayout.visibility = View.GONE
                loadingFavorites = findViewById(R.id.loading_favorites)
                nothingInFavoritesTV.visibility = View.GONE
                nearbySpotsIconPressed = false
                codeOfConductIconPressed = false
                accountIconPressed = false
                addIconPressed = false
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(lightGreyColor)
                accountIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(lightGreyColor)
                favoriteSpotsDisplay(false)
            } else {
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogFavorites.visibility =
                    View.GONE
                bottomSheetDialogFavorites.startAnimation(slideDownAnimation)
                zoomFab.visibility = View.VISIBLE
                favoriteSpotsIconPressed = false
            }
        }


        nearbySpotsIcon.setOnClickListener {
            if (!nearbySpotsIconPressed) {
                clearSearchViewQuery()
                bottomSheetDialogAccount.visibility = View.GONE
                favoriteSpotsIconPressed = false
                addIconPressed = false
                codeOfConductIconPressed = false
                accountIconPressed = false
                nearbySpotsIcon.setColorFilter(coolTealColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(lightGreyColor)
                accountIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE
                bottomSheetDialogCodeOfConduct.visibility = View.GONE
                zoomFab.visibility = View.GONE
                bottomSheetDialogNearby.visibility = View.VISIBLE
                bottomSheetDialogNearby.startAnimation(slideUpAnimation)
                spotLinkConstraintLayout.visibility = View.GONE
                bottomSheetDialogFavorites.visibility = View.GONE
                nearbySpotsIconPressed = true
                loadingNearby.visibility = View.VISIBLE

                nearbySpotsDisplay(false, 10)

            } else {
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogNearby.visibility = View.GONE
                bottomSheetDialogNearby.startAnimation(slideDownAnimation)
                zoomFab.visibility = View.VISIBLE
                nearbySpotsIconPressed = false
            }
        }


        addSpotBtn.setOnClickListener {
            val intent = Intent(this@MapsActivity, AddSpotActivity::class.java)
            startActivity(intent)
        }

        addSpotIcon.setOnClickListener {
            if (!addIconPressed) {
                clearSearchViewQuery()
                bottomSheetDialogCodeOfConduct.visibility = View.GONE
                bottomSheetDialogNearby.visibility = View.GONE
                bottomSheetDialogAccount.visibility = View.GONE
                if (FirebaseAuth.getInstance().currentUser != null) {
                    findViewById<AppCompatButton>(R.id.sign_in_dialog_open).visibility = View.GONE
                    bottomSheetDialogDetails.visibility = View.GONE
                    addSpotBtn.visibility = View.VISIBLE
                } else {
                    googleSignInDialogDismiss = false
                    findViewById<AppCompatButton>(R.id.sign_in_dialog_open).visibility =
                        View.VISIBLE
                    bottomSheetDialogDetails.visibility = View.VISIBLE
                    addSpotBtn.visibility = View.GONE
                }
                favoriteSpotsIconPressed = false
                nearbySpotsIconPressed = false
                codeOfConductIconPressed = false
                accountIconPressed = false
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(lightGreyColor)
                accountIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(coolTealColor)
                bottomSheetDialogLinearLayout.visibility = View.VISIBLE
                bottomSheetDialogLinearLayout.startAnimation(slideUpAnimation)
                zoomFab.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                bottomSheetDialogFavorites.visibility = View.GONE
                addIconPressed = true
            } else {
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE
                bottomSheetDialogLinearLayout.startAnimation(slideDownAnimation)
                zoomFab.visibility = View.VISIBLE
                addIconPressed = false
            }
        }

        codeOfConductIcon.setOnClickListener {
            if (!codeOfConductIconPressed) {
                clearSearchViewQuery()
                bottomSheetDialogAccount.visibility = View.GONE
                favoriteSpotsIconPressed = false
                addIconPressed = false
                nearbySpotsIconPressed = false
                accountIconPressed = false
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(coolTealColor)
                accountIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE
                zoomFab.visibility = View.GONE
                bottomSheetDialogNearby.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                bottomSheetDialogFavorites.visibility = View.GONE
                bottomSheetDialogCodeOfConduct.visibility = View.VISIBLE
                bottomSheetDialogCodeOfConduct.startAnimation(slideUpAnimation)

                codeOfConductIconPressed = true
            } else {
                codeOfConductIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogCodeOfConduct.visibility = View.GONE
                bottomSheetDialogCodeOfConduct.startAnimation(slideDownAnimation)
                zoomFab.visibility = View.VISIBLE
                codeOfConductIconPressed = false
            }
        }

        accountIcon.setOnClickListener {
            if (!accountIconPressed) {
                clearSearchViewQuery()
                val spotsSuggestedTV = findViewById<TextView>(R.id.spots_suggested_text_view)
                spotsSuggestedTV.visibility = View.GONE
                favoriteSpotsIconPressed = false
                addIconPressed = false
                codeOfConductIconPressed = false
                nearbySpotsIconPressed = false
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(lightGreyColor)
                accountIcon.setColorFilter(coolTealColor)
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE
                bottomSheetDialogCodeOfConduct.visibility = View.GONE
                zoomFab.visibility = View.GONE
                bottomSheetDialogNearby.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                bottomSheetDialogFavorites.visibility = View.GONE
                loadingNearby.visibility = View.GONE
                bottomSheetDialogAccount.visibility = View.VISIBLE
                bottomSheetDialogAccount.startAnimation(slideUpAnimation)
                accountIconPressed = true

                if (FirebaseAuth.getInstance().currentUser == null) {
                    googleSignInButtonAccount.setOnClickListener {
                        signInDialogShowup()
                    }
                } else {
                    googleSignInButtonAccount.visibility = View.GONE
                    loadingAccount.visibility = View.VISIBLE
                    usernameBtn.visibility = View.GONE
                    signOutBtn.visibility = View.GONE
                    deleteAccountBtn.visibility = View.GONE
                    bottomSheetDialogAccountTitle.visibility = View.GONE
                    db.collection("Users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                        .get().addOnSuccessListener { document ->
                            loadingAccount.visibility = View.GONE
                            usernameBtn.visibility = View.VISIBLE
                            signOutBtn.visibility = View.VISIBLE
                            deleteAccountBtn.visibility = View.VISIBLE
                            if (document.get("username") == null) {
                                bottomSheetDialogAccountTitle.text = getString(R.string.account)
                                bottomSheetDialogAccountTitle.visibility = View.VISIBLE
                                usernameBtn.setOnClickListener {
                                    dialogUsernameSetUp(false)
                                }
                            } else {
                                bottomSheetDialogAccountTitle.text = document.get("username").toString()
                                bottomSheetDialogAccountTitle.visibility = View.VISIBLE
                                usernameBtn.text = getString(R.string.change_username)

                                usernameBtn.setOnClickListener {
                                    dialogUsernameSetUp(true)
                                }
                            }

                            val spotsSuggested = document.get("proposed").toString()
                            if (spotsSuggested.isEmpty() || spotsSuggested == "null") {
                                spotsSuggestedTV.text = getString(R.string.spots_suggested) + "0"
                            } else
                                spotsSuggestedTV.text =
                                    getString(R.string.spots_suggested) + spotsSuggested
                            spotsSuggestedTV.visibility = View.VISIBLE

                            signOutBtn.setOnClickListener {
                                signOutAndRevokeAccess()
                                val i = Intent(this@MapsActivity, MapsActivity::class.java)
                                startActivity(i)
                                finish()
                            }

                            deleteAccountBtn.setOnClickListener {
                                val dialogAccountDelete = Dialog(this)
                                dialogAccountDelete.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                dialogAccountDelete.setCancelable(true)
                                dialogAccountDelete.setContentView(R.layout.dialog_delete_account)
                                dialogAccountDelete.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                val dialogBtnNo = dialogAccountDelete.findViewById<AppCompatButton>(R.id.button_no)
                                dialogBtnNo.setOnClickListener {
                                    dialogAccountDelete.dismiss()
                                }
                                val dialogLoading = dialogAccountDelete.findViewById<ProgressBar>(R.id.loading_progress_bar)
                                val dialogContent = dialogAccountDelete.findViewById<TextView>(R.id.dialog_content)
                                val dialogBtnYes = dialogAccountDelete.findViewById<AppCompatButton>(R.id.button_yes)
                                val dialogBtnOk = dialogAccountDelete.findViewById<AppCompatButton>(R.id.button_ok)

                                if (pref.getBoolean("NightRideMode", false)) {
                                    dialogLoading.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
                                    dialogAccountDelete.findViewById<CardView>(R.id.main_background_delete_confirm).setBackgroundResource(R.drawable.round_back_dark_20)
                                    dialogContent.setTextColor(getColor(R.color.lighter_grey))
                                }

                                dialogBtnYes.setOnClickListener {
                                    dialogAccountDelete.setCancelable(false)
                                    dialogBtnYes.visibility = View.GONE
                                    dialogBtnNo.visibility = View.GONE
                                    dialogLoading.visibility = View.VISIBLE
                                    db.collection("Users").document(auth.currentUser!!.uid).delete()
                                        .addOnSuccessListener {
                                            auth.currentUser!!.delete().addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    dialogContent.text = getString(R.string.account_deleted_successfully)
                                                    dialogLoading.visibility = View.GONE
                                                    dialogBtnNo.visibility = View.GONE
                                                    dialogBtnYes.visibility = View.GONE
                                                    dialogBtnOk.visibility = View.VISIBLE
                                                    dialogAccountDelete.setCancelable(true)
                                                    closeAllBottomTabs()
                                                }
                                            }
                                        }.addOnFailureListener {
                                            dialogAccountDelete.dismiss()
                                            Toast.makeText(this@MapsActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                                        }
                                }

                                dialogBtnOk.setOnClickListener {
                                    dialogAccountDelete.dismiss()
                                    startActivity(Intent(this@MapsActivity, MapsActivity::class.java))
                                    finish()
                                }


                                dialogAccountDelete.show()
                            }

                        }
                }

            } else {
                accountIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogAccount.visibility = View.GONE
                bottomSheetDialogAccount.startAnimation(slideDownAnimation)
                zoomFab.visibility = View.VISIBLE
                accountIconPressed = false
            }
        }

        fetchAndProcessSpots()

        val searchCancelIcon = findViewById<ImageView>(R.id.close_icon)
        searchOutputLinearLayout = findViewById<LinearLayout>(R.id.search_output_linear_layout)
        val nothingFoundTV = findViewById<TextView>(R.id.nothing_found_text_view)
        val loadingSearchOutput = findViewById<ProgressBar>(R.id.loading_seach_output)
        val searchIconTV = findViewById<ImageView>(R.id.search_icon_text_view)
        var playSearchAnim = true

        searchCancelIcon.setOnClickListener {
            clearSearchViewQuery()
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Call your function when the SearchView gains focus
                onMapTap()
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isBlank()) {
                    searchCancelIcon.visibility = View.GONE
                    searchIconTV.startAnimation(fadeInAnimation)
                    searchIconTV.visibility = View.VISIBLE
                    loadingSearchOutput.startAnimation(fadeOutAnimation)
                    loadingSearchOutput.visibility = View.GONE
                    if (!playSearchAnim) {
                        searchOutputLinearLayout.visibility = View.GONE
                        searchOutputLinearLayout.startAnimation(slideUpOpenAnimation)
                        playSearchAnim = true
                    }
                } else {
                    searchIconTV.startAnimation(fadeOutAnimation)
                    searchIconTV.visibility = View.GONE
                    loadingSearchOutput.startAnimation(fadeInAnimation)
                    loadingSearchOutput.visibility = View.VISIBLE
                    nothingFoundTV.visibility = View.GONE
                    searchCancelIcon.visibility = View.VISIBLE
                    if (playSearchAnim) {
                        searchOutputLinearLayout.visibility = View.VISIBLE
                        searchOutputLinearLayout.startAnimation(slideDownOpenAnimation)
                        playSearchAnim = false
                    }
                    val adapter = NearbySpotsAdapter(this@MapsActivity, pref.getBoolean("NightRideMode", false)) { clickedItem ->
                        // Handle the item click here
                        val reviewIntent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
                        reviewIntent.putExtra("latitude", clickedItem.latitude)
                        reviewIntent.putExtra("longitude", clickedItem.longitude)
                        reviewIntent.putExtra("title", clickedItem.title)
                        reviewIntent.putExtra("type", clickedItem.type)
                        startActivity(reviewIntent)
                    }

                    val adapterFavorites = FavoriteSpotsAdapter(this@MapsActivity, pref.getBoolean("Translation", false), pref.getBoolean("NightRideMode", false)) {clickedItem ->
                        val reviewIntent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
                        reviewIntent.putExtra("latitude", clickedItem.latitude)
                        reviewIntent.putExtra("longitude", clickedItem.longitude)
                        reviewIntent.putExtra("title", clickedItem.title)
                        reviewIntent.putExtra("type", clickedItem.type)
                        startActivity(reviewIntent)
                    }
                    searchOutputRecyclerView.adapter = adapter
                    val layoutManager = LinearLayoutManager(this@MapsActivity)
                    searchOutputRecyclerView.layoutManager = layoutManager


                    lateinit var userLocation: Point
                    if (ActivityCompat.checkSelfPermission(
                            this@MapsActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient?.lastLocation?.addOnSuccessListener(this@MapsActivity) { location: Location? ->
                            // Move to the user's location with animation
                            if (location != null)
                                userLocation = Point(location.latitude, location.longitude)

                            val query = db.collection("Spots")

                            query.get()
                                .addOnSuccessListener { result ->
                                    val spotsFound = mutableListOf<NearbySpot>()
                                    var sortedByDistance = mutableListOf<NearbySpot>()

                                    val spotsFoundFavorite = mutableListOf<FavoriteSpot>()

                                    for (document in result) {
                                        val title = document.getString("title") ?: ""
                                        if (!title.lowercase().contains(newText.lowercase()))
                                            continue

                                        val latitude = document.getDouble("latitude") ?: 0.0
                                        val longitude = document.getDouble("longitude") ?: 0.0
                                        val type = document.getString("type") ?: ""

                                        var distance: Float
                                        if (location != null) {
                                            distance = calculateDistance(
                                                userLocation.latitude,
                                                userLocation.longitude,
                                                latitude,
                                                longitude
                                            )
                                            // Determine the correct icon based on the "type" field

                                            spotsFound.add(
                                                NearbySpot(
                                                    title,
                                                    latitude,
                                                    longitude,
                                                    type,
                                                    distance
                                                )
                                            )
                                        } else {
                                            spotsFoundFavorite.add(
                                                FavoriteSpot(
                                                    title,
                                                    latitude,
                                                    longitude,
                                                    type,
                                                    ""
                                                )
                                            )
                                        }

                                        if (location != null)
                                            sortedByDistance =
                                                spotsFound.sortedBy { it.distance }.toMutableList()
                                    }


                                    // Update the RecyclerView adapter with the data
                                    searchIconTV.startAnimation(fadeInAnimation)
                                    searchIconTV.visibility = View.VISIBLE
                                    loadingSearchOutput.startAnimation(fadeOutAnimation)
                                    loadingSearchOutput.visibility = View.GONE

                                    if (location != null) {
                                        if (sortedByDistance.isEmpty()) {
                                            nothingFoundTV.visibility = View.VISIBLE
                                        } else {
                                            nothingFoundTV.visibility = View.GONE
                                            searchOutputRecyclerView.adapter = adapter
                                            adapter.setData(sortedByDistance)
                                        }
                                    } else {
                                        if (spotsFoundFavorite.isEmpty())
                                            nothingFoundTV.visibility = View.VISIBLE
                                        else {
                                            nothingFoundTV.visibility = View.GONE
                                            searchOutputRecyclerView.adapter = adapterFavorites
                                            adapterFavorites.setData(spotsFoundFavorite)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    // Handle errors
                                }
                        }
                    }

                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })

        diceBtn = findViewById<MaterialButton>(R.id.dice_button)
        diceBtn.setOnClickListener {
            onMapTap()
            clearSearchViewQuery()

            dialogDice = Dialog(this)
            dialogDice.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogDice.setCancelable(true)
            dialogDice.setContentView(R.layout.dialog_dice)
            dialogDice.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogBtnConfirm = dialogDice.findViewById(R.id.button_confirm)
            diceDialogErrorMessage = dialogDice.findViewById(R.id.error_message)

            if (pref.getBoolean("NightRideMode", false)) {
                dialogDice.findViewById<TextView>(R.id.dialog_content).setTextColor(getColor(R.color.lighter_grey))
                dialogDice.findViewById<CardView>(R.id.main_background_dice_dialog).setBackgroundResource(R.drawable.round_back_dark_20)
            }

            val diceListView = dialogDice.findViewById<ListView>(R.id.dice_mode_list_view)
            val arrayAdapter = if (pref.getBoolean("NightRideMode", false)) CustomArrayAdapterSingleChoice(
                this@MapsActivity, android.R.layout.simple_list_item_single_choice,
                resources.getStringArray(R.array.dice_mode)
            ) else ArrayAdapter(
                this@MapsActivity, android.R.layout.simple_list_item_single_choice,
                resources.getStringArray(R.array.dice_mode)
            )
            diceListView?.adapter = arrayAdapter
            diceListView?.choiceMode = ListView.CHOICE_MODE_SINGLE
            diceListView?.onItemClickListener = this

            val dialogDiceMainLinear =
                dialogDice.findViewById<LinearLayout>(R.id.main_linear_layout)
            val dialogDiceOutputLinear = dialogDice.findViewById<LinearLayout>(R.id.output)

            val dialogBtnBack = dialogDice.findViewById<AppCompatButton>(R.id.button_back)
            dialogBtnAgain = dialogDice.findViewById(R.id.button_again)
            loadingDiceOutput = dialogDice.findViewById(R.id.loading_dice_output)

            loadingDice = dialogDice.findViewById(R.id.loading_dice)

            dialogBtnBack.setOnClickListener {
                dialogDiceOutputLinear.visibility = View.GONE
                dialogDiceMainLinear.visibility = View.VISIBLE
            }


            dialogBtnConfirm.setOnClickListener {
                diceDialogErrorMessage.text = ""
                if (diceModeChosen.isEmpty())
                    diceDialogErrorMessage.text = getString(R.string.choose_one_option)
                else {
                    dialogBtnConfirm.visibility = View.GONE
                    loadingDice.visibility = View.VISIBLE
                    if (pref.getBoolean("NightRideMode", false))
                        loadingDice.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
                    dialogBtnAgain.visibility = View.GONE
                    loadingDiceOutput.visibility = View.VISIBLE
                    if (pref.getBoolean("NightRideMode", false))
                        loadingDiceOutput.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)

                    if (diceModeChosen == getString(R.string.spots_nearby)) {
                        nearbySpotsDisplay(true, 10)
                        dialogBtnAgain.setOnClickListener {
                            nearbySpotsDisplay(true, 10)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    } else if (diceModeChosen == getString(R.string.closest_25_spots)) {
                        nearbySpotsDisplay(true, 25)
                        dialogBtnAgain.setOnClickListener {
                            nearbySpotsDisplay(true, 25)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    } else if (diceModeChosen == getString(R.string.favorite_spots)) {
                        favoriteSpotsDisplay(true)
                        dialogBtnAgain.setOnClickListener {
                            favoriteSpotsDisplay(true)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    } else {
                        nearbySpotsDisplay(true, 999)
                        dialogBtnAgain.setOnClickListener {
                            nearbySpotsDisplay(true, 999)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    }
                }
            }

            diceModeChosen = ""
            dialogDice.show()
            val window = dialogDice.window
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialogDice.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            window!!.attributes = lp
        }


        feedbackButton = findViewById(R.id.feedback_button)
        feedbackButton.setOnClickListener {
            clearSearchViewQuery()
            closeAllBottomTabs()
            val dialogFeedback = Dialog(this)
            dialogFeedback.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogFeedback.setCancelable(true)
            dialogFeedback.setContentView(R.layout.dialog_feedback)
            dialogFeedback.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val feedbackEditTextFeedback = dialogFeedback.findViewById<EditText>(R.id.feedback_edit_text)
            val feedbackErrorMessage = dialogFeedback.findViewById<TextView>(R.id.feedback_error_message)
            val feedbackSendButton = dialogFeedback.findViewById<AppCompatButton>(R.id.send_button)
            val feedbackLoading = dialogFeedback.findViewById<ProgressBar>(R.id.loading_feedback)

            if (pref.getBoolean("NightRideMode", false)) {
                dialogFeedback.findViewById<TextView>(R.id.feedback_description).setTextColor(getColor(R.color.lighter_grey))
                dialogFeedback.findViewById<CardView>(R.id.main_background_feedback_dialog).setBackgroundResource(R.drawable.round_back_dark_20)
                feedbackEditTextFeedback.setTextColor(getColor(R.color.lighter_grey))
            }

            feedbackEditTextFeedback.addTextChangedListener(object : TextWatcher {
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
                    feedbackErrorMessage.visibility = View.GONE
                }

                override fun afterTextChanged(s: Editable?) {
                    // This method is called after the text is changed.
                }
            })

            feedbackSendButton.setOnClickListener {
                feedbackErrorMessage.setTextColor(getColor(R.color.red))
                val query = feedbackEditTextFeedback.text.toString()
                if (query.isEmpty()) {
                    feedbackErrorMessage.text = getString(R.string.field_cannot_be_empty)
                    feedbackErrorMessage.visibility = View.VISIBLE
                } else {
                    feedbackSendButton.text = ""
                    feedbackLoading.visibility = View.VISIBLE
                    val data = hashMapOf("message" to query)
                    db.collection("Feedback").document("${System.currentTimeMillis()}_${UUID.randomUUID()}")
                        .set(data).addOnSuccessListener {
                            feedbackSendButton.text = getString(R.string.send)
                            feedbackLoading.visibility = View.GONE
                            feedbackErrorMessage.setTextColor(getColor(R.color.green))
                            feedbackErrorMessage.text = getString(R.string.message_sent)
                            feedbackErrorMessage.visibility = View.VISIBLE
                        }.addOnFailureListener {
                            feedbackErrorMessage.text = getString(R.string.message_error)
                            feedbackButton.text = getString(R.string.send)
                            feedbackLoading.visibility = View.GONE
                            feedbackErrorMessage.visibility = View.VISIBLE
                        }
                }
            }
            dialogFeedback.show()
        }

        settingsButton = findViewById<MaterialButton>(R.id.settings_button)
        settingsButton.setOnClickListener {
            startActivity(Intent(this@MapsActivity, SettingsActivity::class.java))
        }


        /*val easterLocationArray = arrayListOf<Point>()
        val easterTitleArray = arrayListOf<String>()
        db.collection("EasterSpots").get().addOnSuccessListener { documents ->
            for (document in documents) {
                easterLocationArray.add(Point(document.get("latitude").toString().toDouble(), document.get("longitude").toString().toDouble()))
                easterTitleArray.add(document.get("title").toString())
            }
        }

        val easterPlacemarkMapObjectArray = arrayListOf<PlacemarkMapObject>()
        mapView!!.map.addCameraListener { map: Map, cameraPosition: CameraPosition, cameraUpdateReason: CameraUpdateReason, b: Boolean ->
            // Check the zoom level and update marker visibility
            val zoomLevel = cameraPosition.zoom
            if (zoomLevel >= 15f) {
                for (i in 0 until easterLocationArray.size) {
                    val marker = createBitmapFromVector(R.drawable.easter_egg_spot_mark, 40F, 40F)
                    mapObjectCollection =
                        mapView?.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте
                    placemarkMapObject =
                        mapObjectCollection.addPlacemark(easterLocationArray[i], ImageProvider.fromBitmap(marker))
                    easterPlacemarkMapObjectArray.add(placemarkMapObject)

                    mapObjectTapListener = object : MapObjectTapListener {
                        override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
                            clearSearchViewQuery()
                            spotLinkConstraintLayout.visibility = View.VISIBLE
                            spotLinkTitle.setText(easterTitleArray[i])

                            val showSpotDetailesTV =
                                findViewById<TextView>(R.id.show_spot_details_textview)
                            showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))

                            if (pref.getBoolean("Translation", false)) {
                                var correctLanguageCode = ""
                                for (c in easterTitleArray[i].toCharArray()) {
                                    if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                                        correctLanguageCode = "en"
                                        break
                                    }
                                }
                                if (correctLanguageCode.isEmpty()) {
                                    correctLanguageCode = ""
                                    for (c in easterTitleArray[i].toCharArray()) {
                                        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                                            correctLanguageCode = "ru"
                                            break
                                        }
                                    }
                                }

                                if (!correctLanguageCode.isEmpty()) {
                                    showSpotDetailesTV.setText(getString(R.string.translator_initialization))
                                    val options = TranslatorOptions.Builder()
                                        .setSourceLanguage("ru")
                                        .setTargetLanguage(Locale.getDefault().language)
                                        .build()

                                    val translator = Translation.getClient(options)
                                    translator.downloadModelIfNeeded(conditions)
                                        .addOnSuccessListener {
                                            showSpotDetailesTV.setText(getString(R.string.translating))
                                            translator.translate(easterTitleArray[i]).addOnSuccessListener {
                                                showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                                                spotLinkTitle.setText(it)
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this@MapsActivity,
                                                    getString(R.string.error_translating),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                                            }
                                        }.addOnFailureListener {
                                        Toast.makeText(
                                            this@MapsActivity,
                                            getString(R.string.error_downloading_model),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                                    }.addOnCanceledListener {
                                        val backgroundModelDownload = Thread(Runnable {
                                            translator.downloadModelIfNeeded(conditions)
                                        })
                                        backgroundModelDownload.start()
                                    }
                                }
                            }

                            closeAllBottomTabs()

                            spotLinkConstraintLayout.setOnClickListener {
                                val intent =
                                    Intent(this@MapsActivity, SpotReviewActivity::class.java)
                                intent.putExtra("title", title)
                                intent.putExtra("latitude", easterLocationArray[i].latitude.toString())
                                intent.putExtra("longitude", easterLocationArray[i].longitude.toString())
                                intent.putExtra("type", "Пасхалка")
                                startActivity(intent)
                            }

                            return true
                        }
                    }
//                    mapObjectTapListenerPointlessArray.add(mapObjectTapListener)
                }
            } else {
                for (i in 0 until easterPlacemarkMapObjectArray.size) {
                    mapObjectCollection =
                        mapView?.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте
                    mapObjectCollection.remove(easterPlacemarkMapObjectArray[i])
                }
            }
        }*/


        /*val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // User is not signed in or the account is not verified
                // You can sign out the user here
                auth.signOut()
                // Redirect the user to the sign-in screen or perform any other actions
            }
        }
        auth.addAuthStateListener(authStateListener)*/

        //Account validity check:
        if (auth.currentUser != null) {
            db.collection("Users").document(auth.currentUser!!.uid).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documentSnapshot = task.result
                        // If the document exists, documentSnapshot will not be null
                        val exists = documentSnapshot != null && documentSnapshot.exists()
                        if (!exists) {
                            Toast.makeText(
                                this@MapsActivity,
                                getString(R.string.account_deleted),
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                    }
                }
        }

    }


    private fun favoriteSpotsDisplay(fromDiceModeDialog: Boolean) {
        val youNeedToSignInToFavorites =
            findViewById<TextView>(R.id.you_need_to_sign_in_for_favorites)
        val goToSignInBtnFavorites =
            findViewById<AppCompatButton>(R.id.go_to_signin_button_favorites)
        goToSignInBtnFavorites.visibility = View.GONE
        youNeedToSignInToFavorites.visibility = View.GONE
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim)

        if (FirebaseAuth.getInstance().currentUser == null) {
            if (fromDiceModeDialog) {
                dialogBtnConfirm.visibility = View.VISIBLE
                loadingDice.visibility = View.GONE
                dialogBtnAgain.visibility = View.VISIBLE
                loadingDiceOutput.visibility = View.GONE
                Toast.makeText(this@MapsActivity, getString(R.string.you_need_to_sign_in_to_favorites), Toast.LENGTH_SHORT).show()
            } else {
                googleSignInDialogDismiss = true
                youNeedToSignInToFavorites.visibility = View.VISIBLE
                goToSignInBtnFavorites.visibility = View.VISIBLE
                goToSignInBtnFavorites.setOnClickListener {
                    signInDialogShowup()
                }
                favoriteSpotsIcon.setColorFilter(coolTealColor)
                bottomSheetDialogFavorites.visibility =
                    View.VISIBLE
                bottomSheetDialogFavorites.startAnimation(slideUpAnimation)
                zoomFab.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                favoriteSpotsIconPressed = true
            }
        } else {
            lateinit var recyclerView: RecyclerView
            lateinit var recyclerViewDice: RecyclerView
            if (!fromDiceModeDialog) {
                loadingFavorites.visibility = View.VISIBLE
                favoriteSpotsIcon.setColorFilter(coolTealColor)
                bottomSheetDialogFavorites.visibility =
                    View.VISIBLE
                bottomSheetDialogFavorites.startAnimation(slideUpAnimation)
                zoomFab.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                favoriteSpotsIconPressed = true

                recyclerView = findViewById(R.id.favorite_spots_recycler_view)
            } else {
                recyclerViewDice = dialogDice.findViewById(R.id.dice_recycler_view)
            }
            val adapter = FavoriteSpotsAdapter(this, pref.getBoolean("Translation", false), pref.getBoolean("NightRideMode", false)) { clickedItem ->
                // Handle the item click here
                val reviewIntent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
                reviewIntent.putExtra("latitude", clickedItem.latitude)
                reviewIntent.putExtra("longitude", clickedItem.longitude)
                reviewIntent.putExtra("title", clickedItem.title)
                reviewIntent.putExtra("type", clickedItem.type)
                startActivity(reviewIntent)
            }
            val layoutManager = LinearLayoutManager(this)

            if (!fromDiceModeDialog) {
                recyclerView.adapter = adapter
                recyclerView.layoutManager = layoutManager
            } else {
                recyclerViewDice.adapter = adapter
                recyclerViewDice.layoutManager = layoutManager
            }
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val query = db.collection("Users")
                .document(userId)
                .collection("FavoriteSpots")

            query.get()
                .addOnSuccessListener { result ->
                    val favoriteSpots = mutableListOf<FavoriteSpot>()

                    for (document in result) {
                        val title = document.getString("title") ?: ""
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0
                        val type = document.getString("type") ?: ""

                        favoriteSpots.add(FavoriteSpot(title, latitude, longitude, type, ""))
                    }

                    if (!fromDiceModeDialog) {
                        loadingFavorites.visibility = View.GONE

                        if (result.size() == 0) {
                            nothingInFavoritesTV.visibility = View.VISIBLE
                        }
                        adapter.setData(favoriteSpots)
                    } else {
                        if (result.size() == 0)
                            Toast.makeText(
                                this,
                                getString(R.string.theres_nothing_in_favorites_yet),
                                Toast.LENGTH_SHORT
                            ).show()
                        else {
                            val oneFavoriteSpotList = mutableListOf<FavoriteSpot>()
                            oneFavoriteSpotList.add(favoriteSpots.random())
                            val dialogDiceMainLayout =
                                dialogDice.findViewById<LinearLayout>(R.id.main_linear_layout)
                            dialogDiceMainLayout.visibility = View.GONE
                            val dialogDiceOutputLayout =
                                dialogDice.findViewById<LinearLayout>(R.id.output)
                            dialogDiceOutputLayout.visibility = View.VISIBLE
                            adapter.setData(oneFavoriteSpotList)
                        }
                        if (fromDiceModeDialog) {
                            dialogBtnConfirm.visibility = View.VISIBLE
                            loadingDice.visibility = View.GONE
                            dialogBtnAgain.visibility = View.VISIBLE
                            loadingDiceOutput.visibility = View.GONE
                        }
                    }

                    // Update the RecyclerView adapter with the data
                }
                .addOnFailureListener { exception ->
                    // Handle errors
                }

        }
    }


    private fun nearbySpotsDisplay(fromDiceModeDialog: Boolean, quantity: Int) {
        val adapter = NearbySpotsAdapter(this@MapsActivity, pref.getBoolean("NightRideMode", false)) { clickedItem ->
            // Handle the item click here
            val reviewIntent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
            reviewIntent.putExtra("latitude", clickedItem.latitude)
            reviewIntent.putExtra("longitude", clickedItem.longitude)
            reviewIntent.putExtra("title", clickedItem.title)
            reviewIntent.putExtra("type", clickedItem.type)
            startActivity(reviewIntent)
        }

        lateinit var recyclerViewDice: RecyclerView

        if (fromDiceModeDialog)
            recyclerViewDice = dialogDice.findViewById<RecyclerView>(R.id.dice_recycler_view)

        val layoutManager = LinearLayoutManager(this)

        if (!fromDiceModeDialog) {
            nearbyRecyclerView.adapter = adapter
            nearbyRecyclerView.layoutManager = layoutManager
        } else {
            recyclerViewDice.adapter = adapter
            recyclerViewDice.layoutManager = layoutManager
        }


        lateinit var userLocation: Point
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    // Move to the user's location with animation
                    userLocation = Point(location.latitude, location.longitude)

                    val query = db.collection("Spots")

                    query.get()
                        .addOnSuccessListener { result ->
                            val nearbySpots = mutableListOf<NearbySpot>()
                            var sortedByDistance = mutableListOf<NearbySpot>()
                            val arrayToBeDisplayed = arrayListOf<NearbySpot>()

                            for (document in result) {
                                val title = document.getString("title") ?: ""
                                val latitude = document.getDouble("latitude") ?: 0.0
                                val longitude = document.getDouble("longitude") ?: 0.0
                                val type = document.getString("type") ?: ""

                                val distance: Float = calculateDistance(
                                    userLocation.latitude,
                                    userLocation.longitude,
                                    latitude,
                                    longitude
                                )

                                // Determine the correct icon based on the "type" field

                                nearbySpots.add(
                                    NearbySpot(
                                        title,
                                        latitude,
                                        longitude,
                                        type,
                                        distance
                                    )
                                )
                                sortedByDistance =
                                    nearbySpots.sortedBy { it.distance }.toMutableList()
                            }
                            if (quantity < 999) {
                                if (sortedByDistance.size > quantity) {
                                    for (i in 0 until quantity) {
//                                                sortedByDistance.removeAt(i) //only first 10 items to be displayed
                                        arrayToBeDisplayed.add(sortedByDistance[i])
                                    }
                                }
                            }

                            loadingNearby.visibility = View.GONE

                            if (fromDiceModeDialog) {
                                val oneRandomElementArray = mutableListOf<NearbySpot>()
                                val dialogDiceMainLayout =
                                    dialogDice.findViewById<LinearLayout>(R.id.main_linear_layout)
                                dialogDiceMainLayout.visibility = View.GONE
                                val dialogDiceOutputLayout =
                                    dialogDice.findViewById<LinearLayout>(R.id.output)
                                dialogDiceOutputLayout.visibility = View.VISIBLE
                                if (quantity < 999)
                                    oneRandomElementArray.add(arrayToBeDisplayed.random())
                                else
                                    oneRandomElementArray.add(sortedByDistance.random())
                                dialogBtnConfirm.visibility = View.VISIBLE
                                loadingDice.visibility = View.GONE
                                dialogBtnAgain.visibility = View.VISIBLE
                                loadingDiceOutput.visibility = View.GONE
                                adapter.setData(oneRandomElementArray)

                            } else
                                adapter.setData(arrayToBeDisplayed)
                        }
                        .addOnFailureListener { exception ->
                            // Handle errors
                        }

                } else {
                    loadingNearby.visibility = View.GONE
                    if (fromDiceModeDialog) {
                        dialogBtnConfirm.visibility = View.VISIBLE
                        loadingDice.visibility = View.GONE
                        dialogBtnAgain.visibility = View.VISIBLE
                        loadingDiceOutput.visibility = View.GONE
                    }
                    Toast.makeText(
                        baseContext,
                        getString(R.string.location_data_not_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        diceModeChosen = parent?.getItemAtPosition(position) as String
        diceDialogErrorMessage.setText("")
    }


    private fun clearSearchViewQuery() {
        searchView.clearFocus()
        searchView.setQuery("", false)
    }


    fun signOutAndRevokeAccess() {
        FirebaseAuth.getInstance().signOut()

        // Revoke Google Sign-In access
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.revokeAccess()
    }


    private fun calculateDistance(
        userLat: Double,
        userLon: Double,
        markerLat: Double,
        markerLon: Double
    ): Float {
        val earthRadius = 6371.0 // Radius of the Earth in kilometers

        val lat1 = toRadians(userLat)
        val lon1 = toRadians(userLon)
        val lat2 = toRadians(markerLat)
        val lon2 = toRadians(markerLon)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c.toFloat()).toFloat()
    }


    private fun signInDialogShowup() {
        closeAllBottomTabs()
        zoomFab.visibility = View.VISIBLE

        onLeftTab = true
        dialogSignIn = Dialog(this)
        dialogSignIn.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogSignIn.setCancelable(true)
        dialogSignIn.setContentView(R.layout.dialog_signin)
        dialogSignIn.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogLoginButton =
            dialogSignIn.findViewById<AppCompatButton>(R.id.login_button)
        val dialogSignUpButton = dialogSignIn.findViewById<AppCompatButton>(R.id.sign_up_button)

        val nightRideModeOn = pref.getBoolean("NightRideMode", false)

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
                        { task: Task<AuthResult?> ->
                            if (task.isSuccessful) {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    if (user.isEmailVerified) {
                                        dialogSignIn.dismiss()
                                        zoomFab.visibility = View.VISIBLE
                                        db.collection("Users").document(auth.currentUser!!.uid).get()
                                            .addOnSuccessListener { document ->
                                                if (document.get("username") == null || document.get("username")
                                                        .toString().isEmpty()
                                                ) {
                                                    dialogUsernameSetUp(false)
                                                } else {
                                                    Toast.makeText(this@MapsActivity, getString(R.string.login_completed) + document.get("username").toString(), Toast.LENGTH_SHORT).show()
                                                }
                                            }.addOnFailureListener {
                                                Toast.makeText(this@MapsActivity, getString(R.string.username_check_error), Toast.LENGTH_SHORT).show()
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
            dialogLoginTabTitle.setTextColor(coolTealColor)
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
                            .mapToObj<UCharacter.UnicodeBlock>({ ch: Int ->
                                UCharacter.UnicodeBlock.of(
                                    ch
                                )
                            })
                            .anyMatch({ b: UCharacter.UnicodeBlock -> b == UCharacter.UnicodeBlock.CYRILLIC })
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
                            .addOnCompleteListener({ task11: Task<QuerySnapshot> ->
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


    private fun dialogUsernameSetUp(changeSpotProponents: Boolean) {
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
            val i = Intent(this, MapsActivity::class.java)
            startActivity(i)
            finish()
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
                val usernameEntered = dialogUsernameField.text.toString()
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
                        dialogErrorMessage.text =
                            getString(R.string.given_username_already_taken)
                        dialogBtnConfirm.visibility = View.VISIBLE
                        dialogLoading.visibility = View.GONE
                    } else {
                        Log.d("asasassin", "started adding data to firestore")
                        val data = hashMapOf("username" to usernameEntered)
                        if (changeSpotProponents) {
                            Log.d("usernameChangeIssue", "Inside the changeSpotProponents if")
                            db.collection("Users").document(auth.currentUser!!.uid).get()
                                .addOnSuccessListener { document ->
                                    val oldUsername = document.get("username").toString()
                                    db.collection("Users").document(auth.currentUser!!.uid)
                                        .update("username", usernameEntered)
                                        .addOnSuccessListener {
                                            Log.d("usernameChangeIssue", "Username in Users changed successfully")
                                            if (usernameEntered != oldUsername) {
                                                db.collection("Spots")
                                                    .whereEqualTo("proponent", oldUsername).get()
                                                    .addOnSuccessListener { documents ->
                                                        var updatesCount = 0
                                                        Log.d("usernameChangeIssue", "Got in the Spots collection. Number of documents with old username: ${documents.size()}")
                                                        for (document in documents) {
                                                            db.collection("Spots")
                                                                .document(document.id).update(
                                                                    "proponent",
                                                                    usernameEntered
                                                                )
                                                                .addOnSuccessListener {
                                                                    Log.d("usernameChangeIssue", "Username change in 1 Spots document")
                                                                    updatesCount++
                                                                    if (updatesCount == documents.size()) {
                                                                        // All updates are complete, update UI here
                                                                        mainLayout.visibility =
                                                                            View.GONE
                                                                        successLayout.visibility =
                                                                            View.VISIBLE
                                                                    }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    // Handle update failure here
                                                                }
                                                        }
                                                    }
                                            }

                                        }.addOnFailureListener {
                                        }
                                }
                        } else {
                            db.collection("Users").document(auth.currentUser!!.uid).update(data as kotlin.collections.Map<String, Any>)
                                .addOnSuccessListener {
                                    mainLayout.visibility = View.GONE
                                    successLayout.visibility = View.VISIBLE
                                }
                        }
                    }
                }.addOnFailureListener {
                    if (usernameTaken) {
                        dialogErrorMessage.text =
                            getString(R.string.given_username_already_taken)
                    } else {
                        Log.d("asasassin", "asfega")
                        dialogBtnConfirm.visibility = View.GONE
                        dialogLoading.visibility = View.VISIBLE
                        val data = hashMapOf("username" to usernameEntered)
                        db.collection("Users").document(auth.currentUser!!.uid).update(data as kotlin.collections.Map<String, Any>)
                            .addOnSuccessListener {
                                mainLayout.visibility = View.GONE
                                successLayout.visibility = View.VISIBLE
                            }.addOnFailureListener {
                            }
                    }
                }
            }
        }
        Log.d("iamhere", "it should appear now")
        dialog.show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check for location availability
                getAndMoveToUserLocation()
            } else {
                // Permission denied, show a toast message
            }
        }
    }

    private fun checkLocationPermissionAndAvailability() {
        checkLocationAvailability()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        // Check if permission is not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        ) {
            // Permission and location data are available, get user's location and move
            getAndMoveToUserLocation()
        } else {
            // Permission or location data is not available, show a toast message
        }
    }

    private fun checkLocationAvailability() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location is available
        } else {
            Toast.makeText(this, getString(R.string.location_not_available), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getAndMoveToUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    // Move to the user's location with animation
                    val userLocation = Point(location.latitude, location.longitude)
                    locationOnMapKit.isVisible = true
                    mapView?.map?.move(
                        CameraPosition(userLocation, zoomValue, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                } else {
                    // Location data is not available, show a toast message
                }
            }
        }
    }


    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    private fun createBitmapFromVector(art: Int, widthDp: Float, heightDp: Float): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, art) ?: return null

        // Convert dp to pixels
        val widthPx = dpToPx(widthDp)
        val heightPx = dpToPx(heightDp)

        val bitmap = Bitmap.createBitmap(
            widthPx,
            heightPx,
            Bitmap.Config.ARGB_8888
        ) ?: return null

        val canvas = Canvas(bitmap)

        // Set the bounds using pixel values
        drawable.setBounds(0, 0, widthPx, heightPx)
        drawable.draw(canvas)

        return bitmap
    }


    private fun fetchAndProcessSpots() {
        // Use a CoroutineScope to perform Firestore operations
        spotsCollection.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val totalDocumentCount =
                    documents.size() // Total number of documents in the collection
                val title = document.getString("title") ?: ""
                val latitude = document.getDouble("latitude") ?: 0.0
                val longitude = document.getDouble("longitude") ?: 0.0
                val type = document.getString("type") ?: ""

                setMarkerInLocation(latitude, longitude, type, title)
                processedDocumentCount++
                if (processedDocumentCount == totalDocumentCount) {
                    // All documents are processed, hide the loading view
                    spotsLoadingLinearLayout.visibility = View.GONE
                }
            }
        }.addOnFailureListener {
            Toast.makeText(baseContext, "Error reading firestore data", Toast.LENGTH_SHORT)
                .show()
            spotsLoadingLinearLayout.visibility = View.GONE
        }
    }


    private fun setMarkerInLocation(
        latitude: Double,
        longitude: Double,
        type: String,
        title: String
    ) {
        val location = Point(latitude, longitude)
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

        val marker = createBitmapFromVector(correctIconId, 40F, 40F)
        mapObjectCollection =
            mapView?.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте
        placemarkMapObject =
            mapObjectCollection.addPlacemark(location, ImageProvider.fromBitmap(marker))

        mapObjectTapListener = object : MapObjectTapListener {
            override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
                clearSearchViewQuery()
                spotLinkConstraintLayout.visibility = View.VISIBLE
                spotLinkTitle.setText(title)

                val showSpotDetailesTV = findViewById<TextView>(R.id.show_spot_details_textview)
                showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))

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
                        showSpotDetailesTV.setText(getString(R.string.translator_initialization))
                        val options = TranslatorOptions.Builder()
                            .setSourceLanguage("ru")
                            .setTargetLanguage(Locale.getDefault().language)
                            .build()

                        val translator = Translation.getClient(options)
                        translator.downloadModelIfNeeded(conditions).addOnSuccessListener {
                            showSpotDetailesTV.setText(getString(R.string.translating))
                            translator.translate(title).addOnSuccessListener {
                                showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                                spotLinkTitle.setText(it)
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@MapsActivity,
                                    getString(R.string.error_translating),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                            }
                        }.addOnFailureListener {
                            Toast.makeText(
                                this@MapsActivity,
                                getString(R.string.error_downloading_model),
                                Toast.LENGTH_SHORT
                            ).show()
                            showSpotDetailesTV.setText(getString(R.string.go_to_spot_info))
                        }.addOnCanceledListener {
                            val backgroundModelDownload = Thread(Runnable {
                                translator.downloadModelIfNeeded(conditions)
                            })
                            backgroundModelDownload.start()
                        }
                    }
                }

                closeAllBottomTabs()

                spotLinkConstraintLayout.setOnClickListener {
                    val intent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
                    intent.putExtra("title", title)
                    intent.putExtra("latitude", latitude)
                    intent.putExtra("longitude", longitude)
                    intent.putExtra("type", type)
                    startActivity(intent)
                }

                return true
            }
        }
        mapObjectTapListenerPointlessArray.add(mapObjectTapListener)


        inputListener = object : InputListener {
            override fun onMapTap(p0: Map, p1: Point) {
                searchView.clearFocus()
                onMapTap()
            }

            override fun onMapLongTap(p0: Map, p1: Point) {

            }
        }

        mapView?.map?.addInputListener(inputListener)
        placemarkMapObject.addTapListener(mapObjectTapListener)
    }


    private fun onMapTap() {
        zoomFab.visibility = View.VISIBLE
        spotLinkConstraintLayout.visibility = View.GONE
        spotLinkTitle.setText("")
        closeAllBottomTabs()
    }


    private fun closeAllBottomTabs() {
        val slideDownAnimation =
            AnimationUtils.loadAnimation(baseContext, R.anim.slide_down_anim)
        if (favoriteSpotsIconPressed)
            bottomSheetDialogFavorites.startAnimation(slideDownAnimation)
        else if (nearbySpotsIconPressed)
            bottomSheetDialogNearby.startAnimation(slideDownAnimation)
        else if (addIconPressed)
            bottomSheetDialogLinearLayout.startAnimation(slideDownAnimation)
        else if (codeOfConductIconPressed)
            bottomSheetDialogCodeOfConduct.startAnimation(slideDownAnimation)
        else if (accountIconPressed)
            bottomSheetDialogAccount.startAnimation(slideDownAnimation)
        favoriteSpotsIconPressed = false
        nearbySpotsIconPressed = false
        addIconPressed = false
        codeOfConductIconPressed = false
        accountIconPressed = false
        favoriteSpotsIcon.setColorFilter(lightGreyColor)
        nearbySpotsIcon.setColorFilter(lightGreyColor)
        codeOfConductIcon.setColorFilter(lightGreyColor)
        accountIcon.setColorFilter(lightGreyColor)
        addSpotIcon.setColorFilter(lightGreyColor)
        bottomSheetDialogFavorites.visibility = View.GONE
        bottomSheetDialogNearby.visibility = View.GONE
        bottomSheetDialogLinearLayout.visibility = View.GONE
        bottomSheetDialogCodeOfConduct.visibility = View.GONE
        bottomSheetDialogAccount.visibility = View.GONE
    }


    /*fun setAppLocale(languageCode: String?) {
        val locale = Locale(languageCode!!)
        Locale.setDefault(locale)

        val res: Resources = baseContext.getResources()
        val config = Configuration(res.getConfiguration())
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }*/


    override fun onResume() {
        super.onResume()
        val nightRideMode = pref.getBoolean("NightRideMode", false)
        if (nightRideMode) {
            applyDarkTheme()
        } else {
            applyLightTheme()
        }
    }


    private fun applyDarkTheme() {
        mapView?.map?.isNightModeEnabled = true
        diceBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme))
        feedbackButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme))
        settingsButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme))
        zoomFab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.dark_theme)))
        iconsLinearLayout.setBackgroundResource(R.drawable.round_back_dark_20)
        searchView.setBackgroundResource(R.drawable.round_back_dark_20)
        val id = searchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
        val textView = searchView.findViewById<View>(id) as TextView
        textView.setTextColor(getColor(R.color.lighter_grey))
        searchOutputLinearLayout.setBackgroundResource(R.drawable.round_back_dark_20)
        bottomSheetDialogFavorites.setBackgroundResource(R.drawable.round_back_dark_20)
        bottomSheetDialogAccount.setBackgroundResource(R.drawable.round_back_dark_20)
        bottomSheetDialogLinearLayout.setBackgroundResource(R.drawable.round_back_dark_20)
        bottomSheetDialogNearby.setBackgroundResource(R.drawable.round_back_dark_20)
        bottomSheetDialogCodeOfConduct.setBackgroundResource(R.drawable.round_back_dark_20)
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_favorites).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_nearby).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_account).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.code_of_conduct_title).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.code_1).setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.code_2).setTextColor(getColor(R.color.lighter_grey))
        loadingAccount.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
        loadingNearby.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
        findViewById<ProgressBar>(R.id.loading_favorites).indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
        addSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        addSpotBtn.setTextColor(getColor(R.color.lighter_grey))
        val signInBtn = findViewById<AppCompatButton>(R.id.google_sign_in_button_account)
        signInBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        signInBtn.setTextColor(getColor(R.color.lighter_grey))
        val usernameBtn = findViewById<MaterialButton>(R.id.username_button)
        usernameBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        usernameBtn.setTextColor(getColor(R.color.lighter_grey))
        val signOutBtn = findViewById<MaterialButton>(R.id.sign_out_button)
        signOutBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        signOutBtn.setTextColor(getColor(R.color.lighter_grey))
        val deleteAccountBtn = findViewById<MaterialButton>(R.id.delete_account_button)
        deleteAccountBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        deleteAccountBtn.setTextColor(getColor(R.color.lighter_grey))
        val signInAddSpotBtn = findViewById<AppCompatButton>(R.id.sign_in_dialog_open)
        signInAddSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        signInAddSpotBtn.setTextColor(getColor(R.color.lighter_grey))
        val signInFavSpotBtn = findViewById<AppCompatButton>(R.id.go_to_signin_button_favorites)
        signInFavSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.dark_theme_lighter))
        signInFavSpotBtn.setTextColor(getColor(R.color.lighter_grey))
        findViewById<TextView>(R.id.spots_loading_textview).setTextColor(getColor(R.color.lighter_grey))
        findViewById<ProgressBar>(R.id.loading_progress_bar).indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_white)
        spotLinkConstraintLayout.setBackgroundResource(R.drawable.round_back_dark_20)
        spotLinkTitle.setTextColor(getColor(R.color.lighter_grey))
        findViewById<ImageButton>(R.id.cancel_spot_link_image_button).imageTintList = ColorStateList.valueOf(getColor(R.color.light_grey))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.dark_theme)
        }
    }


    private fun applyLightTheme() {
        mapView?.map?.isNightModeEnabled = false
        diceBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        feedbackButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        settingsButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        zoomFab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)))
        iconsLinearLayout.setBackgroundResource(R.drawable.round_back_white_20)
        searchView.setBackgroundResource(R.drawable.round_back_white_20)
        val id = searchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
        val textView = searchView.findViewById<View>(id) as TextView
        textView.setTextColor(getColor(R.color.black))
        searchOutputLinearLayout.setBackgroundResource(R.drawable.round_back_white_20)
        bottomSheetDialogFavorites.setBackgroundResource(R.drawable.round_back_white_20)
        bottomSheetDialogAccount.setBackgroundResource(R.drawable.round_back_white_20)
        bottomSheetDialogLinearLayout.setBackgroundResource(R.drawable.round_back_white_20)
        bottomSheetDialogNearby.setBackgroundResource(R.drawable.round_back_white_20)
        bottomSheetDialogCodeOfConduct.setBackgroundResource(R.drawable.round_back_white_20)
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_favorites).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_nearby).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.bottom_sheet_dialog_title_account).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.code_of_conduct_title).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.code_1).setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.code_2).setTextColor(getColor(R.color.black))
        loadingAccount.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_black)
        loadingNearby.indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_black)
        findViewById<ProgressBar>(R.id.loading_favorites).indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_black)
        addSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        addSpotBtn.setTextColor(getColor(R.color.black))
        val signInBtn = findViewById<AppCompatButton>(R.id.google_sign_in_button_account)
        signInBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        signInBtn.setTextColor(getColor(R.color.black))
        val usernameBtn = findViewById<MaterialButton>(R.id.username_button)
        usernameBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        usernameBtn.setTextColor(getColor(R.color.black))
        val signOutBtn = findViewById<MaterialButton>(R.id.sign_out_button)
        signOutBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        signOutBtn.setTextColor(getColor(R.color.black))
        val deleteAccountBtn = findViewById<MaterialButton>(R.id.delete_account_button)
        deleteAccountBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        deleteAccountBtn.setTextColor(getColor(R.color.black))
        val signInAddSpotBtn = findViewById<AppCompatButton>(R.id.sign_in_dialog_open)
        signInAddSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        signInAddSpotBtn.setTextColor(getColor(R.color.black))
        val signInFavSpotBtn = findViewById<AppCompatButton>(R.id.go_to_signin_button_favorites)
        signInFavSpotBtn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lighter_grey))
        signInFavSpotBtn.setTextColor(getColor(R.color.black))
        findViewById<TextView>(R.id.spots_loading_textview).setTextColor(getColor(R.color.black))
        findViewById<ProgressBar>(R.id.loading_progress_bar).indeterminateDrawable = getDrawable(R.drawable.custom_progress_bar_black)
        spotLinkConstraintLayout.setBackgroundResource(R.drawable.round_back_white_20)
        spotLinkTitle.setTextColor(getColor(R.color.black))
        findViewById<ImageButton>(R.id.cancel_spot_link_image_button).imageTintList = ColorStateList.valueOf(getColor(R.color.light_grey))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.cool_teal)
        }
    }


    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }

    override fun onDestroy() {
        unregisterReceiver(networkChangeReceiver)
        super.onDestroy()
    }
}
