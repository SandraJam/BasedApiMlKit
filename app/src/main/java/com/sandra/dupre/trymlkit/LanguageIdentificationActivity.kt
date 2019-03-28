package com.sandra.dupre.trymlkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import kotlinx.android.synthetic.main.activity_language_identification.*

class LanguageIdentificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_identification)

        pictureButton.setOnClickListener {
            applyOtherMagic(editText.text.toString())
        }
    }

    private fun applyOtherMagic(text: String) {
        FirebaseNaturalLanguage.getInstance().languageIdentification.identifyLanguage(text)
            .addOnSuccessListener {
                translateTextView.text = translateToFlag(it)
            }
            .addOnFailureListener {
                Snackbar.make(root, it.message ?: "Unknown error", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun translateToFlag(country: String) = when(country) {
        "en" -> giveMeFlag("GB")
        "fr" -> giveMeFlag("FR")
        "nl" -> giveMeFlag("NL")
        else -> String(Character.toChars(0x1F3C1))
    }

    private fun giveMeFlag(countryCode: String): String =
        letterToEmoji(countryCode, 0) + letterToEmoji(countryCode , 1)

    private fun letterToEmoji(word: String, index: Int): String =
        String(Character.toChars(Character.codePointAt(word, index) - 0x41 + 0x1F1E6))

}
