package com.darkbrotherz.rcrobocontrolpro

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CommunityFragment : Fragment() {

    private lateinit var sectionDiscussion: LinearLayout
    private lateinit var sectionFeedback: LinearLayout
    private lateinit var btnToggleDiscussion: TextView
    private lateinit var btnToggleFeedback: TextView
    
    private val discussionList = mutableListOf<DiscussionItem>()
    private lateinit var discussionAdapter: DiscussionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)

        sectionDiscussion = view.findViewById(R.id.sectionDiscussion)
        sectionFeedback = view.findViewById(R.id.sectionFeedback)
        btnToggleDiscussion = view.findViewById(R.id.btnToggleDiscussion)
        btnToggleFeedback = view.findViewById(R.id.btnToggleFeedback)

        val rvDiscussion = view.findViewById<RecyclerView>(R.id.rvDiscussion)
        val rvFeedback = view.findViewById<RecyclerView>(R.id.rvFeedback)
        val etQuestion = view.findViewById<EditText>(R.id.etQuestion)
        val btnAsk = view.findViewById<Button>(R.id.btnAsk)

        // Setup Discussion Adapter
        discussionList.add(DiscussionItem("Pinned", "Welcome to Arduino Academy Community!", "Welcome everyone! This is a place to share projects...", "by Moderator", "1 day ago"))
        discussionList.add(DiscussionItem("Solved", "Arduino Uno not responding - Need help!", "My Arduino Uno suddenly stopped working...", "by User", "6 hours ago"))
        discussionAdapter = DiscussionAdapter(discussionList)
        rvDiscussion.layoutManager = LinearLayoutManager(requireContext())
        rvDiscussion.adapter = discussionAdapter

        // Setup Feedback Adapter (Static for now)
        val feedbackList = listOf(
            DiscussionItem("Feature", "Add dark theme to code editor", "The code playground should have a dark theme option...", "by Dev Team", "1 day ago")
        )
        rvFeedback.layoutManager = LinearLayoutManager(requireContext())
        rvFeedback.adapter = DiscussionAdapter(feedbackList)

        // Toggle Listeners
        btnToggleDiscussion.setOnClickListener { showSection(true) }
        btnToggleFeedback.setOnClickListener { showSection(false) }

        // Ask Button Listener
        btnAsk.setOnClickListener {
            val question = etQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                discussionList.add(1, DiscussionItem("New", question, "Details coming soon...", "by Me", "Just now"))
                discussionAdapter.notifyItemInserted(1)
                etQuestion.setText("")
                Toast.makeText(requireContext(), "Question posted!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showSection(isDiscussion: Boolean) {
        sectionDiscussion.visibility = if (isDiscussion) View.VISIBLE else View.GONE
        sectionFeedback.visibility = if (isDiscussion) View.GONE else View.VISIBLE

        val activeColor = Color.WHITE
        val inactiveColor = Color.parseColor("#80FFFFFF")
        
        btnToggleDiscussion.setTextColor(if (isDiscussion) activeColor else inactiveColor)
        btnToggleDiscussion.setBackgroundResource(if (isDiscussion) R.drawable.bg_category_pill else 0)
        
        btnToggleFeedback.setTextColor(if (isDiscussion) inactiveColor else activeColor)
        btnToggleFeedback.setBackgroundResource(if (isDiscussion) 0 else R.drawable.bg_category_pill)
    }
}

data class DiscussionItem(val tag: String, val title: String, val content: String, val user: String, val time: String)

class DiscussionAdapter(private val list: List<DiscussionItem>) : RecyclerView.Adapter<DiscussionAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tag: TextView = v.findViewById(R.id.txtDiscussionTag)
        val title: TextView = v.findViewById(R.id.txtDiscussionTitle)
        val content: TextView = v.findViewById(R.id.txtDiscussionContent)
        val user: TextView = v.findViewById(R.id.txtDiscussionUser)
        val time: TextView = v.findViewById(R.id.txtDiscussionTime)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_discussion, p, false))
    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val item = list[pos]
        h.tag.text = item.tag
        h.title.text = item.title
        h.content.text = item.content
        h.user.text = item.user
        h.time.text = item.time
    }
    override fun getItemCount() = list.size
}
