package com.hieuta.yudoviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails

class VideoListAdapter(private val videos: List<SearchResultVideoDetails>) :
    RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_list_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description_text_view)
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnail_image_view)

        fun bind(video: SearchResultVideoDetails) {
            titleTextView.text = video.title()
            descriptionTextView.text = video.description()
            thumbnailImageView.load(video.thumbnails().last()) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
            }
        }
    }
}
