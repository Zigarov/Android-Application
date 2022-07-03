package com.example.macc_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter (private var map: Map<String, Int>, private var images: List<Int>, private var places : List<Int>, private val users: List<User>):
RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(){

inner class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
    val itemName : TextView = itemView.findViewById(R.id.tv_name)
    val itemScore : TextView = itemView.findViewById(R.id.tv_score)
    val itemAvatar: ImageView = itemView.findViewById(R.id.tv_image)

    val itemPlace : ImageView = itemView.findViewById(R.id.imageView2)


}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item,parent,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemName.text = users[position].fullname
        holder.itemScore.text = users[position].score.toString()
        if (images.isNotEmpty()) {
            holder.itemAvatar.setImageResource(images[position])
        }
        if (places.isNotEmpty()) {
            holder.itemPlace.setImageResource(places[position])
        }
        if (position>2) {
            holder.itemPlace.isVisible = false
        }

        holder.itemView.setOnClickListener {
            val name = users[position].fullname
            val email = users[position].email
            val score = users[position].score
            val followers = users[position].nFollowers
            val following = users[position].nFollowing
            val navController = Navigation.findNavController(holder.itemView)
            navController.navigate(SocialFragmentDirections.nextAction(name,email,score,followers, following))
        }

    }

    override fun getItemCount(): Int {
        return map.size
    }

}