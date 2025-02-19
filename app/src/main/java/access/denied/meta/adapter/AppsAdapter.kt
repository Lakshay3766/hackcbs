package access.denied.meta.adapter


import android.app.Application
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import access.denied.meta.R
import access.denied.meta.models.ApplicationState
import access.denied.meta.models.InstalledApplication
import access.denied.meta.repository.ApplicationRepository
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily


class AppsAdapter(ctx: Context) : ListAdapter<InstalledApplication, AppsAdapter.AppHolder>(
    DIFF_CALLBACK
) {
    private lateinit var listener: OnItemClickListener
    private lateinit var context: Context

    init {
        ApplicationRepository.getInstance(ctx as Application).getThirdPartyApps()
            .observeForever { apps ->
                submitList(apps)
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.listview_apps, parent, false)
        context = view.context
        return AppHolder(view)
    }

    override fun submitList(list: List<InstalledApplication>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    override fun onBindViewHolder(
        holder: AppsAdapter.AppHolder,
        position: Int
    ) {

        val currentApp: InstalledApplication = getItem(position)
        holder.image.background = BitmapDrawable.createFromStream(currentApp.icon.inputStream(), "")
        holder.imageName.text = currentApp.name

        val radius: Float = context.resources.getDimension(R.dimen.roundedCornerPrimary)
        holder.image.setImageIcon(Icon.createWithData(currentApp.icon, 0, currentApp.icon.size))
        holder.image.shapeAppearanceModel = holder.image.shapeAppearanceModel
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, radius)
            .build()

        when (currentApp.applicationState) {
            ApplicationState.DANGEROUS -> {
                holder.tvState.text = context.getString(R.string.warning)
                holder.tvState.setTextColor(context.resources.getColor(R.color.warningColor))
            }
            ApplicationState.NORMAL -> {
                holder.tvState.text = context.getString(R.string.normal)
                holder.tvState.setTextColor(context.resources.getColor(R.color.doneColor))
            }
            ApplicationState.MEDIUM -> {
                holder.tvState.text = context.getString(R.string.medium)
                holder.tvState.setTextColor(context.resources.getColor(R.color.warningColor))
            }
        }
    }

    inner class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ShapeableImageView
        var imageName: TextView
        var parentLayout: RelativeLayout
        var tvState: TextView

        init {
            image = itemView.findViewById(R.id.imageApp)
            imageName = itemView.findViewById(R.id.tvAppName)
            parentLayout = itemView.findViewById(R.id.rlCardViewListApps)
            tvState = itemView.findViewById(R.id.tvStatus)
            parentLayout.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position))
                }
            }
        }

    }

    interface OnItemClickListener {

        fun onItemClick(app: InstalledApplication)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<InstalledApplication> =
            object : DiffUtil.ItemCallback<InstalledApplication>() {
                override fun areItemsTheSame(
                    oldItem: InstalledApplication,
                    newItem: InstalledApplication
                ): Boolean {
                    return oldItem.packageName == newItem.packageName
                            && oldItem.name == newItem.name
                            && oldItem.isSystemApp == newItem.isSystemApp
                            && oldItem.versionCode == newItem.versionCode
                            && oldItem.applicationState == newItem.applicationState
                            && oldItem.uninstalled == newItem.uninstalled
                }

                override fun areContentsTheSame(
                    oldItem: InstalledApplication,
                    newItem: InstalledApplication
                ): Boolean {
                    return oldItem.name == newItem.name
                            && oldItem.isSystemApp == newItem.isSystemApp
                            && oldItem.versionCode == newItem.versionCode
                            && oldItem.applicationState == newItem.applicationState
                            && oldItem.uninstalled == newItem.uninstalled
                }
            }
    }
}