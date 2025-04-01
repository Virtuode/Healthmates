package com.corps.healthmate.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.corps.healthmate.R
import com.corps.healthmate.models.DoctorSummary
import de.hdodenhof.circleimageview.CircleImageView

class DoctorAdapter(
    private val context: Context,
    private var doctorList: List<DoctorSummary>,
    private val onDoctorClickListener: OnDoctorClickListener
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    fun updateDoctorList(updatedList: List<DoctorSummary>) {
        doctorList = updatedList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.doctor_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctorList[position]
        holder.nameTextView.text = if (doctor.name != null) "Dr. ${doctor.name}" else "N/A"
        holder.specializationTextView.text = doctor.specialization ?: "N/A"
        val experience = doctor.experience?.toIntOrNull() ?: 0
        holder.experienceTextView.text = context.getString(R.string.experience_text, experience)
        holder.tvEducation.text = context.getString(R.string.qualification_text, doctor.education ?: "N/A")

        Glide.with(context)
            .load(doctor.imageUrl)
            .placeholder(R.drawable.user)
            .error(R.drawable.user)
            .into(holder.imageView)

        Log.d("DoctorAdapter", "Doctor: ${doctor.name}, IsVerified: ${doctor.isVerified}")
        if (doctor.isVerified == true) {
            holder.verificationStatusImageView.setImageResource(R.drawable.verifiedicon)
            holder.verificationStatusImageView.visibility = View.VISIBLE
        } else {
            holder.verificationStatusImageView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onDoctorClickListener.onDoctorClick(doctor) }
    }

    override fun getItemCount(): Int = doctorList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvDoctorName)
        val specializationTextView: TextView = itemView.findViewById(R.id.tvSpecialization)
        val experienceTextView: TextView = itemView.findViewById(R.id.tvExperience)
        val tvEducation: TextView = itemView.findViewById(R.id.tveducation)
        val verificationStatusImageView: ImageView = itemView.findViewById(R.id.imgVerified)
        val imageView: CircleImageView = itemView.findViewById(R.id.imgDoctor)
    }

    interface OnDoctorClickListener {
        fun onDoctorClick(doctor: DoctorSummary?)
    }
}