package com.mab.documentuploader

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val permsRequestCode = 200
    private val requestCode = 201
    private val perms = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val firebaseStorage by lazy {
        FirebaseStorage.getInstance().reference
    }

    private val firebaseDatabase by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private val keysRef by lazy {
        firebaseDatabase.child("documentsKeys")
    }

    private val documentRef by lazy {
        firebaseStorage.child("validDocuments")
    }

    private val TAG = MainActivity::class.java.simpleName
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListeners()
    }

    public override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser == null)
            loginUser()
    }

    private fun setListeners() {
        upload_button.setOnClickListener {
            openCamera()
        }
    }


    private var imageUri: Uri? = null
    private fun openCamera() {
        if (checkForPermission()) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, requestCode)
        }
    }

    private fun checkForPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, perms, permsRequestCode)
            false
        } else {
            true
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permsRequestCode -> {
                if (grantResults.isEmpty())
                    return
                val locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                val storageAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED
                if (locationAccepted && cameraAccepted && storageAccepted) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Need all permissions", Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {
            try {
                val imageurl = imageUri?.getRealPathFromURI(contentResolver)
                createDBImage(imageurl!!)
                Log.d(TAG, "Path : $imageurl")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun createDBImage(path: String) {
        loader.visibility = View.VISIBLE
        loader.bringToFront()
        val file = Uri.fromFile(File(path))
        val riversRef = documentRef.child(file.lastPathSegment!!)
        val uploadTask = riversRef.putFile(file)

        uploadTask.addOnFailureListener {
            loader.visibility = View.GONE
            showAlert("Uploading Failed.")
        }.addOnSuccessListener {
            keysRef.child(file.lastPathSegment!!.split(".")[0]).setValue("key")
            loader.visibility = View.GONE
            showAlert("Uploading Done.")
        }
    }


    private fun showAlert(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Status")
            .setMessage(msg)
            .setPositiveButton("Ok", null)
            .show()
    }

    private fun loginUser() {
        loader.visibility = View.VISIBLE
        loader.bringToFront()
        firebaseAuth.signInWithEmailAndPassword("admin@gmail.com", "123456")
            .addOnCompleteListener(this) { task ->
                loader.visibility = View.GONE
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
