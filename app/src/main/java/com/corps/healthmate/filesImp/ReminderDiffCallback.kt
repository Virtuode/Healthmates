package com.corps.healthmate.filesImp

import androidx.recyclerview.widget.DiffUtil
import com.corps.healthmate.database.Reminder

class ReminderDiffCallback(
    private val oldList: List<Reminder>,
    private val newList: List<Reminder>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id // Assuming Reminder has an id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}