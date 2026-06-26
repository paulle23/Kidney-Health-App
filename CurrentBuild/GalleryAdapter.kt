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
class GalleryAdapter(
    private val images: List<Uri>,
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]

        Glide.with(holder.imageView.context)
            .load(uri)
            .centerCrop()
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick(uri)
        }
    }

    override fun getItemCount(): Int = images.size
}
