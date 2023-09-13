package com.tricktrack.tricktrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider

class MapsActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private val startLocation = Point(53.9, 27.56)
    private var zoomValue: Float = 12f
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private var locationUpdatesStarted = false
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationOnMapKit: UserLocationLayer
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var favoriteSpotsIcon: ImageView
    private lateinit var nearbySpotsIcon: ImageView
    private lateinit var addSpotIcon: ImageView
    private lateinit var settingsIcon: ImageView
    private lateinit var moreIcon: ImageView
    private lateinit var bottomSheetDialogLinearLayout: LinearLayout
    private var addIconPressed = false
    private var favoriteSpotsIconPressed = false
    private var nearbySpotsIconPressed = false
    private var settingsIconPressed = false
    private var moreIconPressed = false
    private val coolTealColor = Color.rgb(0, 120, 140)
    private val lightGreyColor = Color.rgb(155, 155, 155)
    private lateinit var auth: FirebaseAuth
    private lateinit var client: GoogleSignInClient
    private lateinit var bottomSheedDialogTitle: TextView
    private lateinit var bottomSheedDialogDetails: TextView
    private lateinit var addSpotBtn: AppCompatButton


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
        mapView = findViewById(R.id.mapview)
        favoriteSpotsIcon = findViewById(R.id.favorite_spots_icon)
        nearbySpotsIcon = findViewById(R.id.nearby_spots_icon)
        addSpotIcon = findViewById(R.id.add_spot_icon)
        settingsIcon = findViewById(R.id.settings_icon)
        moreIcon = findViewById(R.id.more_icon)
        bottomSheedDialogTitle = findViewById(R.id.bottom_sheet_dialog_title)
        bottomSheedDialogDetails = findViewById(R.id.bottom_sheet_dialog_details)
        addSpotBtn = findViewById(R.id.add_spot_button)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var mapKit: MapKit = MapKitFactory.getInstance()
        locationOnMapKit = mapView?.mapWindow?.let { mapKit.createUserLocationLayer(it) }!!
        locationOnMapKit.isVisible = true

        val zoomFab = findViewById<FloatingActionButton>(R.id.zoom_fab)
        zoomFab.setOnClickListener {
            // Check for location permission and availability when the button is clicked
            checkLocationPermissionAndAvailability()
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
        setMarkerInStartLocation()

        bottomSheetDialogLinearLayout = findViewById(R.id.bottom_sheet_dialog_linear_layout)

        auth = FirebaseAuth.getInstance()
        val options =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        client = GoogleSignIn.getClient(this, options)

        findViewById<Button>(R.id.google_sign_in_button).setOnClickListener {
            val intent = client.signInIntent
            startActivityForResult(intent, 10001)
        }



        favoriteSpotsIcon.setOnClickListener {
            favoriteSpotsIcon.setColorFilter(coolTealColor)
        }

        nearbySpotsIcon.setOnClickListener {
            nearbySpotsIcon.setColorFilter(coolTealColor)
        }

        addSpotBtn.setOnClickListener {
            /*val intent = Intent(this@MapsActivity, AddSpotActivity::class.java)         //Shared animation intent
            val pairs : Array<Pair<View, String>?> = arrayOfNulls(1)
            pairs[0] = Pair<View, String>(bottomSheetDialogLinearLayout, "bottomSheet")
            pairs[0] = Pair<View, String>(bottomSheedDialogTitle, "bottomSheetTitle")
            var options: ActivityOptions? = null
            options = ActivityOptions.makeSceneTransitionAnimation(this@MapsActivity, *pairs)
            startActivity(intent, options.toBundle())*/

            val intent = Intent(this@MapsActivity, AddSpotActivity::class.java)
            startActivity(intent)
        }

        addSpotIcon.setOnClickListener {
            if (!addIconPressed) {
                //TODO: Check if the user's authenticated with Google. If not text details changes to something explaining that they're gotta sign in and the
                //TODO: Google sign in button below that. If yes, than just a button transfering to AddSpotActivity
                //TODO: If (!<already signed in>):

                //TODO: To do that check: if (FirebaseAuth.getInstance().currentUser != null) {...}

                if (FirebaseAuth.getInstance().currentUser != null) {
                    findViewById<Button>(R.id.google_sign_in_button).visibility = View.GONE
                    bottomSheedDialogDetails.visibility = View.GONE
                    addSpotBtn.visibility = View.VISIBLE
                } else {
                    findViewById<Button>(R.id.google_sign_in_button).visibility = View.VISIBLE
                    bottomSheedDialogDetails.visibility = View.VISIBLE
                    addSpotBtn.visibility = View.GONE
                }
                favoriteSpotsIconPressed = false
                nearbySpotsIconPressed = false
                settingsIconPressed = false
                moreIconPressed = false
                nearbySpotsIcon.setColorFilter(lightGreyColor)
                favoriteSpotsIcon.setColorFilter(lightGreyColor)
                settingsIcon.setColorFilter(lightGreyColor)
                moreIcon.setColorFilter(lightGreyColor)
                addSpotIcon.setColorFilter(coolTealColor)
                bottomSheetDialogLinearLayout.visibility = View.VISIBLE
                zoomFab.visibility = View.GONE
                addIconPressed = true
            } else {
                addSpotIcon.setColorFilter(lightGreyColor)
                bottomSheetDialogLinearLayout.visibility = View.GONE
                zoomFab.visibility = View.VISIBLE
                addIconPressed = false
            }
        }

        settingsIcon.setOnClickListener {
            settingsIcon.setColorFilter(coolTealColor)

        }

        moreIcon.setOnClickListener {
            moreIcon.setColorFilter(coolTealColor)
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
                            val i = Intent(this, MapsActivity::class.java)
                            startActivity(i)
                        finish()
                        } else {
                            Toast.makeText(
                                this,
                                "onActivity result issue" + task.exception?.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        } catch (_: ApiException) {}
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
                .show() //TODO: Doesn't work. Fix it
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


    private fun createBitmapFromVector(art: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, art) ?: return null
        val bitmap = Bitmap.createBitmap(
            120,
            120,
            Bitmap.Config.ARGB_8888
        ) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    private fun setMarkerInStartLocation() { //TODO: Should take all the info from firebase
        val marker = createBitmapFromVector(R.drawable.park_spot_mark)
        mapObjectCollection =
            mapView?.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте
        placemarkMapObject =
            mapObjectCollection.addPlacemark(startLocation, ImageProvider.fromBitmap(marker))
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
}
