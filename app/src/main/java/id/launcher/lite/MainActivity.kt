package id.launcher.lite

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

private class AppItem(val label: String, val icon: Drawable, val cn: ComponentName)

class MainActivity : Activity() {

    private val apps = ArrayList<AppItem>()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var adapter: AppAdapter
    private var loading = false

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = loadApps()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0F1218"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // Panel kiri: jam + shortcut besar
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"
            textSize = 56f; setTextColor(Color.WHITE)
        })
        left.addView(TextClock(this).apply {
            format24Hour = "EEEE, d MMM"; format12Hour = "EEEE, d MMM"
            textSize = 18f; setTextColor(Color.parseColor("#9AA4B2"))
            setPadding(0, 0, 0, dp(16))
        })
        left.addView(bigButton("Navigasi") {
            start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS))
        })
        left.addView(bigButton("Musik") {
            start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
        })
        left.addView(bigButton("Telepon") { start(Intent(Intent.ACTION_DIAL)) })
        left.addView(bigButton("Setelan") { start(Intent(Settings.ACTION_SETTINGS)) })
        root.addView(left, LinearLayout.LayoutParams(dp(260), ViewGroup.LayoutParams.MATCH_PARENT))

        // Panel kanan: grid semua app
        adapter = AppAdapter()
        val grid = GridView(this).apply {
            numColumns = GridView.AUTO_FIT
            columnWidth = dp(110)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(8)
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setAdapter(adapter)
            setOnItemClickListener { _, _, pos, _ ->
                start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(apps[pos].cn))
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            leftMargin = dp(20)
        })

        setContentView(root)
        loadApps()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(pkgReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(pkgReceiver) } catch (_: Exception) {}
    }

    // Tekan Home saat sudah di launcher: tidak melakukan apa-apa
    override fun onBackPressed() {}

    private fun start(i: Intent) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Aplikasi tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bigButton(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 22f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#252B36")); cornerRadius = dp(16).toFloat()
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)).apply {
            bottomMargin = dp(12)
        }
    }

    private fun loadApps() {
        if (loading) return
        loading = true
        Thread {
            val pm = packageManager
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(q, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map {
                    AppItem(
                        it.loadLabel(pm).toString(), it.loadIcon(pm),
                        ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                    )
                }
                .sortedBy { it.label.lowercase() }
            ui.post {
                apps.clear(); apps.addAll(list)
                adapter.notifyDataSetChanged()
                loading = false
            }
        }.start()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val v = cv ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4), dp(8), dp(4), dp(8))
                addView(ImageView(context).apply {
                    id = 1; scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(56), dp(56)))
                addView(TextView(context).apply {
                    id = 2; textSize = 14f; setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER; maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, dp(6), 0, 0)
                })
            }
            val a = apps[p]
            v.findViewById<ImageView>(1).setImageDrawable(a.icon)
            v.findViewById<TextView>(2).text = a.label
            return v
        }
    }
}
