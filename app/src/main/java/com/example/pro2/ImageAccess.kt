package com.example.pro2

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by FM on 4/7/2017.
 */
class ImageAccess : AppCompatActivity() {
    var btn_take_image: Button? = null
    var btn_load_image: Button? = null
    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                galleryAddPic()
                val message_intent = Intent(this, Offline::class.java)
                message_intent.putExtra(message_key, mcurpath)
                startActivity(message_intent)
            }
        }
        if (requestCode == 0 && resultCode == RESULT_OK && null != data) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? =
                getContentResolver().query(selectedImage!!, filePathColumn, null, null, null)
            cursor?.moveToFirst()
            val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
            val picturePath = cursor?.getString(columnIndex!!)
            cursor?.close()
            val message_intent = Intent(this, Offline::class.java)
            message_intent.putExtra(message_key, picturePath)
            startActivity(message_intent)
        }
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity1)
        btn_take_image = findViewById(R.id.btn_take_image) as Button?
        btn_load_image = findViewById(R.id.btn_load_image) as Button?
        val shapedrawable = ShapeDrawable()
        shapedrawable.shape = RectShape()
        shapedrawable.paint.color = Color.LTGRAY
        shapedrawable.paint.strokeWidth = 10f
        shapedrawable.paint.style = Paint.Style.STROKE
        btn_take_image!!.background = shapedrawable
        btn_load_image!!.background = shapedrawable
        btn_take_image!!.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            outputMediaFile
            intent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(image)
            )
            startActivityForResult(intent, 1)
        }
        btn_load_image!!.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, 0)
        }
    }

    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(mcurpath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    companion object {
        const val message_key = "message.message" //connects 2 messages
        var image: File? = null
        var mcurpath: String? = null
        private val outputMediaFile: File?
            private get() {
                val mediaStorageDir =
                    File(Environment.getExternalStoragePublicDirectory(""), "Camera Measure")
                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        return null
                    }
                }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                image = File(mediaStorageDir.path + File.separator + "IMG_" + timeStamp + ".JPG")
                mcurpath = image!!.absolutePath
                return null
            }
    }
}