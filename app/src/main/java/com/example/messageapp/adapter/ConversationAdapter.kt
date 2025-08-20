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
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.contact_name)
        private val lastMessageText: TextView = itemView.findViewById(R.id.last_message)

        fun bind(conversation: Conversation) {
            nameText.text = conversation.name
            lastMessageText.text = conversation.lastMessage
            itemView.setOnClickListener { listener(conversation) }
        }
    }
}
