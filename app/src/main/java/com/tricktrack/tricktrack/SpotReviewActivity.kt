package com.tricktrack.tricktrack

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spot_review)

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
                    descriptionTV.setText(description)
                    conditionTV.setText(condition)
                    typeTV.setText(type)
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
                    Toast.makeText(this, getString(R.string.no_map_app_on_device), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun bookmarkBtnListenerSetup() {
        bookmarkBtn.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                if (!spotInFavorites) {
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_filled_icon))
                    val data = hashMapOf(
                        "title" to title,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "type" to type
                    )
                    db.collection("Users").document(auth.currentUser!!.uid).collection("FavoriteSpots")
                        .document(latitude.toString()+longitude.toString()).set(data).addOnSuccessListener {
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
                //TODO: suggest sign in:
                googleSignInDialogShowup(true, false)
            }
        }
    }


    private fun googleSignInDialogShowup(bookmarkClicked: Boolean, deleteClicked: Boolean) {
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
        val dialogGoogleSignInBtn = dialogGoogleSignIn.findViewById<MaterialButton>(R.id.google_sign_in_button)
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
    }


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
                                    googleSignInDialogShowup(false, false)
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
                                    googleSignInDialogShowup(false, true)
                                } else {
                                    val dialog = Dialog(this)
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setCancelable(true)
                                    dialog.setContentView(R.layout.dialog_delete_confirm)
                                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                    val dialogContent = dialog.findViewById<TextView>(R.id.dialog_content)
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
                        Log.d("asasassin", "started adding data to firestore")
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

}