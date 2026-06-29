package com.example.firedatabase_assis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

//Previews the image that was just taken by the camera, also used for the gallery when a user clicks on a picture
class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)//loads the layout

        val imageView = findViewById<ImageView>(R.id.imageView)//finds imageView from layout 
        //gets image uri from previous activity(GalleryActivity or camera)
        val uriString = intent.getStringExtra("image_uri")

        if (uriString == null) {//if image uri was not received, close activty
            Log.e("PREVIEW", "No URI received")
            finish()
            return
        }

        val uri = Uri.parse(uriString)//converts uri string to uri to use

        //Glide handles loading properly, avoids rotation issues in most cases
        Glide.with(this)
            .load(uri)
            .fitCenter()
            .into(imageView)
        //results page button
        val button = findViewById<Button>(R.id.go_to_results_btn)

        button.setOnClickListener {
            val intent = Intent(this, results::class.java)
            startActivity(intent)
        }
    }
}
