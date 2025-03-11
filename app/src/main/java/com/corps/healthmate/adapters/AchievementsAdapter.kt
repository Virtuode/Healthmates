package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.data.model.Achievement
import com.corps.healthmate.databinding.ItemAchievementBinding

class AchievementsAdapter : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {
    private var achievements = listOf<Achievement>()

    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }

    override fun getItemCount() = achievements.size

    inner class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.apply {
                achievementIcon.setImageResource(achievement.iconResId)
                achievementTitle.text = achievement.title
                achievementDescription.text = achievement.description
                achievementProgress.max = achievement.maxProgress
                achievementProgress.progress = achievement.progress.toInt()
                achievementPoints.text = "${achievement.points}pts"

                root.alpha = if (achievement.isUnlocked) 1.0f else 0.5f
            }
        }
    }
}
