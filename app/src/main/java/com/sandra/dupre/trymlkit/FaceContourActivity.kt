package com.sandra.dupre.trymlkit

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour.*
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_face_contour.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FaceContourActivity : AppCompatActivity() {

    private val detector: FirebaseVisionFaceDetector

    private var isFront = true

    init {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
    }

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_contour)

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
                    drawView.changePoint(it.firstOrNull())
                }
            }
        }
    }
}


class DrawFace(context: Context, attrs: AttributeSet?, defStyle: Int) : View(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var paint = Paint()

    private var points: Map<Int, FloatArray> = emptyMap()

    private val colors = mapOf(
        FACE to ContextCompat.getColor(context, R.color.blue_one),
        NOSE_BOTTOM to ContextCompat.getColor(context, R.color.orange_one),
        NOSE_BRIDGE to ContextCompat.getColor(context, R.color.orange_one),
        RIGHT_EYE to ContextCompat.getColor(context, R.color.pink_one),
        LEFT_EYE to ContextCompat.getColor(context, R.color.pink_one),
        LEFT_EYEBROW_BOTTOM to ContextCompat.getColor(context, R.color.yellow_one),
        LEFT_EYEBROW_TOP to ContextCompat.getColor(context, R.color.yellow_one),
        RIGHT_EYEBROW_BOTTOM to ContextCompat.getColor(context, R.color.yellow_one),
        RIGHT_EYEBROW_TOP to ContextCompat.getColor(context, R.color.yellow_one),
        LOWER_LIP_BOTTOM to ContextCompat.getColor(context, R.color.green_one),
        LOWER_LIP_TOP to ContextCompat.getColor(context, R.color.green_one),
        UPPER_LIP_BOTTOM to ContextCompat.getColor(context, R.color.green_one),
        UPPER_LIP_TOP to ContextCompat.getColor(context, R.color.green_one)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.strokeWidth = 8f

        points.forEach {
            paint.color = colors[it.key] ?: Color.BLACK
            canvas.drawLines(it.value, paint)
        }
    }

    fun changePoint(face: FirebaseVisionFace?) {
        points = colors.mapValues { face?.transform(it.key) ?: FloatArray(0)  }
        invalidate()
    }

    private fun FirebaseVisionFace.transform(@FirebaseVisionFaceContour.ContourType contourType : Int) =
        getContour(contourType).points.map { listOf(it.x, it.y, it.x, it.y) }.flatten().drop(2).toFloatArray()
}
