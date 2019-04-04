package com.sandra.dupre.trymlkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult.*
import kotlinx.android.synthetic.main.activity_smart_reply.*
import kotlinx.android.synthetic.main.item_chat.view.*
import kotlinx.android.synthetic.main.item_suggestion.view.*
import java.util.*

class SmartReplyActivity : AppCompatActivity() {

    private lateinit var myPseudo: String
    private var listener: ListenerRegistration? = null
    private val smartReply = FirebaseNaturalLanguage.getInstance().smartReply

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_reply)

        myPseudo = intent.getStringExtra("pseudo")
        val collectionRef = FirebaseFirestore.getInstance().collection("messages")

        chatRecyclerView.layoutManager = LinearLayoutManager(baseContext).apply { reverseLayout = true }
        chatRecyclerView.adapter = SmartAdapter()
        suggestionRecyclerView.layoutManager = LinearLayoutManager(baseContext, LinearLayoutManager.HORIZONTAL, false)
        suggestionRecyclerView.adapter = SuggestionAdapter { collectionRef.sendMessage(it, true)}

        listener = collectionRef.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
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

        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                collectionRef.sendMessage(messageEditText.text.toString(), false)
            }
            true
        }

        sendButton.setOnClickListener { collectionRef.sendMessage(messageEditText.text.toString(), false) }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    private fun fillChat(messages: List<Message>) {
        val conversation = messages.sortedBy { it.time.time }.map {
            if (it.user == myPseudo)
                FirebaseTextMessage.createForLocalUser(it.message, it.time.time)
            else
                FirebaseTextMessage.createForRemoteUser(it.message, it.time.time, it.user)
        }
        if(conversation.isNotEmpty()) {
            smartReply.suggestReplies(conversation).addOnSuccessListener { result ->
                when (result.status) {
                    STATUS_NOT_SUPPORTED_LANGUAGE -> {
                    }
                    STATUS_SUCCESS -> (suggestionRecyclerView.adapter as? SuggestionAdapter)?.replace(result.suggestions)
                }
            }
        }

        (chatRecyclerView.adapter as? SmartAdapter)?.replace(messages.sortedByDescending { it.time.time })
        chatRecyclerView.smoothScrollToPosition(0)
    }

    private fun CollectionReference.sendMessage(message: String, isSmartReply: Boolean) {
        document().set(
            mapOf(
                "message" to message,
                "user" to myPseudo,
                "time" to Date(),
                "smartreply" to isSmartReply
            )
        ).addOnSuccessListener { messageEditText.text?.clear() }
    }
}

data class Message(
    val message: String,
    val user: String,
    val time: Date,
    val isSmartReply: Boolean,
    val isme: Boolean
)

class SmartAdapter : RecyclerView.Adapter<SmartViewHolder>() {
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

class SmartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(message: Message) {
        itemView.pseudoTextView.text = message.user
        itemView.messageTextView.text = message.message
        itemView.smartReplyImageView.isVisible = message.isSmartReply

        itemView.cardView.setBackgroundColor(
            ContextCompat.getColor(
                itemView.context,
                if (message.user == "Aqua") R.color.blue_dark_one else R.color.red_dark_one
            )
        )
    }
}

class SmartDiffUtilCallback(
    private val old: List<Message>,
    private val new: List<Message>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].message == new[newItemPosition].message &&
                old[oldItemPosition].user == new[newItemPosition].user

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].message == new[newItemPosition].message &&
                old[oldItemPosition].user == new[newItemPosition].user
}

class SuggestionAdapter(private val onClickSuggestion: (String) -> Unit) :
    RecyclerView.Adapter<SuggestionViewHolder>() {
    private val suggestions = mutableListOf<SmartReplySuggestion>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder = SuggestionViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false),
        onClickSuggestion
    )

    override fun getItemCount(): Int = suggestions.size

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    fun replace(suggestions: MutableList<SmartReplySuggestion>) {
        this.suggestions.clear()
        this.suggestions.addAll(suggestions.sortedByDescending { it.confidence })
        notifyDataSetChanged()
    }
}

class SuggestionViewHolder(
    itemView: View,
    private val onClickSuggestion: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val colors =
        listOf(R.color.pink_one, R.color.orange_one, R.color.yellow_one, R.color.green_one, R.color.blue_one)

    fun bind(suggestion: SmartReplySuggestion) {
        itemView.setOnClickListener { onClickSuggestion(suggestion.text) }
        itemView.suggestion.text = suggestion.text
        itemView.suggestion.setBackgroundColor(ContextCompat.getColor(itemView.context, colors.random()))
    }
}