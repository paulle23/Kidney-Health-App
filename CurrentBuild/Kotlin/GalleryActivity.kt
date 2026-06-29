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
//Gallery Page that shows all of the user photos
//inherits from AppCompatActivity
class GalleryActivity : AppCompatActivity() {

    private lateinit var imageList: ArrayList<Uri>//list that stores the uris of all images
    private lateinit var recyclerView: RecyclerView//ui component that displays all images
    private lateinit var adapter: GalleryAdapter//connects imageList to recyclerView

    override fun onCreate(savedInstanceState: Bundle?) {//when gallery screen opens
        super.onCreate(savedInstanceState)//calls parent activity's oncreate
        setContentView(R.layout.activity_gallery)//loads layout file

        recyclerView = findViewById(R.id.recyclerView)//finds recyclerview from xml

        imageList = ArrayList()//empty list that will hold image uris
        //creates the gallery adapter
        adapter = GalleryAdapter(imageList) { uri ->//lambda function that runs when user taps image
            val intent = Intent(this, PreviewActivity::class.java)//to open previewActivity
            intent.putExtra("image_uri", uri.toString())//sends image uri to next activity
            startActivity(intent)
        }
        //tells recyclerview to use a grid with 3 per row
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter//connects adapter to recyclerview

        loadUserImages()
    }
    /*searches phone's gallery for images saved by app*/
    private fun loadUserImages() {
        val username = UserSession.username ?: return//gets logged in username
        //points to external image storage
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        //sql style filter to get correct images
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%MyCameraApp/$username/%")
        //searches device's image database
        val cursor = contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        imageList.clear()//removes old images before loading new

        cursor?.use {//use automatically closes cursor when done
            //finds column that contains image ids
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            //loops through images 
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)//current image id

                val uri = ContentUris.withAppendedId(//builds uri
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                imageList.add(uri)//add the uri to the list
            }
        }

        adapter.notifyDataSetChanged()//refresh recyclerView so that new images show
    }
}
