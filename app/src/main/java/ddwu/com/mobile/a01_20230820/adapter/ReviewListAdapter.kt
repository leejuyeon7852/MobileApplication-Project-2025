package ddwu.com.mobile.a01_20230820.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ddwu.com.mobile.a01_20230820.data.review.Review
import java.io.File
import ddwu.com.mobile.a01_20230820.databinding.ReviewItemsBinding

class ReviewListAdapter
    : RecyclerView.Adapter<ReviewListAdapter.ReviewViewHolder>() {

    private var reviewList: MutableList<Review> = mutableListOf()

    interface OnItemClickListener {
        fun onItemClick(review: Review)
        fun onItemLongClick(review: Review)
    }

    var listener: OnItemClickListener? = null

    fun setList(list: List<Review>) {
        reviewList = list.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(review: Review) {
        val idx = reviewList.indexOf(review)
        if (idx != -1) {
            reviewList.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]

        holder.binding.tvRestaurant.text = review.placeName
        holder.binding.tvRAddress.text = review.address

        if (review.imagePath != null) {
            Glide.with(holder.itemView.context)
                .load(File(review.imagePath))
                .into(holder.binding.ivPhoto)
        } else {
            holder.binding.ivPhoto.setImageResource(
                android.R.drawable.ic_menu_camera
            )
        }

        holder.itemView.setOnClickListener {
            listener?.onItemClick(review)
        }

        holder.itemView.setOnLongClickListener {
            listener?.onItemLongClick(review)
            true
        }
    }

    override fun getItemCount(): Int = reviewList.size

    class ReviewViewHolder(
        val binding: ReviewItemsBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
