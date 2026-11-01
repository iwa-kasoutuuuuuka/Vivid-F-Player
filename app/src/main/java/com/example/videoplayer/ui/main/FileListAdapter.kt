package com.example.videoplayer.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.R
import com.example.videoplayer.data.manager.ResumeManager
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.databinding.ItemVideoFileBinding
import com.example.videoplayer.util.ThumbnailLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FileListAdapter(
    private val coroutineScope: CoroutineScope,
    private val onClick: (VideoFile) -> Unit
) : ListAdapter<VideoFile, FileListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemVideoFileBinding,
        private val resumeManager: ResumeManager,
        private val coroutineScope: CoroutineScope
    ) : RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null

        fun bind(file: VideoFile, onClick: (VideoFile) -> Unit) {
            binding.tvFileName.text = file.name
            
            val pos = resumeManager.getFileResumePosition(file.uri.toString())
            val dur = resumeManager.getFileDuration(file.uri.toString())
            if (pos > 1000L && dur > 0L) {
                binding.pbVideoProgress.visibility = android.view.View.VISIBLE
                binding.pbVideoProgress.progress = ((pos * 100) / dur).toInt().coerceIn(1, 100)
                binding.tvUnwatchedBadge.visibility = android.view.View.GONE
            } else {
                binding.pbVideoProgress.visibility = android.view.View.GONE
                binding.tvUnwatchedBadge.visibility = if (pos <= 1000L) android.view.View.VISIBLE else android.view.View.GONE
            }

            // サムネイルの読み込み (キャッシュ優先・ローカルのみ非同期フェッチ)
            thumbnailJob?.cancel()
            val uriStr = file.uri.toString()
            binding.ivThumbnail.tag = uriStr

            val cached = ThumbnailLoader.getCached(uriStr)
            if (cached != null) {
                binding.ivThumbnail.setImageBitmap(cached)
                binding.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_video)
                binding.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                if (file.uri.scheme != "smb") {
                    thumbnailJob = coroutineScope.launch {
                        val bitmap = ThumbnailLoader.loadThumbnail(binding.root.context, file.uri)
                        if (binding.ivThumbnail.tag == uriStr && bitmap != null) {
                            binding.ivThumbnail.setImageBitmap(bitmap)
                            binding.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    }
                }
            }

            binding.root.setOnClickListener { onClick(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, ResumeManager(parent.context), coroutineScope)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<VideoFile>() {
        override fun areItemsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem.uri == newItem.uri
        override fun areContentsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean = oldItem == newItem
    }
}
