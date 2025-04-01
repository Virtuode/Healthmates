package com.corps.healthmate.filesImp

import androidx.recyclerview.widget.DiffUtil
import com.corps.healthmate.models.Badge

class BadgeDiffCallback : DiffUtil.ItemCallback<Badge>() {
    override fun areItemsTheSame(oldItem: Badge, newItem: Badge): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Badge, newItem: Badge): Boolean = oldItem == newItem
}