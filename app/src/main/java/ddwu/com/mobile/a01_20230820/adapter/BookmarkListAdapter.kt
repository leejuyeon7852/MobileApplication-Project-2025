package ddwu.com.mobile.a01_20230820.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ddwu.com.mobile.a01_20230820.data.bookmark.Bookmark
import ddwu.com.mobile.a01_20230820.databinding.BookmarkItemsBinding

class BookmarkListAdapter
    : RecyclerView.Adapter<BookmarkListAdapter.BookmarkViewHolder>() {

    private val bookmarkList = mutableListOf<Bookmark>()

    interface OnItemClickListener {
        fun onItemClick(bookmark: Bookmark)
        fun onItemLongClick(bookmark: Bookmark)
    }

    var listener: OnItemClickListener? = null

    fun setList(list: List<Bookmark>) {
        bookmarkList.clear()
        bookmarkList.addAll(list)
        notifyDataSetChanged()
    }

    fun removeItem(bookmark: Bookmark) {
        val idx = bookmarkList.indexOf(bookmark)
        if (idx != -1) {
            bookmarkList.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = BookmarkItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarkList[position]

        holder.binding.tvBookmarkName.text = bookmark.placeName
        holder.binding.tvBookmarkAddress.text = bookmark.address

        holder.itemView.setOnClickListener {
            listener?.onItemClick(bookmark)
        }

        holder.itemView.setOnLongClickListener {
            listener?.onItemLongClick(bookmark)
            true
        }
    }

    override fun getItemCount(): Int = bookmarkList.size

    class BookmarkViewHolder(
        val binding: BookmarkItemsBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
