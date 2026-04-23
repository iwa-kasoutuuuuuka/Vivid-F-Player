package com.example.videoplayer.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.databinding.ItemVideoFileBinding

class FileListAdapter(private val onClick: (VideoFile) -> Unit) :
    ListAdapter<VideoFile, FileListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemVideoFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: VideoFile, onClick: (VideoFile) -> Unit) {
            val displayName = if (file.name.length > 20) {
                file.name.take(17) + "..."
            } else {
                file.name
            }
            binding.tvFileName.text = displayName
            binding.root.setOnClickListener { onClick(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<VideoFile>() {
        override fun areItemsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem.uri == newItem.uri
        override fun areContentsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem == newItem
    }
}
