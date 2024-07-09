package org.ole.planet.myplanet.ui.community

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.RowJoinedUserBinding
import org.ole.planet.myplanet.model.RealmUserModel

class AdapterLeader(var context: Context, private var leaders: List<RealmUserModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var rowJoinedUserBinding: RowJoinedUserBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        rowJoinedUserBinding = RowJoinedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolderLeader(rowJoinedUserBinding)
    }

    override fun getItemCount(): Int {
        return leaders.size
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderLeader) {
            if (leaders[position].firstName == null) {
                holder.title.text = leaders[position].name
            } else {
                holder.title.text = context.getString(R.string.message_placeholder, leaders[position])
            }
            holder.tvDescription.text = leaders[position].email
        }
    }

    internal inner class ViewHolderLeader(rowJoinedUserBinding: RowJoinedUserBinding) : RecyclerView.ViewHolder(rowJoinedUserBinding.root){
        var title= rowJoinedUserBinding.tvTitle
        var tvDescription= rowJoinedUserBinding.tvDescription
        var icon= rowJoinedUserBinding.icMore
    }
}