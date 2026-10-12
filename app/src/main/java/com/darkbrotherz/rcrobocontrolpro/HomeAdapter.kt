package com.darkbrotherz.rcrobocontrolpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HomeFeedItem(
    val uploaderName: String,
    val description: String,
    val timeAgo: String,
    val codeSnippet: String
)

class HomeAdapter(private val feedList: List<HomeFeedItem>) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    class HomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUploaderName: TextView = view.findViewById(R.id.txtUploaderName)
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        val txtTimeAgo: TextView = view.findViewById(R.id.txtTimeAgo)
        val txtCodePreview: TextView = view.findViewById(R.id.txtCodePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_feed, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = feedList[position]
        holder.txtUploaderName.text = item.uploaderName
        holder.txtDescription.text = item.description
        holder.txtTimeAgo.text = item.timeAgo
        holder.txtCodePreview.text = item.codeSnippet
    }

    override fun getItemCount(): Int = feedList.size
}
