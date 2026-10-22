package com.minimal.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Item(
    val label: String,
    val pkg: String?,
    val intent: Intent,
    val art: Drawable?,
    val isBanner: Boolean
)

class MainActivity : Activity() {

    private lateinit var list: RecyclerView
    private val prefs by lazy { getSharedPreferences("home", Context.MODE_PRIVATE) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            clipChildren = false
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Config.bgTop, Config.bgBottom)
            )
        }

        val clockFormat = if (Config.use24h) "HH:mm" else "h:mm"
        val clockBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(64), dp(48), 0, 0)
            }
        }
        val clock = TextClock(this).apply {
            format12Hour = clockFormat
            format24Hour = clockFormat
            textSize = 72f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"
            format24Hour = "EEEE, d MMMM"
            textSize = 18f
            setTextColor(0x99FFFFFF.toInt())
        }
        clockBox.addView(clock)
        clockBox.addView(date)
        root.addView(clockBox)

        list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            clipChildren = false
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(56), dp(28), dp(56), dp(28))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                bottomMargin = dp(64)
            }
        }
        root.addView(list)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        load()
        focusFirst()
    }

    @Deprecated("Home screen: Back does nothing")
    override fun onBackPressed() {
        // stay on the home screen
    }

    private fun focusFirst() {
        list.post {
            list.scrollToPosition(0)
            list.post { list.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
        }
    }

    private fun load() {
        val pm = packageManager
        val hiddenByUser = prefs.getStringSet("hidden", emptySet()) ?: emptySet()
        val seen = HashSet<String>()
        val found = ArrayList<Item>()

        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == packageName || pkg in Config.hidden || pkg in hiddenByUser) continue
                if (!seen.add(pkg)) continue
                val banner = ai.loadBanner(pm)
                val art = banner ?: ai.loadIcon(pm)
                val launch = Intent(Intent.ACTION_MAIN)
                    .setClassName(pkg, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                found.add(Item(ri.loadLabel(pm).toString(), pkg, launch, art, banner != null))
            }
        }

        val rank = Config.pinned.withIndex().associate { it.value to it.index }
        val sorted = found.sortedWith(
            compareBy<Item>({ rank[it.pkg] ?: Int.MAX_VALUE }, { it.label.lowercase() })
        ).toMutableList()

        sorted.add(
            Item(
                "Settings", null,
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null, false
            )
        )
        list.adapter = TileAdapter(sorted)
    }

    private fun hide(pkg: String) {
        val set = HashSet(prefs.getStringSet("hidden", emptySet()) ?: emptySet())
        set.add(pkg)
        prefs.edit().putStringSet("hidden", set).apply()
        Toast.makeText(this, "Hidden. Long-press Settings to restore all.", Toast.LENGTH_LONG).show()
        load()
        focusFirst()
    }

    private fun restoreHidden() {
        prefs.edit().remove("hidden").apply()
        Toast.makeText(this, "Hidden apps restored", Toast.LENGTH_SHORT).show()
        load()
        focusFirst()
    }

    private fun cardFill() = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Config.cardColor)
    }

    private fun cardStroke() = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.TRANSPARENT)
        setStroke(dp(3), Config.accent)
    }

    inner class VH(
        val root: LinearLayout,
        val card: FrameLayout,
        val img: ImageView,
        val cardText: TextView,
        val label: TextView
    ) : RecyclerView.ViewHolder(root)

    inner class TileAdapter(private val items: List<Item>) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isFocusable = true
                isClickable = true
                layoutParams = RecyclerView.LayoutParams(
                    dp(220), ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(8), 0, dp(8), 0) }
            }
            val card = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(220), dp(124))
                background = cardFill()
                clipToOutline = true
            }
            val img = ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val cardText = TextView(ctx).apply {
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            card.addView(img)
            card.addView(cardText)
            val label = TextView(ctx).apply {
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                alpha = 0f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
            }
            root.addView(card)
            root.addView(label)

            val vh = VH(root, card, img, cardText, label)
            root.setOnFocusChangeListener { _, focused -> applyFocus(vh, focused) }
            return vh
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.label.text = item.label
            holder.img.setImageDrawable(item.art)
            val pad = if (item.isBanner) 0 else dp(16)
            holder.img.setPadding(pad, pad, pad, pad)
            holder.img.visibility = if (item.art == null) View.GONE else View.VISIBLE
            holder.cardText.text = item.label
            holder.cardText.visibility = if (item.art == null) View.VISIBLE else View.GONE

            holder.root.setOnClickListener {
                try {
                    startActivity(item.intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Can't open ${item.label}", Toast.LENGTH_SHORT).show()
                }
            }
            holder.root.setOnLongClickListener {
                if (item.pkg == null) restoreHidden() else hide(item.pkg)
                true
            }
            applyFocus(holder, holder.root.isFocused, animate = false)
        }

        override fun getItemCount() = items.size

        private fun applyFocus(vh: VH, focused: Boolean, animate: Boolean = true) {
            val scale = if (focused) 1.1f else 1f
            val alpha = if (focused) 1f else 0f
            vh.card.foreground = if (focused) cardStroke() else null
            if (animate) {
                vh.root.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
                vh.label.animate().alpha(alpha).setDuration(150).start()
            } else {
                vh.root.scaleX = scale
                vh.root.scaleY = scale
                vh.label.alpha = alpha
            }
        }
    }
}
