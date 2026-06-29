package com.example.firedatabase_assis

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.R

//Recycler Adapter to implement the gallery
//create image views, load images, and handle click events
//Glide is the image loading library that loads and caches images
class GalleryAdapter(
/*two parameters: list containing uris of all images to display and 
click listener that sends image uri back to galleryactivity when image is tapped*/
    private val images: List<Uri>,
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {
    //viewHolder is one image inside recyclerView, used by RecyclerView to improve performance
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageItem)
    }
    //called whenever RecyclerView needs new item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)//gets layoutinflater
            .inflate(R.layout.item_image, parent, false)//converts xml to view object
        return ImageViewHolder(view)//puts view inside a viewholder and returns it
    }
    //fills each viewholder with data, called everytime Recycler views needs image to appear 
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]//gets image uri at current position
        //starts glide
        Glide.with(holder.imageView.context)
            .load(uri)
            .centerCrop()//scales image so it fills imageView
            .into(holder.imageView)//displays it inside imageView
        /*click listener for when user clicks on image, passes uri to 
        GalleryActivity so that Preview shows it full screen*/
        holder.itemView.setOnClickListener {
            onItemClick(uri)
        }
    }
    //returns total number of images in list
    override fun getItemCount(): Int = images.size
}
