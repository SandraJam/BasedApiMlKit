package com.sandra.dupre.trymlkit

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FilterActivity : AppCompatActivity() {

    private val detector: FirebaseVisionFaceDetector

    private var isFront = true

    init {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
    }

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        frontCameraButton.setOnClickListener {
            camera.toggleFacing()
            isFront = !isFront
            frontCameraButton.setImageResource(if (isFront) R.drawable.ic_camera_rear else R.drawable.ic_camera_front)
        }

        pictureButton.setOnClickListener {
            camera.captureImage { _, byteArray -> doMagicWithImage(byteArray) }
        }
    }

    override fun onStart() {
        super.onStart()
        camera.onStart()
    }

    override fun onResume() {
        super.onResume()
        camera.onResume()
    }

    override fun onPause() {
        camera.onPause()
        super.onPause()
    }

    override fun onStop() {
        camera.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun doMagicWithImage(byteArray: ByteArray) {
        job = CoroutineScope(Dispatchers.IO).launch {
            val image = FirebaseVisionImage.fromBitmap(
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            )

            detector.detectInImage(image).addOnSuccessListener {
                CoroutineScope(Dispatchers.Main).launch {
                    filterView.setBackgroundResource(getFilterColor(it.getOrNull(0)))
                }
            }
        }
    }

    private fun getFilterColor(face: FirebaseVisionFace?) =
        when {
            face == null -> android.R.color.transparent
            face.leftEyeOpenProbability > RANDOM_BUT_HIGH && face.rightEyeOpenProbability > RANDOM_BUT_HIGH -> R.color.pink_one
            face.leftEyeOpenProbability > RANDOM_BUT_HIGH -> R.color.orange_one
            face.rightEyeOpenProbability > RANDOM_BUT_HIGH  -> R.color.yellow_one
            else -> R.color.blue_one
        }

    companion object {
        private const val RANDOM_BUT_HIGH = 0.7
    }
}

