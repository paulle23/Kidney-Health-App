package com.example.firedatabase_assis

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.PreviewActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.UserSession

class GalleryActivity : AppCompatActivity() {

    private lateinit var imageList: ArrayList<Uri>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerView)

        imageList = ArrayList()

        adapter = GalleryAdapter(imageList) { uri ->
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("image_uri", uri.toString())
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter

        loadUserImages()
    }

    private fun loadUserImages() {
        val username = UserSession.username ?: return

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%MyCameraApp/$username/%")

        val cursor = contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        imageList.clear()

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                imageList.add(uri)
            }
        }

        adapter.notifyDataSetChanged()
    }
}