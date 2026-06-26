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
        setContentView(R.layout.activity_preview)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val uriString = intent.getStringExtra("image_uri")

        if (uriString == null) {
            Log.e("PREVIEW", "No URI received")
            finish()
            return
        }

        val uri = Uri.parse(uriString)

        // ✅ GLIDE FIX (handles loading properly, avoids rotation issues in most cases)
        Glide.with(this)
            .load(uri)
            .fitCenter()
            .into(imageView)

        val button = findViewById<Button>(R.id.go_to_results_btn)

        button.setOnClickListener {
            val intent = Intent(this, results::class.java)
            startActivity(intent)
        }
    }
}
