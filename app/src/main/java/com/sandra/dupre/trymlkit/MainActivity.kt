package com.sandra.dupre.trymlkit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filterButton.setOnClickListener { startActivity(Intent(this, FilterActivity::class.java)) }
        faceContourButton.setOnClickListener { startActivity(Intent(this, FaceContourActivity::class.java)) }
        languageButton.setOnClickListener { startActivity(Intent(this, LanguageIdentificationActivity::class.java)) }
        chatAquaButton.setOnClickListener { startActivity(Intent(this, SmartReplyActivity::class.java).putExtra("pseudo", "Aqua")) }
        chatMeguminButton.setOnClickListener { startActivity(Intent(this, SmartReplyActivity::class.java).putExtra("pseudo", "Megumin")) }
    }
}
