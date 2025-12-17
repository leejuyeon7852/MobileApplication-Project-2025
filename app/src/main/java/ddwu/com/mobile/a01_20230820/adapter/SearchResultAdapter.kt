package ddwu.com.mobile.a01_20230820.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.databinding.SearchResultItemsBinding

class SearchResultAdapter
    : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {
    private var places: List<KakaoPlace> = emptyList()

    override fun getItemCount(): Int = places.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchResultViewHolder {
        val binding = SearchResultItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SearchResultViewHolder,
        position: Int
    ) {
        val place = places[position]

        holder.binding.tvPlaceName.text = place.place_name
        holder.binding.tvAddress.text =
            if (place.road_address_name.isNotEmpty())
                place.road_address_name
            else
                place.address_name

        holder.binding.tvPhone.text = place.phone ?: "전화번호 없음"

        val dist = place.calcDistance

        holder.binding.tvDistance.text =
            when {
                dist < 0 -> "거리 정보 없음"
                dist < 1000 -> "${dist.toInt()} m"
                else -> String.format("%.2f km", dist / 1000)
            }

        holder.binding.clItem.setOnClickListener {
            clickListener?.onItemClick(position)
        }
    }

    class SearchResultViewHolder(
        val binding: SearchResultItemsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    fun setList(newList: List<KakaoPlace>) {
        places = newList
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    var clickListener: OnItemClickListener? = null
}
