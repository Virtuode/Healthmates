package com.corps.healthmate.filesImp

import androidx.recyclerview.widget.DiffUtil
import com.corps.healthmate.models.DisplayAppointment

class AppointmentDiffCallback(
    private val oldList: List<DisplayAppointment>,
    private val newList: List<DisplayAppointment>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Check if the appointments have the same unique identifier (e.g., appointment ID)
        return oldList[oldItemPosition].appointment.id == newList[newItemPosition].appointment.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Check if the content of the appointments is the same
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
