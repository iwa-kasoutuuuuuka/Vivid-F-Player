package com.example.videoplayer.ui.main

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.databinding.ItemFolderBinding

class FolderListAdapter(
    private val onClick: (Uri) -> Unit,
    private val onDelete: (Uri) -> Unit
) : ListAdapter<Uri, FolderListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, onClick: (Uri) -> Unit, onDelete: (Uri) -> Unit) {
            binding.tvFolderName.text = uri.lastPathSegment ?: uri.toString()
            binding.root.setOnClickListener { onClick(uri) }
            binding.btnDelete.setOnClickListener { onDelete(uri) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onDelete)
    }

    object DiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
    }
}
