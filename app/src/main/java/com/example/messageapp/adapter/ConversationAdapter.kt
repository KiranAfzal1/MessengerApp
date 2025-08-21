package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.messenger.R
import com.example.messageapp.model.Conversation

class ConversationAdapter(private val listener: (Conversation) -> Unit) :
    RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private val conversations = mutableListOf<Conversation>()

    fun submitList(list: List<Conversation>) {
        conversations.clear()
        conversations.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun getItemCount() = conversations.size

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.conversationName)
        private val emailText: TextView = itemView.findViewById(R.id.conversationExtra)
        fun bind(conversation: Conversation) {
            nameText.text = conversation.name
            emailText.text = conversation.extra
            itemView.setOnClickListener { listener(conversation) }
        }
    }
}
