package com.tricktrack.tricktrack

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    private lateinit var zoomFab: FloatingActionButton
    private lateinit var cancelSpotLinkButton: ImageButton
    private var mapObjectTapListenerPointlessArray = arrayListOf<MapObjectTapListener>()
    private lateinit var inputListener: InputListener
    private lateinit var dialogGoogleSignIn: Dialog
    private var googleSignInDialogDismiss = false
    private lateinit var nothingInFavoritesTV: TextView
    private lateinit var bottomSheetDialogNearby: LinearLayout
    private lateinit var loadingNearby: ProgressBar
    private lateinit var nearbyRecyclerView: RecyclerView
    private lateinit var searchOutputRecyclerView: RecyclerView
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var bottomSheetDialogAccount: LinearLayout
    private lateinit var bottomSheetDialogAccountTitle: TextView
    private lateinit var googleSignInButtonAccount: MaterialButton
    private lateinit var signOutBtn: MaterialButton
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

        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim)
        val slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_anim)
        val slideDownOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_open_anim)
        val slideUpOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_open_anim)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_anim)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out_anim)

        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)

        searchView = findViewById<SearchView>(R.id.search_view)

        val searchIconId = resources.getIdentifier("android:id/search_mag_icon", null, null)
        val searchIcon = searchView.findViewById(searchIconId) as ImageView
        searchIcon.setImageResource(android.R.color.transparent)

        val closeIconId = resources.getIdentifier("android:id/search_close_btn", null, null)
        val closeIcon = searchView.findViewById(closeIconId) as ImageView
        closeIcon.setImageResource(android.R.color.transparent)


        bottomSheetDialogFavorites = findViewById<LinearLayout>(R.id.bottom_sheet_dialog_favorites)

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
                                                dialogGoogleSignIn.dismiss()
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

        zoomFab = findViewById<FloatingActionButton>(R.id.zoom_fab)
        zoomFab.setOnClickListener {
            searchView.clearFocus()
            // Check for location permission and availability when the button is clicked
            checkLocationPermissionAndAvailability()
        }

        cancelSpotLinkButton.setOnClickListener {
            searchView.clearFocus()
            zoomFab.visibility = View.VISIBLE
            spotLinkConstraintLayout.visibility = View.GONE
            spotLinkTitle.setText("")
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

        findViewById<Button>(R.id.google_sign_in_button).setOnClickListener {
            searchView.clearFocus()
            val intent = client.signInIntent
            signInLauncher.launch(intent)
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
                loadingFavorites = findViewById<ProgressBar>(R.id.loading_favorites)
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
                    findViewById<Button>(R.id.google_sign_in_button).visibility = View.GONE
                    bottomSheetDialogDetails.visibility = View.GONE
                    addSpotBtn.visibility = View.VISIBLE
                } else {
                    googleSignInDialogDismiss = false
                    findViewById<Button>(R.id.google_sign_in_button).visibility = View.VISIBLE
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
                        val intent = client.signInIntent
                        signInLauncher.launch(intent)
                    }
                } else {
                    googleSignInButtonAccount.visibility = View.GONE
                    loadingAccount.visibility = View.VISIBLE
                    usernameBtn.visibility = View.GONE
                    signOutBtn.visibility = View.GONE
                    bottomSheetDialogAccountTitle.visibility = View.GONE
                    db.collection("Users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                        .get().addOnSuccessListener { document ->
                            loadingAccount.visibility = View.GONE
                            usernameBtn.visibility = View.VISIBLE
                            signOutBtn.visibility = View.VISIBLE
                            if (document.get("username").toString().isEmpty()) {
                                bottomSheetDialogAccountTitle.setText(getString(R.string.account))
                                bottomSheetDialogAccountTitle.visibility = View.VISIBLE
                                usernameBtn.setOnClickListener {
                                    dialogUsernameSetUp(false)
                                }
                            } else {
                                bottomSheetDialogAccountTitle.setText(
                                    document.get("username").toString()
                                )
                                bottomSheetDialogAccountTitle.visibility = View.VISIBLE
                                usernameBtn.setText(getString(R.string.change_username))

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
        val searchOutputLinearLayout = findViewById<LinearLayout>(R.id.search_output_linear_layout)
        val nothingFoundTV = findViewById<TextView>(R.id.nothing_found_text_view)
        val loadingSearchOutput = findViewById<ProgressBar>(R.id.loading_seach_output)
        val searchIconTV = findViewById<ImageView>(R.id.search_icon_text_view)
        var playSearchAnim = true

        searchCancelIcon.setOnClickListener {
            clearSearchViewQuery()
        }

        searchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // Call your function when the SearchView gains focus
                onMapTap()
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNullOrBlank()) {
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
                    val adapter = NearbySpotsAdapter { clickedItem ->
                        // Handle the item click here
                        val reviewIntent = Intent(this@MapsActivity, SpotReviewActivity::class.java)
                        reviewIntent.putExtra("latitude", clickedItem.latitude)
                        reviewIntent.putExtra("longitude", clickedItem.longitude)
                        reviewIntent.putExtra("title", clickedItem.title)
                        reviewIntent.putExtra("type", clickedItem.type)
                        startActivity(reviewIntent)
                    }

                    val adapterFavorites = FavoriteSpotsAdapter { clickedItem ->
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

                                        var distance: Float = 0F
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
                                                    type
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
                                .addOnFailureListener { exception ->
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

        val diceBtn = findViewById<MaterialButton>(R.id.dice_button)
        diceBtn.setOnClickListener {
            onMapTap()
            clearSearchViewQuery()

            dialogDice = Dialog(this)
            dialogDice.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogDice.setCancelable(true)
            dialogDice.setContentView(R.layout.dialog_dice)
            dialogDice.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogBtnConfirm = dialogDice.findViewById<AppCompatButton>(R.id.button_confirm)
            diceDialogErrorMessage = dialogDice.findViewById<TextView>(R.id.error_message)

            val diceListView = dialogDice.findViewById<ListView>(R.id.dice_mode_list_view)
            val arrayAdapter = ArrayAdapter(
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
            dialogBtnAgain = dialogDice.findViewById<AppCompatButton>(R.id.button_again)
            loadingDiceOutput = dialogDice.findViewById(R.id.loading_dice_output)

            loadingDice = dialogDice.findViewById(R.id.loading_dice)

            dialogBtnBack.setOnClickListener {
                dialogDiceOutputLinear.visibility = View.GONE
                dialogDiceMainLinear.visibility = View.VISIBLE
            }


            dialogBtnConfirm.setOnClickListener {
                diceDialogErrorMessage.setText("")
                if (diceModeChosen.isEmpty())
                    diceDialogErrorMessage.setText(getString(R.string.choose_one_option))
                else {
                    dialogBtnConfirm.visibility = View.GONE
                    loadingDice.visibility = View.VISIBLE
                    dialogBtnAgain.visibility = View.GONE
                    loadingDiceOutput.visibility = View.VISIBLE
                    if (diceModeChosen.equals(getString(R.string.spots_nearby))) {
                        nearbySpotsDisplay(true, 10)
                        dialogBtnAgain.setOnClickListener {
                            nearbySpotsDisplay(true, 10)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    } else if (diceModeChosen.equals(getString(R.string.closest_25_spots))) {
                        nearbySpotsDisplay(true, 25)
                        dialogBtnAgain.setOnClickListener {
                            nearbySpotsDisplay(true, 25)
                            dialogBtnAgain.visibility = View.GONE
                            loadingDiceOutput.visibility = View.VISIBLE
                        }
                    } else if (diceModeChosen.equals(getString(R.string.favorite_spots))) {
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
        }
    }


    private fun favoriteSpotsDisplay(fromDiceModeDialog: Boolean) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            dialogBtnConfirm.visibility = View.VISIBLE
            loadingDice.visibility = View.GONE
            dialogBtnAgain.visibility = View.VISIBLE
            loadingDiceOutput.visibility = View.GONE

            googleSignInDialogDismiss = true
            googleSignInDialogShowup(true)
        } else {
            lateinit var recyclerView: RecyclerView
            lateinit var recyclerViewDice: RecyclerView
            if (!fromDiceModeDialog) {
                loadingFavorites.visibility = View.VISIBLE
                favoriteSpotsIcon.setColorFilter(coolTealColor)
                bottomSheetDialogFavorites.visibility =
                    View.VISIBLE
                val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim)
                bottomSheetDialogFavorites.startAnimation(slideUpAnimation)
                zoomFab.visibility = View.GONE
                spotLinkConstraintLayout.visibility = View.GONE
                favoriteSpotsIconPressed = true

                recyclerView = findViewById(R.id.favorite_spots_recycler_view)
            } else {
                recyclerViewDice = dialogDice.findViewById<RecyclerView>(R.id.dice_recycler_view)
            }
            val adapter = FavoriteSpotsAdapter { clickedItem ->
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

                        // Determine the correct icon based on the "type" field

                        favoriteSpots.add(FavoriteSpot(title, latitude, longitude, type))
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
                        dialogBtnConfirm.visibility = View.VISIBLE
                        loadingDice.visibility = View.GONE
                        dialogBtnAgain.visibility = View.VISIBLE
                        loadingDiceOutput.visibility = View.GONE
                    }

                    // Update the RecyclerView adapter with the data
                }
                .addOnFailureListener { exception ->
                    // Handle errors
                }

        }
    }


    private fun nearbySpotsDisplay(fromDiceModeDialog: Boolean, quantity: Int) {
        val adapter = NearbySpotsAdapter { clickedItem ->
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


    private fun googleSignInDialogShowup(favoriteSpotsClicked: Boolean) {
        dialogGoogleSignIn = Dialog(this)
        dialogGoogleSignIn.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogGoogleSignIn.setCancelable(true)
        dialogGoogleSignIn.setContentView(R.layout.dialog_google_sign_in)
        if (favoriteSpotsClicked) {
            dialogGoogleSignIn.findViewById<TextView>(R.id.dialog_content)
                .setText(getString(R.string.you_need_to_sign_in_to_favorites))
        }
        dialogGoogleSignIn.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogGoogleSignInBtn =
            dialogGoogleSignIn.findViewById<MaterialButton>(R.id.google_sign_in_button)
        dialogGoogleSignInBtn.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            val options =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            var client = GoogleSignIn.getClient(this, options)
            val intent = client.signInIntent
            signInLauncher.launch(intent)
        }
        dialogGoogleSignIn.show()
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
                        dialogErrorMessage.text =
                            getString(R.string.given_username_already_taken)
                        dialogBtnConfirm.visibility = View.VISIBLE
                        dialogLoading.visibility = View.GONE
                    } else {
                        Log.d("asasassin", "started adding data to firestore")
                        val data = hashMapOf("username" to usernameEntered)
                        if (changeSpotProponents) {
                            db.collection("Users").document(auth.currentUser!!.uid).get()
                                .addOnSuccessListener { document ->
                                    val oldUsername = document.get("username").toString()
                                    db.collection("Users").document(auth.currentUser!!.uid)
                                        .update("username", usernameEntered)
                                        .addOnSuccessListener {
                                            if (usernameEntered != oldUsername) {
                                                db.collection("Spots")
                                                    .whereEqualTo("proponent", oldUsername).get()
                                                    .addOnSuccessListener { documents ->
                                                        var updatesCount = 0
                                                        for (document in documents) {
                                                            db.collection("Spots")
                                                                .document(document.id).update(
                                                                    "proponent",
                                                                    usernameEntered
                                                                )
                                                                .addOnSuccessListener {
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
                            db.collection("Users").document(auth.currentUser!!.uid).set(data)
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
                        db.collection("Users").document(auth.currentUser!!.uid).set(data)
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
                zoomFab.visibility = View.GONE
                addIconPressed = false
                favoriteSpotsIconPressed = false
                nearbySpotsIconPressed = false
                codeOfConductIconPressed = false
                accountIconPressed = false
                addSpotIcon.setColorFilter(lightGreyColor)
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                codeOfConductIcon.setColorFilter(lightGreyColor)
                accountIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE

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
