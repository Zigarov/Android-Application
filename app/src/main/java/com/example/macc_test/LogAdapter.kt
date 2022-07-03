package com.example.macc_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView

class LogAdapter (private val log : List<Run>, private var images: List<Int>): RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    inner class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val itemName : TextView = itemView.findViewById(R.id.tv_name)
        val itemScore : TextView = itemView.findViewById(R.id.tv_score)
        val itemAvatar: ImageView = itemView.findViewById(R.id.tv_image)
        val itemTimeStamp : TextView = itemView.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item_home,parent,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemName.text = log[position].user.fullname
        holder.itemScore.text = log[position].steps.toString()
        holder.itemTimeStamp.text = log[position].date.toDate().toString()
        if (images.isNotEmpty()) {
            holder.itemAvatar.setImageResource(images[position])
        }

        holder.itemView.setOnClickListener {
            val name = log[position].user.fullname
            val email = log[position].user.email
            val score = log[position].user.score
            val followers = log[position].user.nFollowers
            val following = log[position].user.nFollowing
            val navController = Navigation.findNavController(holder.itemView)
            navController.navigate(HomeFragmentDirections.nextAction(name,email,score,followers, following))
        }
    }

    override fun getItemCount(): Int {
        return log.size
    }

}