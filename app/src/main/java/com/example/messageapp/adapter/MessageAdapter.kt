package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.messenger.R
import com.example.messageapp.model.Message

class MessageAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<Message>()

    fun submitList(list: List<Message>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 1)
            R.layout.item_message_sent
        else
            R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        fun bind(message: Message) {
            messageText.text = message.content
        }
    }
}
