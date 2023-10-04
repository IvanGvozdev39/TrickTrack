package com.tricktrack.tricktrack

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import java.io.ByteArrayOutputStream
import java.lang.Double.min
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
    private lateinit var zoomFab: FloatingActionButton
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_spot)

        mapFrameLayout = findViewById(R.id.map_frame)
        zoomFab = findViewById(R.id.zoom_fab)
        coordinatesTV = findViewById(R.id.coordinates_text_view)
        spotTitleEditText = findViewById(R.id.spot_title_edit_text)
        spotDescriptionEditText = findViewById(R.id.spot_description_edit_text)
        FirebaseApp.initializeApp(this)

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK && data != null) {
                val selectedImageUri = data.data

                // Load the selected image into the clicked ImageView
                selectedImageView.setImageURI(selectedImageUri)

                // Set the background to null to remove the plus sign drawable
                selectedImageView.background = null
            }
        }


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
                chosenSpotType = spotTypeFirebaseTexts.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val customAdapter = SpotTypeAdapter(applicationContext, spotTypeImages, spotTypeTexts)
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
                chosenCondition = conditionFirebaseTexts.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        val customAdapterNoImage = Adapter(applicationContext, conditionTexts)
        spinnerCondition.adapter = customAdapterNoImage
        findViewById<ImageView>(R.id.arrow_down_image_condition).setOnClickListener { spinnerCondition.performClick() }

        mapView = findViewById<MapView>(R.id.mapview)
        scrollViewAddSpot = findViewById(R.id.add_spot_scroll_view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var mapKit: MapKit = MapKitFactory.getInstance()
        locationOnMapKit = mapView?.mapWindow?.let { mapKit.createUserLocationLayer(it) }!!
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
                findViewById<TextView>(R.id.actionbar_title).setText(getString(R.string.tap_on_map_to_add_marker))
                mapFrameLayout.visibility = View.VISIBLE
                scrollViewAddSpot.visibility = View.GONE
            }
        }

        findViewById<ImageView>(R.id.check_icon).setOnClickListener {
            if (mapFrameLayout.visibility == View.VISIBLE) {
                if (placemarkMapObject != null) {
                    findViewById<TextView>(R.id.actionbar_title).setText(getString(R.string.spot_info))
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
                var image1 = findViewById<ImageView>(R.id.image1)
                var image2 = findViewById<ImageView>(R.id.image2)
                var image3 = findViewById<ImageView>(R.id.image3)
                var image4 = findViewById<ImageView>(R.id.image4)
                var image5 = findViewById<ImageView>(R.id.image5)

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
                    chosenCondition.trim().isEmpty() || chosenCondition.trim()
                        .equals(getString(R.string.choose_spot_condition)) || imageBitmapArray.isEmpty() ||
                    chosenSpotType.trim().equals(getString(R.string.choose_spot_type))
                ) {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.all_fields_are_compulsory),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //TODO: Username check:
                    db.collection("Users").document(auth.currentUser!!.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.get("username") == null || document.get("username")
                                    .toString().isEmpty()
                            ) {
                                dialogUsernameSetUp()
                            } else {
                                spotUploadToFirebase()
                            }
                        }.addOnFailureListener {
                            dialogUsernameSetUp()
                        }


                }
            }
        }

        fromSpotReviewActivity = intent.getBooleanExtra("fromSpotReviewActivity", false)
        if (fromSpotReviewActivity) {
            mapFrameLayout.visibility = View.GONE
            scrollViewAddSpot.visibility = View.VISIBLE
            findViewById<TextView>(R.id.actionbar_title).setText(getString(R.string.spot_editing))
            receivedLatitude = intent.getDoubleExtra("latitude", 0.0)
            receivedLongitude = intent.getDoubleExtra("longitude", 0.0)
            val receivedPoint = Point(receivedLatitude, receivedLongitude)
            location = receivedPoint
            val receivedTitle = intent.getStringExtra("title")
            val receivedDescription = intent.getStringExtra("description")
            val receivedCondition = intent.getStringExtra("condition")
            val receivedType = intent.getStringExtra("type")
            val receivedDate = intent.getStringExtra("date")
            receivedProponent = intent.getStringExtra("proponent").toString()
            val imageUris = intent.getParcelableArrayListExtra<Uri>("imageUris")
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
            var conditionIndex = 0
            for (i in 0 until conditionTexts.size) {
                if (receivedCondition.equals(conditionTexts[i]))
                    conditionIndex = i
            }
            spinnerCondition.setSelection(conditionIndex)
            var spotTypeIndex = 0
            for (i in 0 until spotTypeTexts.size) {
                if (receivedType.equals(spotTypeTexts[i]))
                    spotTypeIndex = i
            }
            spinnerSpotType.setSelection(spotTypeIndex)
            val image1IV = findViewById<ImageView>(R.id.image1)
            val image2IV = findViewById<ImageView>(R.id.image2)
            val image3IV = findViewById<ImageView>(R.id.image3)
            val image4IV = findViewById<ImageView>(R.id.image4)
            val image5IV = findViewById<ImageView>(R.id.image5)

            val imageIVArray = arrayListOf<ImageView>(image1IV, image2IV, image3IV, image4IV, image5IV)

            for (i in 0 until receivedImagesList.size) {
                imageIVArray[i].setImageBitmap(receivedImagesList[i])
                imageIVArray[i].background = null
            }
        }

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

            //TODO: Final confirm and Firebase upload:
            dialog.dismiss()
            val dialogLoading = Dialog(this)
            dialogLoading.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogLoading.setCancelable(false)
            dialogLoading.setContentView(R.layout.dialog_add_spot_loading)
            dialogLoading.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogLoading.show()
            var dialogLoadingDetails =
                dialogLoading.findViewById<TextView>(R.id.dialog_loading_details)
            dialogLoadingDetails.setText(getString(R.string.image_upload))

            //Uploading images to Storage
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

// Initialize a reference to the SpotImages folder
            val imagesRef = storageRef.child("SpotImages")

// Iterate through your bitmap images
            val targetSizeInBytes = 100 * 1024 // 100 KB in bytes

            for ((index, bitmap) in imageBitmapArray.withIndex()) {
                // Create a unique filename for each image (you can use any logic)
                val imageName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val imageRef = imagesRef.child(imageName)

                // Initialize compression quality and dimensions
                var compressionQuality = 80 // Start with a reasonable quality
                var width = bitmap.width
                var height = bitmap.height

                // Compress the image while checking the size
                val baos = ByteArrayOutputStream()
                lateinit var resizedBitmap: Bitmap
                lateinit var data: ByteArray
                do {
                    // Resize the image dimensions (optional)
                    if (width > 2048 || height > 2048) {
                        // Resize the image to a maximum dimension of 2048 pixels
                        val scaleFactor = min(2048.0 / width, 2048.0 / height)
                        width = (width * scaleFactor).toInt()
                        height = (height * scaleFactor).toInt()
                        resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                        bitmap.recycle() // Recycle the original bitmap
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, baos)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, baos)
                    }

                    // Compress the bitmap with the current quality setting

                    data = baos.toByteArray()

                    // Check the size of the compressed image
                    if (data.size > targetSizeInBytes) {
                        // If the size is too large, reduce the quality and try again
                        compressionQuality -= 5 // Adjust the quality reduction step as needed
                        baos.reset() // Reset the ByteArrayOutputStream
                    } else {
                        // If the size is within the target range, break out of the loop
                        break
                    }
                } while (compressionQuality > 0)

                // Upload the compressed image to Firebase Storage
                val uploadTask = imageRef.putBytes(data)

                // Monitor the upload progress (optional)
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                    // Update progress UI if needed
//                        dialogLoadingDetails.setText(getString(R.string.image_upload) + " ${progress.toString()}%")
                }

                // Handle successful upload
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        // Save the download URL to your array
                        imageStorageLinkArray.add(downloadUrl)

                        // Check if all images are uploaded and links are saved
                        if (imageStorageLinkArray.size == imageBitmapArray.size) {
                            dialogLoadingDetails.setText(getString(R.string.spot_info_upload))
                            var spotProponent = ""
                            db.collection("Users").document(auth.currentUser!!.uid).get()
                                .addOnSuccessListener { document ->
                                    if (!fromSpotReviewActivity)
                                        spotProponent = document.get("username").toString()
                                    else
                                        spotProponent = receivedProponent

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
                                    for (i in 0..imageStorageLinkArray.size - 1) {
                                        newSpot.put("image${i + 1}", imageStorageLinkArray.get(i))
                                    }

                                    var correctCollectionPath = "Spots"
                                    if (fromSpotReviewActivity)
                                        correctCollectionPath = "Edit"

                                    val moderationCollection = db.collection(correctCollectionPath) //TODO: !!!!!!!!!!!CHANGE TO "MODERATION" BEFORE GOING IN PRODUCTION!!!!!!!!!!!!!1



                                    val documentName =
                                        location.latitude.toString() + location.longitude.toString()
                                    val documentId = documentName
                                    moderationCollection.document(documentId)
                                        .set(newSpot)
                                        .addOnSuccessListener { documentReference ->
                                            db.collection("Users").document(auth.currentUser!!.uid)
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
                                                        .document(auth.currentUser!!.uid).set(data)
                                                        .addOnSuccessListener {
                                                            dialogLoading.dismiss()
                                                            val dialogFinished = Dialog(this)
                                                            dialogFinished.requestWindowFeature(
                                                                Window.FEATURE_NO_TITLE
                                                            )
                                                            dialogFinished.setCancelable(true)
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
                                                                dialogFinished.findViewById<TextView>(R.id.dialog_content).setText(getString(R.string.changes_were_sent))
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
                                                        .document(auth.currentUser!!.uid).set(data)
                                                        .addOnSuccessListener {
                                                            dialogLoading.dismiss()
                                                            val dialogFinished = Dialog(this)
                                                            dialogFinished.requestWindowFeature(
                                                                Window.FEATURE_NO_TITLE
                                                            )
                                                            dialogFinished.setCancelable(true)
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
                                        .addOnFailureListener { e ->
                                            // Handle errors
                                        }
                                }
                        }
                    }
                }

                // Handle upload failures (if needed)
                uploadTask.addOnFailureListener { exception ->
                    // Handle the error
                }
            }


        }
        dialogBtnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
        if (fromSpotReviewActivity)
            dialog.findViewById<TextView>(R.id.dialog_content).setText(getString(R.string.confirm_spot_edit))
    }


    private fun dialogUsernameSetUp() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
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
                        db.collection("Users").document(auth.currentUser!!.uid).set(data)
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
                        db.collection("Users").document(auth.currentUser!!.uid).set(data)
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
            findViewById<TextView>(R.id.actionbar_title).setText(getString(R.string.tap_on_map_to_add_marker))
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
        coordinatesTV.setText("${location.latitude}\n${location.longitude}")
    }

    private fun deleteMarker() {
        // Check if placemarkMapObject is not null
        placemarkMapObject?.let {
            // Remove the placemark from the mapObjectCollection
            mapObjectCollection?.remove(it)
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
            override fun onMapTap(p0: com.yandex.mapkit.map.Map, p1: Point) {
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
