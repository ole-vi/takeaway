package org.ole.planet.myplanet.ui.news

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import io.realm.Case
import io.realm.Realm
import io.realm.RealmList
import io.realm.Sort
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.RowNewsBinding
import org.ole.planet.myplanet.model.Conversation
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmNews
import org.ole.planet.myplanet.model.RealmNews.Companion.createNews
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.ui.chat.ChatAdapter
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.Constants.showBetaFeature
import org.ole.planet.myplanet.utilities.JsonUtils.getString
import org.ole.planet.myplanet.utilities.SharedPrefManager
import org.ole.planet.myplanet.utilities.TimeUtils.formatDate
import org.ole.planet.myplanet.utilities.Utilities
import java.io.File
import java.util.Calendar

class AdapterNews(var context: Context, private val list: MutableList<RealmNews?>, private var currentUser: RealmUserModel?, private val parentNews: RealmNews?) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private lateinit var rowNewsBinding: RowNewsBinding
    private var listener: OnNewsItemClickListener? = null
    private val config: ChipCloudConfig = Utilities.getCloudConfig().selectMode(ChipCloud.SelectMode.close)
    private var imageList: RealmList<String>? = null
    lateinit var mRealm: Realm
    private var fromLogin = false
    private var sharedPreferences: SharedPrefManager? = null
    fun setImageList(imageList: RealmList<String>?) {
        this.imageList = imageList
    }

    fun addItem(news: RealmNews?) {
        list.add(news)
        notifyDataSetChanged()
    }

    fun setFromLogin(fromLogin: Boolean) {
        this.fromLogin = fromLogin
    }

    fun setListener(listener: OnNewsItemClickListener?) {
        this.listener = listener
    }

    fun setmRealm(mRealm: Realm?) {
        if (mRealm != null) {
            this.mRealm = mRealm
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        rowNewsBinding = RowNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        sharedPreferences = SharedPrefManager(context)
        return ViewHolderNews(rowNewsBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderNews) {
            holder.bind(position)
            val news = getNews(holder, position)

            if (news?.isValid == true) {
                val userModel = mRealm.where(RealmUserModel::class.java).equalTo("id", news.userId).findFirst()
                if (userModel != null && currentUser != null) {
                    holder.rowNewsBinding.tvName.text = userModel.toString()
                    Utilities.loadImage(userModel.userImage, holder.rowNewsBinding.imgUser)
                    showHideButtons(userModel, holder)
                } else {
                    holder.rowNewsBinding.tvName.text = news.userName
                    holder.rowNewsBinding.llEditDelete.visibility = View.GONE
                }
                showShareButton(holder, news)
                if ("${news.messageWithoutMarkdown}" != "</br>") {
                    holder.rowNewsBinding.tvMessage.text = news.messageWithoutMarkdown
                } else {
                    holder.rowNewsBinding.linearLayout51.visibility = View.GONE
                }
                holder.rowNewsBinding.tvDate.text = formatDate(news.time)
                if (news.userId == currentUser?._id) {
                    holder.rowNewsBinding.imgDelete.setOnClickListener {
                        AlertDialog.Builder(context)
                            .setMessage(R.string.delete_record)
                            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                                deletePost(news, context)
                            }.setNegativeButton(R.string.cancel, null).show()
                    }
                    holder.rowNewsBinding.imgEdit.setOnClickListener {
                        showEditAlert(news.id, true)
                    }
                    holder.rowNewsBinding.btnAddLabel.visibility = if (fromLogin) View.GONE else View.VISIBLE
                } else {
                    holder.rowNewsBinding.imgEdit.visibility = View.GONE
                    holder.rowNewsBinding.imgDelete.visibility = View.GONE
                    holder.rowNewsBinding.btnAddLabel.visibility = View.GONE
                }
                holder.rowNewsBinding.llEditDelete.visibility = if (fromLogin) View.GONE else View.VISIBLE
                holder.rowNewsBinding.btnReply.visibility = if (fromLogin) View.GONE else View.VISIBLE
                loadImage(holder, news)
                showReplyButton(holder, news, position)
                if (news.isCommunityNews) {
                    holder.itemView.setOnClickListener {
                        context.startActivity(Intent(context, NewsDetailActivity::class.java).putExtra("newsId", list[position]?.id))
                    }
                }
                addLabels(holder, news)
                showChips(holder, news)

                if (news.newsId?.isNotEmpty() == true) {
                    val conversations = Gson().fromJson(news.conversations, Array<Conversation>::class.java).toList()
                    val chatAdapter = ChatAdapter(ArrayList(), context, holder.rowNewsBinding.recyclerGchat)
                    for (conversation in conversations) {
                        val query = conversation.query
                        val response = conversation.response
                        if (query != null) {
                            chatAdapter.addQuery(query)
                        }
                        chatAdapter.responseSource = ChatAdapter.RESPONSE_SOURCE_SHARED_VIEW_MODEL
                        if (response != null) {
                            chatAdapter.addResponse(response)
                        }
                    }

                    holder.rowNewsBinding.recyclerGchat.adapter = chatAdapter
                    holder.rowNewsBinding.recyclerGchat.layoutManager = LinearLayoutManager(context)
                } else {
                    holder.rowNewsBinding.recyclerGchat.visibility = View.GONE
                    holder.rowNewsBinding.sharedChat.visibility = View.GONE
                }
            }
        }
    }

    private fun addLabels(holder: RecyclerView.ViewHolder, news: RealmNews?) {
        val viewHolder = holder as ViewHolderNews
        viewHolder.rowNewsBinding.btnAddLabel.setOnClickListener {
            val menu = PopupMenu(context, viewHolder.rowNewsBinding.btnAddLabel)
            val inflater = menu.menuInflater
            inflater.inflate(R.menu.menu_add_label, menu.menu)
            menu.setOnMenuItemClickListener { menuItem: MenuItem ->
                if (!mRealm.isInTransaction) {
                    mRealm.beginTransaction()
                }
                news?.addLabel(Constants.LABELS["${menuItem.title}"])
                Utilities.toast(context, R.string.label_added.toString())
                mRealm.commitTransaction()
                news?.let { it1 -> showChips(holder, it1) }
                false
            }
            menu.show()
        }
    }

    private fun showChips(holder: RecyclerView.ViewHolder, news: RealmNews) {
        val viewHolder = holder as ViewHolderNews
        viewHolder.rowNewsBinding.fbChips.removeAllViews()
        val chipCloud = ChipCloud(context, viewHolder.rowNewsBinding.fbChips, config)
        for (s in news.labels ?: emptyList()) {
            chipCloud.addChip(getLabel(s))
            chipCloud.setDeleteListener { _: Int, s1: String? ->
                if (!mRealm.isInTransaction) {
                    mRealm.beginTransaction()
                }
                news.labels?.remove(Constants.LABELS[s1])
                mRealm.commitTransaction()
                viewHolder.rowNewsBinding.btnAddLabel.isEnabled = (news.labels?.size ?: 0) < 3
            }
        }
        viewHolder.rowNewsBinding.btnAddLabel.isEnabled = (news.labels?.size ?: 0) < 3
    }

    private fun loadImage(holder: RecyclerView.ViewHolder, news: RealmNews?) {
        val viewHolder = holder as ViewHolderNews
        val imageUrls = news?.imageUrls
        if (imageUrls != null && imageUrls.size > 0) {
            try {
                val imgObject = Gson().fromJson(imageUrls[0], JsonObject::class.java)
                viewHolder.rowNewsBinding.imgNews.visibility = View.VISIBLE
                Glide.with(context).load(File(getString("imageUrl", imgObject)))
                    .into(viewHolder.rowNewsBinding.imgNews)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            loadRemoteImage(holder, news)
        }
    }

    private fun loadRemoteImage(holder: RecyclerView.ViewHolder, news: RealmNews?) {
        val viewHolder = holder as ViewHolderNews
        news?.imagesArray?.let { imagesArray ->
            if (imagesArray.size() > 0) {
                val ob = imagesArray[0]?.asJsonObject
                getString("resourceId", ob).let { resourceId ->
                    mRealm.where(RealmMyLibrary::class.java).equalTo("_id", resourceId).findFirst()?.let { library ->
                        context.getExternalFilesDir(null)?.let { basePath ->
                            val imageFile = File(basePath, "ole/${library.id}/${library.resourceLocalAddress}")
                            if (imageFile.exists()) {
                                Glide.with(context)
                                    .load(imageFile)
                                    .into(viewHolder.rowNewsBinding.imgNews)
                                viewHolder.rowNewsBinding.imgNews.visibility = View.VISIBLE
                                return
                            }
                        }
                    }
                }
            }
        }
        viewHolder.rowNewsBinding.imgNews.visibility = View.GONE
    }

    private fun showReplyButton(holder: RecyclerView.ViewHolder, finalNews: RealmNews?, position: Int) {
        val viewHolder = holder as ViewHolderNews
        if (listener == null || fromLogin) {
            viewHolder.rowNewsBinding.btnShowReply.visibility = View.GONE
        }
        viewHolder.rowNewsBinding.btnReply.setOnClickListener { showEditAlert(finalNews?.id, false) }
        val replies: List<RealmNews> = mRealm.where(RealmNews::class.java).sort("time", Sort.DESCENDING).equalTo("replyTo", finalNews?.id, Case.INSENSITIVE).findAll()
        viewHolder.rowNewsBinding.btnShowReply.text = String.format(context.getString(R.string.show_replies) + " (%d)", replies.size)
        viewHolder.rowNewsBinding.btnShowReply.visibility = if (replies.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (position == 0 && parentNews != null) {
            viewHolder.rowNewsBinding.btnShowReply.visibility = View.GONE
        }
        viewHolder.rowNewsBinding.btnShowReply.setOnClickListener {
            sharedPreferences?.setREPLIEDNEWSID(finalNews?.id)
            listener?.showReply(finalNews, fromLogin)
        }
    }

    private fun showEditAlert(id: String?, isEdit: Boolean) {
        val v = LayoutInflater.from(context).inflate(R.layout.alert_input, null)
        val et = v.findViewById<EditText>(R.id.et_input)
        v.findViewById<View>(R.id.ll_image).visibility = if (showBetaFeature(Constants.KEY_NEWSADDIMAGE, context)) View.VISIBLE else View.GONE
        val llImage = v.findViewById<LinearLayout>(R.id.ll_alert_image)
        v.findViewById<View>(R.id.add_news_image).setOnClickListener { listener?.addImage(llImage) }
        val news = mRealm.where(RealmNews::class.java).equalTo("id", id).findFirst()
        if (isEdit) et.setText("${news?.message}")
        AlertDialog.Builder(context).setTitle(if (isEdit) R.string.edit_post else R.string.reply)
            .setIcon(R.drawable.ic_edit).setView(v)
            .setPositiveButton(R.string.button_submit) { _: DialogInterface?, _: Int ->
                val s = et.text.toString()
                if (isEdit) {
                    editPost(s, news)
                } else {
                    postReply(s, news)
                }
            }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun postReply(s: String?, news: RealmNews?) {
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        val map = HashMap<String?, String>()
        map["message"] = s ?: ""
        map["viewableBy"] = news?.viewableBy ?: ""
        map["viewableId"] = news?.viewableId ?: ""
        map["replyTo"] = news?.id ?: ""
        map["messageType"] = news?.messageType ?: ""
        map["messagePlanetCode"] = news?.messagePlanetCode ?: ""

        currentUser?.let { createNews(map, mRealm, it, imageList) }
        notifyDataSetChanged()
    }

    private fun editPost(s: String, news: RealmNews?) {
        if (s.isEmpty()) {
            Utilities.toast(context, R.string.please_enter_message.toString())
            return
        }
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        news?.message = s
        mRealm.commitTransaction()
        notifyDataSetChanged()
    }

    private fun getNews(holder: RecyclerView.ViewHolder, position: Int): RealmNews? {
        val news: RealmNews? = if (parentNews != null) {
            if (position == 0) {
                (holder.itemView as CardView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_blue_50))
                parentNews
            } else {
                (holder.itemView as CardView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_white_1000))
                list[position - 1]
            }
        } else {
            (holder.itemView as CardView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_white_1000))
            list[position]
        }
        return news
    }

    private fun showHideButtons(userModel: RealmUserModel, holder: RecyclerView.ViewHolder) {
        val viewHolder = holder as ViewHolderNews
        if (currentUser?.id == userModel.id) {
            viewHolder.rowNewsBinding.llEditDelete.visibility = View.VISIBLE
            viewHolder.rowNewsBinding.btnAddLabel.visibility = View.VISIBLE
        } else {
            viewHolder.rowNewsBinding.llEditDelete.visibility = View.GONE
            viewHolder.rowNewsBinding.btnAddLabel.visibility = View.GONE
        }
    }

    private fun deletePost(news: RealmNews?, context: Context) {
        if (!mRealm.isInTransaction) mRealm.beginTransaction()
        val position = list.indexOf(news)
        if (position != -1) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
        news?.let {
            it.deleteFromRealm()
            if (context is ReplyActivity) {
                val restartIntent = context.intent
                context.finish()
                context.overridePendingTransition(0, 0)
                context.startActivity(restartIntent)
                context.overridePendingTransition(0, 0)
            }
        }
        mRealm.commitTransaction()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (parentNews == null) list.size else list.size + 1
    }

    interface OnNewsItemClickListener {
        fun showReply(news: RealmNews?, fromLogin: Boolean)
        fun addImage(llImage: LinearLayout?)
    }

    private fun getLabel(s: String): String {
        for (key in Constants.LABELS.keys) {
            if (s == Constants.LABELS[key]) {
                return key
            }
        }
        return ""
    }

    private fun showShareButton(holder: RecyclerView.ViewHolder, news: RealmNews?) {
        val viewHolder = holder as ViewHolderNews
        viewHolder.rowNewsBinding.btnShare.visibility = if (news?.isCommunityNews == true || fromLogin) View.GONE else View.VISIBLE
        viewHolder.rowNewsBinding.btnShare.setOnClickListener {
            val array = Gson().fromJson(news?.viewIn, JsonArray::class.java)
            val ob = JsonObject()
            ob.addProperty("section", "community")
            ob.addProperty("_id", currentUser?.planetCode + "@" + currentUser?.parentCode)
            ob.addProperty("sharedDate", Calendar.getInstance().timeInMillis)
            array.add(ob)
            if (!mRealm.isInTransaction) {
                mRealm.beginTransaction()
            }
            news?.viewIn = Gson().toJson(array)
            mRealm.commitTransaction()
            Utilities.toast(context, context.getString(R.string.shared_to_community))
            viewHolder.rowNewsBinding.btnShare.visibility = View.GONE
        }
    }

    internal inner class ViewHolderNews(val rowNewsBinding: RowNewsBinding) : RecyclerView.ViewHolder(rowNewsBinding.root) {
        private var adapterPosition = 0
        fun bind(position: Int) {
            adapterPosition = position
        }
    }
}
