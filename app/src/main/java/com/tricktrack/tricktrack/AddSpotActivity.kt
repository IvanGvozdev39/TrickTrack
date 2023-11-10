package com.tricktrack.tricktrack

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.lang.Float.min
import java.text.SimpleDateFormat
import java.util.*

class AddSpotActivity : AppCompatActivity(), ActivityResultCaller {

    private lateinit var spotTypeTexts: Array<String>
    private lateinit var spotTypeFirebaseTexts: Array<String>
    private lateinit var spotTypeImages: IntArray
    private lateinit var conditionTexts: Array<String>
    private lateinit var conditionFirebaseTexts: Array<String>
    private lateinit var selectedImageView: ImageView
    private lateinit var mapView: MapView
    private lateinit var scrollViewAddSpot: ScrollView
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationOnMapKit: UserLocationLayer
    private var zoomValue: Float = 15f
    private lateinit var mapFrameLayout: FrameLayout
    private lateinit var zoomFab: MaterialButton
    private lateinit var mapObjectCollection: MapObjectCollection
    private var placemarkMapObject: PlacemarkMapObject? = null
    private lateinit var location: Point
    private lateinit var coordinatesTV: TextView
    private lateinit var mapInputListener: InputListener
    private lateinit var spotTitleEditText: EditText
    private lateinit var spotDescriptionEditText: EditText
    private var chosenSpotType: String = ""
    private var chosenCondition: String = ""
    private var imageBitmapArray = ArrayList<Bitmap>()
    private var imageStorageLinkArray = ArrayList<String>()
    private var db = Firebase.firestore
    private var auth = FirebaseAuth.getInstance()
    private var receivedLatitude = 0.0
    private var receivedLongitude = 0.0
    private var fromSpotReviewActivity = false
    private lateinit var receivedProponent: String
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var checkIconLoading: ProgressBar
    private lateinit var checkIcon: ImageView
    private lateinit var titleSymbolCount: TextView
    private lateinit var descriptionSymbolCount: TextView
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_spot)

        mapFrameLayout = findViewById(R.id.map_frame)
        zoomFab = findViewById(R.id.zoom_fab)
        coordinatesTV = findViewById(R.id.coordinates_text_view)
        spotTitleEditText = findViewById(R.id.spot_title_edit_text)
        spotDescriptionEditText = findViewById(R.id.spot_description_edit_text)
        checkIconLoading = findViewById(R.id.check_icon_loading)
        checkIcon = findViewById(R.id.check_icon)
        titleSymbolCount = findViewById(R.id.title_symbol_count)
        descriptionSymbolCount = findViewById(R.id.description_symbol_count)
        FirebaseApp.initializeApp(this)

        pref = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedImageUri = data.data

                    // Load the selected image into the clicked ImageView
                    selectedImageView.setImageURI(selectedImageUri)

                    val drawable = selectedImageView.drawable
                    if (drawable is BitmapDrawable) {
                        val resizedBitmap = resizeBitmap(drawable.bitmap, 1000, 1000)
//                        imageBitmapArray.add(resizedBitmap)
                        selectedImageView.setImageBitmap(resizedBitmap)
                    }

                    // Set the background to null to remove the plus sign drawable
                    selectedImageView.background = null
                }
            }

        spotTitleEditText.addTextChangedListener(object : TextWatcher {
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
                val symbolCount = spotTitleEditText.text.length
                titleSymbolCount.text = "$symbolCount/120"
                if (symbolCount > 120)
                    titleSymbolCount.setTextColor(getColor(R.color.red))
                else
                    titleSymbolCount.setTextColor(getColor(R.color.light_grey))
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text is changed.
            }
        })

        spotDescriptionEditText.addTextChangedListener(object : TextWatcher {
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
                val symbolCount = spotDescriptionEditText.text.length
                descriptionSymbolCount.text = "$symbolCount/1500"
                if (symbolCount > 1500)
                    descriptionSymbolCount.setTextColor(getColor(R.color.red))
                else
                    descriptionSymbolCount.setTextColor(getColor(R.color.light_grey))
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text is changed.
            }
        })


        // Initialize the arrays inside the onCreate method
        spotTypeTexts = arrayOf(
            getString(R.string.choose_spot_type),
            getString(R.string.skatepark),
            getString(R.string.covered_skatepark),
            getString(R.string.street_spot),
            getString(R.string.diy_spot),
            getString(R.string.shop),
            getString(R.string.dirt)
        )
        spotTypeFirebaseTexts = arrayOf(
            getString(R.string.choose_spot_type),
            "Открытый скейтпарк",
            "Крытый скейтпарк",
            "Стрит",
            "D.I.Y",
            "Шоп",
            "Dirt"
        )

        spotTypeImages =
            intArrayOf(
                R.drawable.not_chosen_mark,
                R.drawable.park_spot_mark,
                R.drawable.covered_park_mark,
                R.drawable.street_spot_mark,
                R.drawable.diy_spot_mark,
                R.drawable.shop_mark,
                R.drawable.dirt_spot_mark
            )
        conditionTexts = arrayOf(
            getString(R.string.choose_spot_condition),
            getString(R.string.excelent),
            getString(R.string.good),
            getString(R.string.normal),
            getString(R.string.bad),
            getString(R.string.very_bad)
        )
        conditionFirebaseTexts = arrayOf(
            getString(R.string.choose_spot_condition),
            "Отличное",
            "Хорошее",
            "Нормальное",
            "Плохое",
            "Ужасное"
        )

        //Getting the instance of Spinner and applying OnItemSelectedListener on it
        val spinnerSpotType = findViewById<View>(R.id.spinner_spot_type) as Spinner
        spinnerSpotType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                chosenSpotType = spotTypeFirebaseTexts[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val customAdapter = SpotTypeAdapter(applicationContext, pref.getBoolean("NightRideMode", false), spotTypeImages, spotTypeTexts)
        spinnerSpotType.adapter = customAdapter
        findViewById<ImageView>(R.id.arrow_down_image_spot_type).setOnClickListener { spinnerSpotType.performClick() }


        val spinnerCondition = findViewById<Spinner>(R.id.spinner_condition)
        spinnerCondition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                chosenCondition = conditionFirebaseTexts[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        val customAdapterNoImage = SpotConditionAdapter(applicationContext, pref.getBoolean("NightRideMode", false), conditionTexts)
        spinnerCondition.adapter = customAdapterNoImage
        findViewById<ImageView>(R.id.arrow_down_image_condition).setOnClickListener { spinnerCondition.performClick() }

        mapView = findViewById(R.id.mapview)
        scrollViewAddSpot = findViewById(R.id.add_spot_scroll_view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapKit: MapKit = MapKitFactory.getInstance()
        locationOnMapKit = mapView.mapWindow?.let { mapKit.createUserLocationLayer(it) }!!
        locationOnMapKit.isVisible = true
        checkLocationPermissionAndAvailability()


        // Добавьте YMKMapInputListener к вашей карте

        mapObjectCollection =
            mapView.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте

        zoomFab.setOnClickListener {
            // Check for location permission and availability when the button is clicked
            checkLocationPermissionAndAvailability()
        }

        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            if (mapFrameLayout.visibility == View.VISIBLE || fromSpotReviewActivity)
                onBackPressedDispatcher.onBackPressed()
            else {
                findViewById<TextView>(R.id.actionbar_title).text = getString(R.string.tap_on_map_to_add_marker)
                mapFrameLayout.visibility = View.VISIBLE
                scrollViewAddSpot.visibility = View.GONE
            }
        }

        checkIcon.setOnClickListener {
            if (mapFrameLayout.visibility == View.VISIBLE) {
                if (placemarkMapObject != null) {
                    findViewById<TextView>(R.id.actionbar_title).text = getString(R.string.spot_info)
                    mapFrameLayout.visibility = View.GONE
                    scrollViewAddSpot.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.tap_on_map_to_add_marker_no_digits),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val image1 = findViewById<ImageView>(R.id.image1)
                val image2 = findViewById<ImageView>(R.id.image2)
                val image3 = findViewById<ImageView>(R.id.image3)
                val image4 = findViewById<ImageView>(R.id.image4)
                val image5 = findViewById<ImageView>(R.id.image5)

                var drawable = image1.drawable
                if (drawable is BitmapDrawable && !drawable.equals(getDrawable(R.drawable.rounded_rectangle_light_grey_with_plus))) {
                    imageBitmapArray.add(drawable.bitmap)
                }
                drawable = image2.drawable
                if (drawable is BitmapDrawable && !drawable.equals(getDrawable(R.drawable.rounded_rectangle_light_grey_with_plus))) {
                    imageBitmapArray.add(drawable.bitmap)
                }
                drawable = image3.drawable
                if (drawable is BitmapDrawable && !drawable.equals(getDrawable(R.drawable.rounded_rectangle_light_grey_with_plus))) {
                    imageBitmapArray.add(drawable.bitmap)
                }
                drawable = image4.drawable
                if (drawable is BitmapDrawable && !drawable.equals(getDrawable(R.drawable.rounded_rectangle_light_grey_with_plus))) {
                    imageBitmapArray.add(drawable.bitmap)
                }
                drawable = image5.drawable
                if (drawable is BitmapDrawable && !drawable.equals(getDrawable(R.drawable.rounded_rectangle_light_grey_with_plus))) {
                    imageBitmapArray.add(drawable.bitmap)
                }

                if (spotTitleEditText.text.trim().isEmpty() || spotDescriptionEditText.text.trim()
                        .isEmpty() || chosenSpotType.trim().isEmpty() ||
                    chosenCondition.trim().isEmpty() || chosenCondition.trim() == getString(R.string.choose_spot_condition) || imageBitmapArray.isEmpty() ||
                    chosenSpotType.trim() == getString(R.string.choose_spot_type)
                ) {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.all_fields_are_compulsory),
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (spotTitleEditText.text.length > 120 || spotDescriptionEditText.text.length > 1500) {
                  Toast.makeText(baseContext, getString(R.string.symbol_count_exceeded), Toast.LENGTH_SHORT).show()
                } else {
                    checkIcon.visibility = View.GONE
                    checkIconLoading.visibility = View.VISIBLE
                    //TODO: Username check:
                    db.collection("Users").document(auth.currentUser!!.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.get("username") == null || document.get("username")
                                    .toString().isEmpty()
                            ) {
                                checkIcon.visibility = View.VISIBLE
                                checkIconLoading.visibility = View.GONE
                                dialogUsernameSetUp()
                            } else {
                                checkIcon.visibility = View.VISIBLE
                                checkIconLoading.visibility = View.GONE
                                spotUploadToFirebase()
                            }
                        }.addOnFailureListener {
                            checkIcon.visibility = View.VISIBLE
                            checkIconLoading.visibility = View.GONE
                            dialogUsernameSetUp()
                        }


                }
            }
        }

        fromSpotReviewActivity = intent.getBooleanExtra("fromSpotReviewActivity", false)
        if (fromSpotReviewActivity) {
            mapFrameLayout.visibility = View.GONE
            scrollViewAddSpot.visibility = View.VISIBLE
            findViewById<TextView>(R.id.actionbar_title).text = getString(R.string.spot_editing)
            receivedLatitude = intent.getDoubleExtra("latitude", 0.0)
            receivedLongitude = intent.getDoubleExtra("longitude", 0.0)
            val receivedPoint = Point(receivedLatitude, receivedLongitude)
            location = receivedPoint
            val receivedTitle = intent.getStringExtra("title")
            val receivedDescription = intent.getStringExtra("description")
            val receivedCondition = intent.getStringExtra("condition")
            val receivedType = intent.getStringExtra("type")
            intent.getStringExtra("date")
            receivedProponent = intent.getStringExtra("proponent").toString()
            val imageUris = intent.parcelableArrayList<Uri>("imageUris")
            val receivedImagesList = ArrayList<Bitmap>()

            if (imageUris != null) {
                for (imageUri in imageUris) {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val receivedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (receivedBitmap != null) {
                        receivedImagesList.add(receivedBitmap)
                    }
                }
            }

            spotTitleEditText.setText(receivedTitle)
            spotDescriptionEditText.setText(receivedDescription)
            titleSymbolCount.text = "${receivedTitle!!.length}/120"
            descriptionSymbolCount.text = "${receivedDescription!!.length}/1500"

            var conditionIndex = 0
            for (i in conditionTexts.indices) {
                if (receivedCondition.equals(conditionTexts[i]))
                    conditionIndex = i
            }
            spinnerCondition.setSelection(conditionIndex)
            var spotTypeIndex = 0
            for (i in spotTypeTexts.indices) {
                if (receivedType.equals(spotTypeTexts[i]))
                    spotTypeIndex = i
            }
            spinnerSpotType.setSelection(spotTypeIndex)
            val image1IV = findViewById<ImageView>(R.id.image1)
            val image2IV = findViewById<ImageView>(R.id.image2)
            val image3IV = findViewById<ImageView>(R.id.image3)
            val image4IV = findViewById<ImageView>(R.id.image4)
            val image5IV = findViewById<ImageView>(R.id.image5)

            val imageIVArray =
                arrayListOf<ImageView>(image1IV, image2IV, image3IV, image4IV, image5IV)

            for (i in 0 until receivedImagesList.size) {
                imageIVArray[i].setImageBitmap(receivedImagesList[i])
                imageIVArray[i].background = null
            }
        }

        if (pref.getBoolean("NightRideMode", false)) {
            mapView.map?.isNightModeEnabled = true
            coordinatesTV.setTextColor(getColor(R.color.lighter_grey))
            zoomFab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.dark_theme)))
            findViewById<LinearLayout>(R.id.main_background_add_spot).setBackgroundColor(getColor(R.color.dark_theme))
            spotTitleEditText.setTextColor(getColor(R.color.lighter_grey))
            spotTitleEditText.setBackgroundColor(getColor(R.color.dark_theme))
            spotDescriptionEditText.setTextColor(getColor(R.color.lighter_grey))
            spotDescriptionEditText.setBackgroundColor(getColor(R.color.dark_theme))
            findViewById<ImageView>(R.id.arrow_down_image_spot_type).setColorFilter(getColor(R.color.light_grey))
            findViewById<ImageView>(R.id.arrow_down_image_condition).setColorFilter(getColor(R.color.light_grey))
            findViewById<ImageView>(R.id.image1).setBackgroundResource(R.drawable.rounded_rectangle_dark_theme_lighter_with_plus)
            findViewById<ImageView>(R.id.image2).setBackgroundResource(R.drawable.rounded_rectangle_dark_theme_lighter_with_plus)
            findViewById<ImageView>(R.id.image3).setBackgroundResource(R.drawable.rounded_rectangle_dark_theme_lighter_with_plus)
            findViewById<ImageView>(R.id.image4).setBackgroundResource(R.drawable.rounded_rectangle_dark_theme_lighter_with_plus)
            findViewById<ImageView>(R.id.image5).setBackgroundResource(R.drawable.rounded_rectangle_dark_theme_lighter_with_plus)
        }

    }


    /*inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

    inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
        SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
    }*/

    private inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
        SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
    }


    private fun spotUploadToFirebase() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_add_spot_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogBtnYes = dialog.findViewById<AppCompatButton>(R.id.button_confirm)
        val dialogBtnNo = dialog.findViewById<AppCompatButton>(R.id.button_no)
        dialogBtnYes.setOnClickListener {
            dialog.dismiss()
            val dialogLoading = Dialog(this)
            dialogLoading.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogLoading.setCancelable(false)
            dialogLoading.setContentView(R.layout.dialog_add_spot_loading)
            dialogLoading.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogLoading.show()
            val dialogLoadingDetails =
                dialogLoading.findViewById<TextView>(R.id.dialog_loading_details)
            dialogLoadingDetails.text = getString(R.string.image_proccessing)

            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val imagesRef = storageRef.child("SpotImages")
            val targetSizeInBytes = 500 * 1024 // 100 KB in bytes
            val uploadTasks = mutableListOf<Deferred<ByteArray>>() // Store upload tasks

            for (bitmap in imageBitmapArray) {
                val imageName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                imagesRef.child(imageName)

                val uploadTask = GlobalScope.async {
                    compressImageAsync(bitmap, targetSizeInBytes)
                }

                uploadTasks.add(uploadTask)
            }
            Log.d("uploadtaskscount", uploadTasks.size.toString())

            GlobalScope.launch {
                uploadTasks.awaitAll()

                runOnUiThread {
                    dialogLoadingDetails.text = getString(R.string.image_upload)
                }

                var imagesProcessedCount = 0

                for (uploadTask in uploadTasks) {
                    val finalData = uploadTask.await()

                    val imageName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                    val imageRef = imagesRef.child(imageName)

                    val uploadTask = imageRef.putBytes(finalData)

                    // Monitor the upload progress (optional)
                    uploadTask.addOnProgressListener { snapshot ->
                        (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                        // Update progress UI if needed
                        // dialogLoadingDetails.setText(getString(R.string.image_upload) + " ${progress.toString()}%")
                    }

                    // Handle successful upload
                    uploadTask.addOnSuccessListener {
                        // Get the download URL
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()
                            // Save the download URL to your array
                            imageStorageLinkArray.add(downloadUrl)

                            // Increment the counter for processed images
                            imagesProcessedCount++

                            // Check if all images are uploaded and links are saved
                            if (imagesProcessedCount == imageBitmapArray.size) {
                                dialogLoadingDetails.text = getString(R.string.spot_info_upload)
                                var spotProponent: String
                                db.collection("Users").document(auth.currentUser!!.uid).get()
                                    .addOnSuccessListener { document ->
                                        spotProponent = if (!fromSpotReviewActivity)
                                            document.get("username").toString()
                                        else
                                            receivedProponent

                                        val calendar = Calendar.getInstance()

                                        // Get the current date
                                        val currentDate = calendar.time

                                        // Format the current date as a string
                                        val dateFormatter = SimpleDateFormat("dd.MM.yyyy")
                                        val formattedDate = dateFormatter.format(currentDate)

                                        val newSpot = hashMapOf(
                                            "latitude" to location.latitude,
                                            "longitude" to location.longitude,
                                            "title" to spotTitleEditText.text.trim().toString(),
                                            "description" to spotDescriptionEditText.text.trim()
                                                .toString(),
                                            "type" to chosenSpotType,
                                            "condition" to chosenCondition,
                                            "proponent" to spotProponent,
                                            "date" to formattedDate
                                        )
                                        for (i in 0 until imageStorageLinkArray.size) {
                                            newSpot["image${i + 1}"] = imageStorageLinkArray[i]
                                        }

                                        var correctCollectionPath = "Moderation"
                                        if (fromSpotReviewActivity)
                                            correctCollectionPath = "Edit"

                                        val moderationCollection =
                                            db.collection(correctCollectionPath)


                                        val documentName =
                                            location.latitude.toString() + location.longitude.toString()
                                        moderationCollection.document(documentName)
                                            .set(newSpot)
                                            .addOnSuccessListener {
                                                db.collection("Users")
                                                    .document(auth.currentUser!!.uid)
                                                    .get().addOnSuccessListener { document ->
                                                        if (document.get("proposed") == null) {
                                                            var tempProposedCount = 1
                                                            if (fromSpotReviewActivity)
                                                                tempProposedCount = 0
                                                            val data = hashMapOf(
                                                                "username" to document.get("username"),
                                                                "proposed" to tempProposedCount
                                                            )
                                                            db.collection("Users")
                                                                .document(auth.currentUser!!.uid)
                                                                .update(data)
                                                                .addOnSuccessListener {
                                                                    dialogLoading.dismiss()
                                                                    val dialogFinished =
                                                                        Dialog(this@AddSpotActivity)
                                                                    dialogFinished.requestWindowFeature(
                                                                        Window.FEATURE_NO_TITLE
                                                                    )
                                                                    dialogFinished.setCancelable(
                                                                        true
                                                                    )
                                                                    dialogFinished.setContentView(R.layout.dialog_add_spot_finished)
                                                                    dialogFinished.window?.setBackgroundDrawable(
                                                                        ColorDrawable(Color.TRANSPARENT)
                                                                    )
                                                                    val okBtnDialog =
                                                                        dialogFinished.findViewById<AppCompatButton>(
                                                                            R.id.button_ok
                                                                        )
                                                                    okBtnDialog.setOnClickListener {
                                                                        dialogFinished.dismiss()
                                                                        onBackPressedDispatcher.onBackPressed()
                                                                    }
                                                                    dialogFinished.show()
                                                                    if (fromSpotReviewActivity)
                                                                        dialogFinished.findViewById<TextView>(
                                                                            R.id.dialog_content
                                                                        ).text = getString(R.string.changes_were_sent)
                                                                }
                                                        } else {
                                                            val proposedCount =
                                                                (document.get("proposed") as Long).toInt()
                                                            var tempProposedIncrement = 1
                                                            if (fromSpotReviewActivity)
                                                                tempProposedIncrement = 0
                                                            val data = hashMapOf(
                                                                "username" to document.get("username"),
                                                                "proposed" to proposedCount + tempProposedIncrement
                                                            )
                                                            db.collection("Users")
                                                                .document(auth.currentUser!!.uid)
                                                                .update(data)
                                                                .addOnSuccessListener {
                                                                    dialogLoading.dismiss()
                                                                    val dialogFinished =
                                                                        Dialog(this@AddSpotActivity)
                                                                    dialogFinished.requestWindowFeature(
                                                                        Window.FEATURE_NO_TITLE
                                                                    )
                                                                    dialogFinished.setCancelable(
                                                                        true
                                                                    )
                                                                    dialogFinished.setContentView(R.layout.dialog_add_spot_finished)
                                                                    dialogFinished.window?.setBackgroundDrawable(
                                                                        ColorDrawable(Color.TRANSPARENT)
                                                                    )
                                                                    val okBtnDialog =
                                                                        dialogFinished.findViewById<AppCompatButton>(
                                                                            R.id.button_ok
                                                                        )
                                                                    okBtnDialog.setOnClickListener {
                                                                        dialogFinished.dismiss()
                                                                        onBackPressedDispatcher.onBackPressed()
                                                                    }
                                                                    dialogFinished.show()
                                                                }
                                                        }
                                                    }

                                            }
                                            .addOnFailureListener {
                                                // Handle errors
                                            }
                                    }
                            }
                        }
                    }

                    // Handle upload failures (if needed)
                    uploadTask.addOnFailureListener {
                        // Handle the error
                    }
                }
            }
        }

        dialogBtnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        if (fromSpotReviewActivity)
            dialog.findViewById<TextView>(R.id.dialog_content).text = getString(R.string.confirm_spot_edit)
    }


    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate the scale factor to fit within the specified dimensions
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scaleFactor = min(scaleWidth, scaleHeight)

        // Create a new scaled bitmap
        val matrix = Matrix()
        matrix.postScale(scaleFactor, scaleFactor)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }


    private fun compressImageAsync(bitmap: Bitmap, targetSizeInBytes: Int): ByteArray {
        var compressionQuality = 100 // Start with a reasonable quality
        val baos = ByteArrayOutputStream()

        // Compress the bitmap to WebP format
        if (SDK_INT >= Build.VERSION_CODES.R) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, compressionQuality, baos)
        } else
            bitmap.compress(Bitmap.CompressFormat.WEBP, compressionQuality, baos)

        val data = baos.toByteArray()
        val dataSize = data.size
        Log.d("dataSizeBytes", dataSize.toString())

        if (dataSize > targetSizeInBytes) {
            val targetSizeToDataSizePercentage: Float =
                targetSizeInBytes.toFloat() / dataSize.toFloat()
            val timesSmallerTarget = 1 / targetSizeToDataSizePercentage
            compressionQuality = (100 / timesSmallerTarget).toInt()
            if (compressionQuality == 0) {
                compressionQuality = 1
            }

            // Compress the bitmap again with the updated compression quality
            baos.reset() // Clear the existing data in the stream

            // Compress to WebP format with the new compression quality
            if (SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, compressionQuality, baos)
            } else
                bitmap.compress(Bitmap.CompressFormat.WEBP, compressionQuality, baos)
        }

        val finalData = baos.toByteArray()
        Log.d("finalDataYes", "one finalData is defined")

        return finalData
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
        okBtn.setOnClickListener {
            dialog.dismiss()
            spotUploadToFirebase()
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
                        if (usernameEntered == document.get("username")) {
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
                        db.collection("Users").document(auth.currentUser!!.uid).update(data as kotlin.collections.Map<String, Any>)
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
                        db.collection("Users").document(auth.currentUser!!.uid).update(data as kotlin.collections.Map<String, Any>)
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


    override fun onBackPressed() {
        //TODO: Remove all deprecations
        if (mapFrameLayout.visibility == View.VISIBLE)
            onBackPressedDispatcher.onBackPressed()
        else {
            findViewById<TextView>(R.id.actionbar_title).text = getString(R.string.tap_on_map_to_add_marker)
            mapFrameLayout.visibility = View.VISIBLE
            scrollViewAddSpot.visibility = View.GONE
        }
    }


    private fun createBitmapFromVector(art: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, art) ?: return null
        val bitmap = Bitmap.createBitmap(
            80,
            80,
            Bitmap.Config.ARGB_8888
        ) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    private fun setMarkerInLocation(location: Point) { //TODO: Should take all the info from firebase
        val marker = createBitmapFromVector(R.drawable.map_circle_mark)
        mapObjectCollection =
            mapView.map!!.mapObjects // Инициализируем коллекцию различных объектов на карте
        placemarkMapObject =
            mapObjectCollection.addPlacemark(location, ImageProvider.fromBitmap(marker))
        coordinatesTV.text = "${location.latitude}\n${location.longitude}"
    }

    private fun deleteMarker() {
        // Check if placemarkMapObject is not null
        placemarkMapObject?.let {
            // Remove the placemark from the mapObjectCollection
            mapObjectCollection.remove(it)
            // Set placemarkMapObject to null to indicate it has been removed
            placemarkMapObject = null
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
                123
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
                    mapView.map?.move(
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


    fun onImageClick(view: View) {  //pick photos
        val imageView = view as ImageView

        // Open a gallery intent to select an image
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)


        // Pass the clicked ImageView to onActivityResult to update its background later
        selectedImageView = imageView
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapInputListener = object : InputListener {
            override fun onMapTap(p0: Map, p1: Point) {
                val latitude = p1.latitude
                val longitude = p1.longitude
                location = Point(latitude, longitude)
                deleteMarker()
                setMarkerInLocation(location)
            }

            override fun onMapLongTap(p0: Map, p1: Point) {
            }
        }
        mapView.map.addInputListener(mapInputListener)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

}
