package org.ole.planet.myplanet.ui.mylife.helper

import androidx.recyclerview.widget.RecyclerView

interface ItemTouchHelperViewHolder {
    fun onItemSelected()
    fun onItemClear(viewHolder: RecyclerView.ViewHolder?)
}
