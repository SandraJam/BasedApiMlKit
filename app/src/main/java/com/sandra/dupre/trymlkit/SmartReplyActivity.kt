package com.sandra.dupre.trymlkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import kotlinx.android.synthetic.main.activity_smart_reply.*
import kotlinx.android.synthetic.main.item_chat.view.*
import java.util.*

class SmartReplyActivity : AppCompatActivity() {

    private lateinit var myPseudo : String
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_reply)

        chatRecyclerView.layoutManager = LinearLayoutManager(baseContext).apply {
            reverseLayout = true
        }
        chatRecyclerView.adapter =  SmartAdapter()

        val db = FirebaseFirestore.getInstance().collection("messages")

        myPseudo = intent.getStringExtra("pseudo")

        listener =  db.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
            if (e != null) return@EventListener

            fillChat(value?.documents?.map {
                Message(
                    message = it.getString("message") ?: "",
                    user = it.getString("user") ?: "",
                    time = it.getDate("time") ?: Date(),
                    isSmartReply = it.getBoolean("smartreply") ?: false,
                    isme = it.getString("user") ?: "" == myPseudo
                )
            } ?: emptyList())
        })

        sendButton.setOnClickListener {
            db
                .document()
                .set(
                    mapOf(
                        "message" to messageEditText.text.toString(),
                        "user" to myPseudo,
                        "time" to Date(),
                        "smartreply" to false
                    )
                )
                .addOnSuccessListener { messageEditText.text?.clear() }
        }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    private fun fillChat(messages: List<Message>) {
        (chatRecyclerView.adapter as? SmartAdapter)?.replace(messages)
    }
}

data class Message(
    val message: String,
    val user: String,
    val time: Date,
    val isSmartReply: Boolean,
    val isme: Boolean
)

class SmartAdapter: RecyclerView.Adapter<SmartViewHolder>() {
    private val list = mutableListOf<Message>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder = SmartViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
    )

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        holder.bind(list[position])
    }

    fun replace(messages: List<Message>) {
        val diffResult = DiffUtil.calculateDiff(SmartDiffUtilCallback(list, messages))
        list.clear()
        list.addAll(messages)
        diffResult.dispatchUpdatesTo(this)
    }
}

class SmartViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    fun bind (message: Message) {
        itemView.pseudoTextView.text = message.user
        itemView.messageTextView.text = message.message
        itemView.smartReplyImageView.isVisible = message.isSmartReply

        itemView.cardView.setBackgroundColor(ContextCompat.getColor(
            itemView.context,
            if(message.isme)  R.color.pink_one else R.color.green_one
        ))
    }
}

class SmartDiffUtilCallback(
    private val old: List<Message>,
    private val new: List<Message>
): DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].message == new[newItemPosition].message &&
                old[oldItemPosition].user == new[newItemPosition].user

    override fun getOldListSize(): Int  = old.size

    override fun getNewListSize(): Int = new.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].message == new[newItemPosition].message &&
            old[oldItemPosition].user == new[newItemPosition].user
}