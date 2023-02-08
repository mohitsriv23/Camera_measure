package com.example.pro2

import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.appindexing.AppIndex
import com.google.android.gms.common.api.GoogleApiClient
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class Offline() : AppCompatActivity() {
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private var client: GoogleApiClient? = null

    //Interface Variables
    var rbar: SeekBar? = null
    var btnref: Button? = null
    var btn_save_image: Button? = null
    var btn_mode: Button? = null
    var textView: TextView? = null
    var circle_1: ImageView? = null
    var circle_2: ImageView? = null
    var touch_original: ImageView? = null
    var touch_tmp: ImageView? = null
    var cropimage: ImageView? = null

    //popup menu Interface Variables
    var mBuilder: AlertDialog.Builder? = null
    var mView: View? = null
    var rb_cutom: RadioButton? = null
    var spinner: Spinner? = null
    var editText: EditText? = null
    var adapter: ArrayAdapter<CharSequence>? = null
    var aSwitch: Switch? = null

    //Variables
    var x = 0f
    var y = 0f
    var pixel_ratio = 0f
    var ref_len = 0f
    var circles_h = 0
    var circles_w = 0
    var small_local_pos_x = 0
    var small_local_pos_y = 0
    var crop_img_w = 0
    var crop_img_h = 0
    var threshold = 50
    var bmp: Bitmap? = null
    var c: Canvas? = null
    var recived_image_path: String? = null
    var input_unit: String? = null
    var out_unit = "cm"
    var refrence = false
    var hold: Boolean? = null
    var hold2: Boolean? = null
    var mode = true //True If REgion Growing  , False Custom Mode
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Intialize_Variables()
        val imgClose = findViewById(R.id.imgClose) as ImageButton
        imgClose.setOnClickListener {
            System.exit(0)
            startActivity(Intent(this@Offline, Offline::class.java))
        }
        rbar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                threshold = i
                textView!!.text = threshold.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        touch_original!!.setOnTouchListener { v, event ->
            Update_Bullets_Dimensions(event)
            Check_Either_Bullet_will_Move()
            Reset_Bullets_Properties_When_Action_Up(event)
            true
        }
        btnref!!.setOnClickListener {
            if (btnref!!.text.toString()
                    .equals("Keep This Measured Line", ignoreCase = true)
            ) keeping_measured_line()
            if (btnref!!.text.toString()
                    .equals("Take Reference Length", ignoreCase = true)
            ) Check_Case_User_Use()
        }
        btn_save_image!!.setOnClickListener { Save_Image_in_Gallary() }
        btn_mode!!.setOnClickListener {
            Set_initial_Configuration_Of_Popup_Menu()
            val rb_region_growing =
                mView!!.findViewById<View>(R.id.rb_regiongrowing) as RadioButton
            val rb_coin = mView!!.findViewById<View>(R.id.rb_coin) as RadioButton
            val rb_a4 = mView!!.findViewById<View>(R.id.rb_a4) as RadioButton
            rb_cutom!!.setOnClickListener {
                Set_Configuration_Of_Custom_Radio_Button(
                    rb_region_growing,
                    rb_coin,
                    rb_a4
                )
            }
            rb_region_growing.setOnClickListener {
                Set_Configuration_Of_RegionGrowing_Radio_Button(
                    rb_region_growing,
                    rb_coin,
                    rb_a4
                )
            }
            rb_a4.setOnClickListener {
                Set_Reference_Configurations_Of_Radio_Button(
                    rb_region_growing,
                    rb_coin,
                    rb_a4,
                    true,
                    false,
                    true,
                    false
                )
            }
            rb_coin.setOnClickListener {
                Set_Reference_Configurations_Of_Radio_Button(
                    rb_region_growing,
                    rb_coin,
                    rb_a4,
                    true,
                    true,
                    false,
                    false
                )
            }
            adapter = ArrayAdapter.createFromResource(
                this@Offline,
                R.array.unit_choices,
                android.R.layout.simple_spinner_item
            )
            adapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner!!.adapter = adapter
            mBuilder?.setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, which ->
                    if (rb_cutom!!.isChecked) {
                        ref_len = editText!!.text.toString().toFloat()
                        mode = false
                    }
                    Check_Measure_Type()
                    Set_Real_Reference_Length(rb_coin, rb_a4)
                    if (rb_region_growing.isChecked) {
                        rbar!!.visibility = View.VISIBLE
                    }
                })
            Set_Final_Configuration_Of_Popup_Menu(rb_region_growing)
        }
        aSwitch!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (aSwitch!!.isChecked) {
                aSwitch!!.text = "Cm"
                out_unit = "cm"
            } else {
                aSwitch!!.text = "Inch"
                out_unit = "in"
            }
        }
        client = GoogleApiClient.Builder(this).addApi(AppIndex.API).build()
    }

    /**
     * I-reset configurations's bullets.
     */
    fun Reset_Bullets_Properties_When_Action_Up(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP) {
            circle_1!!.alpha = 0.3f
            circle_2!!.alpha = 0.3f
            hold = false
            hold2 = false
        }
    }

    /**
     * I-get the positions X,Y of the circle from screen coordinates.
     * II-get width and height of circle and crop scene.
     */
    fun Update_Bullets_Dimensions(event: MotionEvent) {
        x = event.x
        y = event.y
        circles_w = circle_1!!.width
        circles_h = circle_1!!.height
        crop_img_w = cropimage!!.width
        crop_img_h = cropimage!!.height
    }

    /**
     * I-Check the touch X,Y within the region of any circle to move.
     * II-Update zooming scene around the area that user touched on it.
     */
    fun Check_Either_Bullet_will_Move() {
        if (move_circles(x, y) == 1 || hold == true) {
            hold = true
            circle_1!!.x = x - (circles_w / 2)
            circle_1!!.y = y - (circles_h / 2)
            circle_1!!.alpha = 0.8f
            circle_2!!.alpha = 0.3f
            crop_with_circle_move()
            if (!mode) crearPunto(
                circle_1!!.x + circles_w / 2, circle_1!!.y + circles_h / 2,
                circle_2!!.x + circles_w / 2, circle_2!!.y + circles_h / 2, Color.YELLOW
            )
        } else if (move_circles(x, y) == 2 || hold2 == true) {
            hold2 = true
            circle_2!!.x = x - (circles_w / 2)
            circle_2!!.y = y - (circles_h / 2)
            circle_1!!.alpha = 0.3f
            circle_2!!.alpha = 0.8f
            crop_with_circle_move()
            crearPunto(
                circle_1!!.x + circles_w / 2,
                circle_1!!.y + circles_h / 2,
                circle_2!!.x + circles_w / 2,
                circle_2!!.y + circles_h / 2,
                Color.YELLOW
            )
        }
    }

    /**
     * I-check the case (custom,region growing) that user will use it.
     * II-get pixel ratio.
     * III-update image with measured lines.
     */
    fun Check_Case_User_Use() {
        //custom case.
        if (!mode) {
            val CoorRefDistance = Math.sqrt(
                Math.pow((circle_1!!.x - circle_2!!.x).toDouble(), 2.0)
                        + Math.pow((circle_1!!.y - circle_2!!.y).toDouble(), 2.0)
            ).toFloat()
            get_Pixel_Ratio(CoorRefDistance)
        } else if (mode) {
            bmp = Bitmap.createBitmap(
                touch_original!!.width,
                touch_original!!.height,
                Bitmap.Config.ARGB_8888
            )
            c = Canvas(bmp!!)
            touch_tmp!!.draw(c)
            val CoorRefDistance = regionGrowing(x.toInt(), y.toInt(), threshold).toFloat()
            get_Pixel_Ratio(CoorRefDistance)
            touch_original!!.setImageBitmap(bmp)
            circle_2!!.visibility = View.VISIBLE
            mode = false
        }
        keeping_measured_line()
        btnref!!.text = "Keep This Measured Line"
        btnref!!.setBackgroundResource(R.drawable.keep_measured)
        refrence = true
    }

    /**
     * I-calculate pixel ratio about real reference length and coordination reference length.
     */
    fun get_Pixel_Ratio(CoorRefDistance: Float) {
        pixel_ratio = ref_len / CoorRefDistance
    }

    /**
     * I-save image in gallary.
     */
    fun Save_Image_in_Gallary() {
        var outStream: OutputStream? = null
        val file = File(recived_image_path)
        val image_path =
            File(Environment.getExternalStoragePublicDirectory(""), "Camera Measure/" + file.name)
        try {
            outStream = FileOutputStream(image_path)
            bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
            galleryAddPic(image_path.absolutePath)
        } catch (e: Exception) {
        }
        Toast.makeText(this@Offline, "Image Saved", Toast.LENGTH_SHORT).show()
    }

    /**
     * I-adjust the view of popup menu modes.
     */
    fun Set_initial_Configuration_Of_Popup_Menu() {
        mBuilder = AlertDialog.Builder(this@Offline)
        mView = getLayoutInflater().inflate(R.layout.mode_dialog, null)
        rb_cutom = mView!!.findViewById<View>(R.id.rb_custom) as RadioButton
        spinner = mView!!.findViewById<View>(R.id.spinner) as Spinner
        editText = mView!!.findViewById<View>(R.id.editText) as EditText
    }

    /**
     * I-adjust the view of custom radio button.
     */
    fun Set_Configuration_Of_Custom_Radio_Button(
        rb_region_growing: RadioButton,
        rb_coin: RadioButton,
        rb_a4: RadioButton
    ) {
        rb_region_growing.isChecked = false
        rb_coin.isChecked = false
        rb_a4.isChecked = false
        rb_cutom!!.isChecked = true
        rb_coin.visibility = View.INVISIBLE
        rb_a4.visibility = View.INVISIBLE
        spinner!!.visibility = View.VISIBLE
        editText!!.visibility = View.VISIBLE
        btn_mode!!.visibility = View.INVISIBLE
        circle_2!!.visibility = View.VISIBLE
        circle_1!!.visibility = View.VISIBLE
        btnref!!.visibility = View.VISIBLE
        aSwitch!!.visibility = View.VISIBLE
        btn_save_image!!.visibility = View.VISIBLE
        circle_2!!.alpha = 0.3f
        rb_cutom!!.y = rb_region_growing.bottom.toFloat()
    }

    /**
     * I-adjust the view of RegionGrowing radio button.
     */
    fun Set_Configuration_Of_RegionGrowing_Radio_Button(
        rb_region_growing: RadioButton,
        rb_coin: RadioButton,
        rb_a4: RadioButton
    ) {
        rb_region_growing.isChecked = true
        rb_coin.isChecked = false
        rb_a4.isChecked = false
        rb_cutom!!.isChecked = false
        btn_mode!!.visibility = View.INVISIBLE
        circle_1!!.visibility = View.VISIBLE
        btnref!!.visibility = View.VISIBLE
        btn_save_image!!.visibility = View.VISIBLE
        aSwitch!!.visibility = View.VISIBLE
        rb_cutom!!.visibility = View.VISIBLE
        rb_coin.visibility = View.VISIBLE
        rb_a4.visibility = View.VISIBLE
        spinner!!.visibility = View.INVISIBLE
        editText!!.visibility = View.INVISIBLE
        rb_cutom!!.y = spinner!!.y
    }

    /**
     * I-update configurations of radio button according to reference's type.
     */
    fun Set_Reference_Configurations_Of_Radio_Button(
        rb_region_growing: RadioButton,
        rb_coin: RadioButton,
        rb_a4: RadioButton,
        rgbool: Boolean,
        coinbool: Boolean,
        a4bool: Boolean,
        custombool: Boolean
    ) {
        rb_region_growing.isChecked = rgbool
        rb_coin.isChecked = coinbool
        rb_a4.isChecked = a4bool
        rb_cutom!!.isChecked = custombool
    }

    /**
     * I-check measure type(cm,inch).
     */
    fun Check_Measure_Type() {
        if (spinner!!.selectedItem.toString().equals("CM", ignoreCase = true)) input_unit =
            "cm" else input_unit = "in"
    }

    /**
     * I-set the real reference value.
     */
    fun Set_Real_Reference_Length(rb_coin: RadioButton, rb_a4: RadioButton) {
        if (rb_coin.isChecked) ref_len = 2.2f //length
        if (rb_a4.isChecked) ref_len = 36.37f //diameter
    }

    /**
     * I-show the dialog.
     */
    fun Set_Final_Configuration_Of_Popup_Menu(rb_region_growing: RadioButton?) {
        mBuilder?.setView(mView)
        val alertDialog: AlertDialog? = mBuilder?.create()
        alertDialog?.setTitle("Modes")
        alertDialog?.show()
    }

    /**
     * I-choose which circle will be move.
     */
    fun move_circles(x_t: Float, y_t: Float): Int {
        small_local_pos_x = circle_1!!.x.toInt()
        small_local_pos_y = circle_1!!.y.toInt()
        if (x_t > small_local_pos_x && x_t < (small_local_pos_x + circles_w)) if (y_t > small_local_pos_y && y_t < (small_local_pos_y + circles_h)) return 1
        small_local_pos_x = circle_2!!.x.toInt()
        small_local_pos_y = circle_2!!.y.toInt()
        if (x_t > small_local_pos_x && x_t < (small_local_pos_x + circles_w)) if (y_t > small_local_pos_y && y_t < (small_local_pos_y + circles_h)) return 2
        return 0
    }

    /**
     * I-crop area around touched point.
     */
    fun crop_with_circle_move() {
        var x_s = x.toInt() - (crop_img_w / 2)
        var y_s = y.toInt() - (crop_img_h / 2)
        var x_e = crop_img_w
        var y_e = crop_img_h
        if (x_s < 0) {
            x_e = crop_img_w + x_s
            x_s = 0
        }
        if (y_s < 0) {
            y_e = crop_img_h + y_s
            y_s = 0
        }
        if (x_s + crop_img_w > touch_original!!.width) {
            x_e = touch_original!!.width - x_s
        }
        if (y_s + crop_img_h > touch_original!!.height) {
            y_e = touch_original!!.height - y_s
        }
        touch_tmp!!.isDrawingCacheEnabled = true
        touch_tmp!!.buildDrawingCache(true)
        val bitmap = touch_tmp!!.drawingCache
        val tmp = Bitmap.createBitmap(bitmap, x_s, y_s, x_e, y_e)
        cropimage!!.setImageBitmap(tmp)
    }

    /**
     * I-darw measured line.
     */
    fun crearPunto(x: Float, y: Float, xend: Float, yend: Float, color: Int) {
        bmp = Bitmap.createBitmap(
            touch_original!!.width,
            touch_original!!.height,
            Bitmap.Config.ARGB_8888
        )
        c = Canvas(bmp!!)
        touch_tmp!!.draw(c)
        val p = Paint()
        val p1 = Paint()
        p1.strokeWidth = 5f
        p1.color = Color.BLUE
        p.strokeWidth = 5f
        p.color = color
        c!!.drawCircle(x, y, 10f, p1)
        c!!.drawCircle(xend, yend, 10f, p1)
        c!!.drawLine(x, y, xend, yend, p)
        if (refrence) {
            var actual_dist = Math.sqrt(
                (Math.pow((circle_1!!.x - circle_2!!.x).toDouble(), 2.0)
                        + Math.pow((circle_1!!.y - circle_2!!.y).toDouble(), 2.0))
            ).toFloat()
            actual_dist *= pixel_ratio
            if (input_unit === "cm" && out_unit === "in") actual_dist *= 0.393701.toFloat() else if (input_unit === "in" && out_unit === "cm") actual_dist /= 0.393701.toFloat()
            val text_paint = Paint()
            text_paint.color = Color.BLUE
            text_paint.style = Paint.Style.FILL_AND_STROKE
            text_paint.textSize = 30f
            text_paint.strokeWidth = 3f
            val text_x = Math.abs((circle_1!!.x - circle_2!!.x) / 2) + Math.min(
                circle_1!!.x, circle_2!!.x
            )
            val text_y = Math.abs((circle_1!!.y - circle_2!!.y) / 2) + Math.min(
                circle_1!!.y, circle_2!!.y
            )
            c!!.drawText(
                (String.format("%.2f", actual_dist) + " " + out_unit),
                text_x,
                text_y,
                text_paint
            )
        }
        touch_original!!.setImageBitmap(bmp)
    }

    /**
     * I-save measured line.
     */
    fun keeping_measured_line() {
        circle_1!!.x = (touch_original!!.width / 2).toFloat()
        circle_1!!.y = 0f
        circle_2!!.x = (touch_original!!.width / 2).toFloat()
        circle_2!!.y = (touch_original!!.height / 2).toFloat()
        touch_tmp!!.setImageBitmap(bmp)
    }

    /**
     * I-add the photo to gallary.
     */
    fun galleryAddPic(path: String?) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(path)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    /**
     * I-get the dimensions of object automatically using region growing
     */
    fun regionGrowing(x: Int, y: Int, Threshold: Int): Double {
        var x = x
        var y = y
        val open = TreeSet<Int>()
        val closed = TreeSet<Int>()
        val count = 0
        //scale image and x,y
        val image = scaleDown(bmp, 800f, true)
        x = (x * image.width.toFloat() / bmp!!.width.toFloat()).toInt()
        y = (y * image.height.toFloat() / bmp!!.height.toFloat()).toInt()
        //get Seed Node RGB values
        val color = image.getPixel(x, y)
        val A = Color.alpha(color)
        val R = Color.red(color)
        val G = Color.green(color)
        val B = Color.blue(color)
        var Xmin = x
        var Xmax = x
        var Ymin = y
        var Ymax = y
        var Ymin2 = y
        var Ymax2 = y
        val seedNode = x * image.width + y
        open.add(seedNode)
        while (!open.isEmpty()) {
            // remove current node from open list and add to closed list
            val currentNode = open.first()
            closed.add(currentNode)
            open.remove(currentNode)
            //get current node coordinates
            val xCurr = currentNode / image.width
            val yCurr = currentNode % image.width
            var upColor = 0
            var downColor = 0
            var leftColor = 0
            var rightColor = 0
            if (xCurr - 1 > 0) upColor = image.getPixel(xCurr - 1, yCurr)
            if (xCurr + 1 < image.width) downColor = image.getPixel(xCurr + 1, yCurr)
            if (yCurr - 1 > 0) leftColor = image.getPixel(xCurr, yCurr - 1)
            if (yCurr + 1 < image.height) rightColor = image.getPixel(xCurr, yCurr + 1)
            //Check upper Pixel
            if (((xCurr - 1 > 0) && (Math.abs(R - Color.red((upColor))) <= Threshold
                        ) && (Math.abs(B - Color.blue((upColor))) <= Threshold) && (Math.abs(
                    G - Color.green(
                        (upColor)
                    )
                ) <= Threshold
                        ) && (Math.abs(A - Color.alpha(upColor)) <= Threshold) &&
                        !closed.contains(currentNode - image.width) && !open.contains(currentNode - image.width))
            ) open.add((xCurr - 1) * image.width + yCurr)
            //Check lower Pixel
            if ((xCurr + 1 < image.width) && (Math.abs(R - Color.red((downColor))) <= Threshold
                        ) && (Math.abs(B - Color.blue((downColor))) <= Threshold
                        ) && (Math.abs(G - Color.green((downColor))) <= Threshold
                        ) && (Math.abs(A - Color.alpha(downColor)) <= Threshold
                        ) && !closed.contains(currentNode + image.width) && !open.contains(
                    currentNode + image.width
                )
            ) open.add((xCurr + 1) * image.width + yCurr)
            //Check left Pixel
            if (((yCurr - 1 > 0) && (Math.abs(R - Color.red((leftColor))) <= Threshold
                        ) && (Math.abs(B - Color.blue((leftColor))) <= Threshold) && (Math.abs(
                    G - Color.green(
                        (leftColor)
                    )
                ) <= Threshold
                        ) && (Math.abs(A - Color.alpha(leftColor)) <= Threshold) && !closed.contains(
                    currentNode - 1
                )
                        && !open.contains(currentNode - 1))
            ) open.add((xCurr) * image.width + yCurr - 1)
            //Check right Pixel
            if (((yCurr + 1 < image.height) && (Math.abs(R - Color.red((rightColor))) <= Threshold
                        ) && (Math.abs(B - Color.blue((rightColor))) <= Threshold
                        ) && (Math.abs(G - Color.green((rightColor))) <= Threshold
                        ) && (Math.abs(A - Color.alpha(rightColor)) <= Threshold) && !closed.contains(
                    currentNode + 1
                )
                        && !open.contains(currentNode + 1))
            ) open.add(((xCurr) * image.width) + yCurr + 1)
            image.setPixel(xCurr, yCurr, Color.RED)
            //check for min & max x,y
            if (xCurr < Xmin) {
                Xmin = xCurr
                Ymin2 = yCurr
                Ymin = Ymin2
            } else if (xCurr == Xmin) {
                Ymin = Math.min(Ymin, yCurr)
                Ymin2 = Math.max(Ymin2, yCurr)
            }
            if (xCurr > Xmax) {
                Xmax = xCurr
                Ymax2 = yCurr
                Ymax = Ymax2
            } else if (xCurr == Xmax) {
                Ymax = Math.max(Ymax, yCurr)
                Ymax2 = Math.min(Ymax2, yCurr)
            }
        }
        image.setPixel(Xmin, Ymin, Color.BLUE)
        image.setPixel(Xmax, Ymax, Color.BLUE)
        image.setPixel(Xmin, Ymin2, Color.GREEN)
        image.setPixel(Xmax, Ymax2, Color.GREEN)
        Xmax = (Xmax * (bmp!!.width.toFloat() / image.width.toFloat())).toInt()
        Xmin = (Xmin * (bmp!!.width.toFloat() / image.width.toFloat())).toInt()
        Ymax = (Ymax * (bmp!!.height.toFloat() / image.height.toFloat())).toInt()
        Ymin = (Ymin * (bmp!!.height.toFloat() / image.height.toFloat())).toInt()
        Ymax2 = (Ymax2 * (bmp!!.height.toFloat() / image.height.toFloat())).toInt()
        Ymin2 = (Ymin2 * (bmp!!.height.toFloat() / image.height.toFloat())).toInt()
        val dist1 = Math.sqrt(
            Math.pow((Xmax - Xmin).toDouble(), 2.0) + Math.pow(
                (Ymax - Ymin).toDouble(),
                2.0
            )
        )
        val dist2 = Math.sqrt(
            Math.pow((Xmax - Xmin).toDouble(), 2.0) + Math.pow(
                (Ymax2 - Ymin2).toDouble(),
                2.0
            )
        )
        bmp = scaleDown(image, Math.max(bmp!!.width, bmp!!.height).toFloat(), true)
        return Math.max(dist1, dist2)
    }

    fun Intialize_Variables() {
        rbar = findViewById(R.id.region_bar) as SeekBar?
        btnref = findViewById(R.id.btn_ref) as Button?
        btn_save_image = findViewById(R.id.btn_save_image) as Button?
        btn_mode = findViewById(R.id.btn_mode) as Button?
        textView = findViewById(R.id.textView) as TextView?
        touch_original = findViewById(R.id.imageView) as ImageView?
        touch_tmp = findViewById(R.id.img_tmp) as ImageView?
        val intent: Intent = getIntent()
        recived_image_path = intent.getStringExtra(message_key)
        touch_original!!.setImageBitmap(BitmapFactory.decodeFile(recived_image_path))
        touch_tmp!!.setImageBitmap(BitmapFactory.decodeFile(recived_image_path))

        //initiate Popup menu interface variable
        aSwitch = findViewById(R.id.switch2) as Switch?
        hold2 = false
        hold = hold2
        cropimage = findViewById(R.id.imageView2) as ImageView?
        circle_1 = findViewById(R.id.imv_circle1) as ImageView?
        circle_2 = findViewById(R.id.imv_circle2) as ImageView?
        circle_1!!.alpha = 0.3f
        circle_2!!.alpha = 0.3f
        circle_2!!.visibility = View.INVISIBLE
        circle_1!!.visibility = View.INVISIBLE
        rbar!!.max = 100
        rbar!!.progress = 50
    }

    companion object {
        val message_key = "message.message" //connects 2 messages

        /**
         * I-scale the bitmap down.
         */
        fun scaleDown(
            realImage: Bitmap?,
            maxImageSize: Float,
            filter: Boolean
        ): Bitmap {
            val ratio = Math.min(
                maxImageSize / realImage!!.width,
                maxImageSize / realImage.height
            )
            val width =
                Math.round(ratio * realImage.width)
            val height =
                Math.round(ratio * realImage.height)
            return Bitmap.createScaledBitmap(
                (realImage), width,
                height, filter
            )
        }
    }
}