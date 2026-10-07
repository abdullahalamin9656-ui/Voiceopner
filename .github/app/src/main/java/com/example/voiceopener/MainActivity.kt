package com.example.voiceopener

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private var langCode = "en-US"   // Bangla-r jonno "bn-BD"

    private val aliases = mapOf(
        "হোয়াটসঅ্যাপ" to "whatsapp", "ইউটিউব" to "youtube",
        "ফেসবুক" to "facebook", "ক্যামেরা" to "camera",
        "সেটিংস" to "settings", "গ্যালারি" to "gallery",
        "মেসেঞ্জার" to "messenger", "ক্রোম" to "chrome",
        "ফোন" to "phone", "গুগল ম্যাপ" to "maps"
    )
    private val fillers = listOf("open", "launch", "start", "খোলো", "খুলো", "ওপেন",
        "kholo", "khulo", "chalu", "korun", "koro", "please", "দাও", "চালু", "করো")

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) listen() else status.text = "Mic permission dorkar"
        }

    private val speech =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (text != null) handle(text) else status.text = "Kichu shuntey pai nai"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        status = TextView(this).apply {
            text = "Mic chepe bolun: \"open WhatsApp\""
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val mic = Button(this).apply {
            text = "🎤  Bolun"
            textSize = 24f
            setOnClickListener { startListening() }
        }
        val lang = Button(this).apply {
            text = "Language: English (tap to change)"
            setOnClickListener {
                langCode = if (langCode == "en-US") "bn-BD" else "en-US"
                text = "Language: " + if (langCode == "en-US") "English" else "বাংলা"
            }
        }
        root.addView(status); root.addView(mic); root.addView(lang)
        setContentView(root)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) listen()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Kon app khulbo?")
        }
        try { speech.launch(i) }
        catch (e: Exception) { status.text = "Speech service nai (Google app install korun)" }
    }

    private fun handle(spoken: String) {
        var q = spoken.lowercase(Locale.ROOT)
        aliases.forEach { (k, v) -> q = q.replace(k, v) }
        fillers.forEach { q = q.replace(it, " ") }
        q = q.trim().replace(Regex("\\s+"), " ")
        status.text = "Shunlam: \"$spoken\""

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)

        val match = apps.firstOrNull {
            val label = it.loadLabel(pm).toString().lowercase(Locale.ROOT)
            label == q || label.contains(q) || (q.isNotEmpty() && q.contains(label))
        }
        val intent = match?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "\"$q\" app pailam na", Toast.LENGTH_LONG).show()
    }
}
