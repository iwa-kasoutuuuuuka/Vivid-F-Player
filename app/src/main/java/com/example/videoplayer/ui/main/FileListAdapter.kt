package com.example.videoplayer.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.databinding.ItemVideoFileBinding
import com.example.videoplayer.data.manager.ResumeManager

class FileListAdapter(private val onClick: (VideoFile) -> Unit) :
    ListAdapter<VideoFile, FileListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemVideoFileBinding, private val resumeManager: ResumeManager) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: VideoFile, onClick: (VideoFile) -> Unit) {
            binding.tvFileName.text = file.name
            
            val pos = resumeManager.getFileResumePosition(file.uri.toString())
            val dur = resumeManager.getFileDuration(file.uri.toString())
            if (pos > 0 && dur > 0) {
                binding.pbVideoProgress.visibility = android.view.View.VISIBLE
                binding.pbVideoProgress.progress = ((pos * 100) / dur).toInt()
            } else {
                binding.pbVideoProgress.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, ResumeManager(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<VideoFile>() {
        override fun areItemsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem.uri == newItem.uri
        override fun areContentsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem == newItem
    }
}
