package com.example.pro2

import android.hardware.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity


class Online : AppCompatActivity(), SensorEventListener {
    var sensorManager: SensorManager? = null
    var accelerateSensor: Sensor? = null
    var magenticSensor: Sensor? = null
    var geomagnetic: FloatArray? = null
    var gravity: FloatArray? = null
    var RR = FloatArray(9)
    var orientation = FloatArray(3)
    var mCamera: Camera? = null
    var mCameraView: CameraView? = null
    var cross_h: ImageView? = null
    var dm: DisplayMetrics? = null
    var down_angle = 0.0
    var up_angle = 0.0
    var object_height_from_ground = 0.0
    var angle_with_ground = 0.0
    var distance_from_object = 0.0
    var length_of_object = 0.0
    var human_length = 0.0
    var touch_ground_switch: Switch? = null
    var seek_human_length: SeekBar? = null
    var ORI: TextView? = null
    var text_sek: TextView? = null
    var rolls: String? = null
    var i = 0
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.online_lay)
        intialize_variables()
        try {
            mCamera = Camera.open() //you can use open(int) to use different cameras
        } catch (e: Exception) {
            Log.d("ERROR", "Failed to get camera: " + e.message)
        }
        if (mCamera != null) {
            mCameraView = CameraView(this, mCamera!!) //create a SurfaceView to show camera data
            val camera_view = findViewById(R.id.camera_view) as FrameLayout
            camera_view.addView(mCameraView) //add the SurfaceView to the layout
        }
        //btn to close the application
        val imgClose = findViewById(R.id.imgClose) as ImageButton
        imgClose.setOnClickListener {
            ORI!!.text = ""
            ORI!!.visibility = View.INVISIBLE
            down_angle = 0.0
            up_angle = 0.0
            angle_with_ground = 0.0
            touch_ground_switch!!.visibility = View.VISIBLE
            object_height_from_ground = 0.0
            length_of_object = 0.0
            distance_from_object = 0.0
        }
        ORI = findViewById(R.id.or) as TextView?
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
        accelerateSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magenticSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager!!.registerListener(this, accelerateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, magenticSensor, SensorManager.SENSOR_DELAY_NORMAL)
        cross_h!!.setOnClickListener { take_angles() }
        text_sek!!.text = "height from ground = $human_length CM"
        seek_human_length!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                human_length = i.toDouble()
                text_sek!!.text = "height from ground = $i CM"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        touch_ground_switch!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (touch_ground_switch!!.isChecked) touch_ground_switch!!.text =
                "On Ground" else touch_ground_switch!!.text =
                "Above Ground"
        }
    }

    protected override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, accelerateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, magenticSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    protected override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this, accelerateSensor)
        sensorManager!!.unregisterListener(this, magenticSensor)
    }

    override fun onSensorChanged(Event: SensorEvent) {
        if (Event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic =
            Event.values.clone() else if (Event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity =
            Event.values.clone()
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic)
            SensorManager.getOrientation(RR, orientation)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    /**
     * I-measure object started from ground and taller than human.
     */
    fun base_case() {
        down_angle = Math.abs(down_angle)
        up_angle = Math.abs(up_angle)
        distance_from_object = human_length / Math.tan(Math.toRadians(down_angle))
        length_of_object = human_length + Math.tan(Math.toRadians(up_angle)) * distance_from_object
        if (length_of_object / 100 > 0) {
            ORI!!.text = "length_of_object :\n" + (String.format(
                "%.2f",
                length_of_object / 100
            ) + " M" +
                    "\n" + "distance_from_object :\n" + String.format(
                "%.2f",
                distance_from_object / 100
            ) + " M")
            ORI!!.visibility = View.VISIBLE
        } else {
            Toast.makeText(this@Online, "Move Forward", Toast.LENGTH_LONG).show()
            down_angle = 0.0
            up_angle = 0.0
            touch_ground_switch!!.visibility = View.VISIBLE
        }
    }

    /**
     * I-measure object started from ground and shorter than human.
     */
    fun measure_small_object() {
        down_angle = Math.abs(down_angle)
        up_angle = Math.abs(up_angle)
        val distance_angle = 90 - down_angle
        distance_from_object = human_length * Math.tan(Math.toRadians(distance_angle))
        val part_of_my_tall = distance_from_object * Math.tan(Math.toRadians(up_angle))
        length_of_object = human_length - part_of_my_tall
        if (length_of_object / 100 > 0) {
            ORI!!.text = "length_of_object :\n" + (String.format(
                "%.2f",
                length_of_object / 100
            ) + " M" +
                    "\n" + "distance_from_object :\n" + String.format(
                "%.2f",
                distance_from_object / 100
            ) + " M")
            ORI!!.visibility = View.VISIBLE
        } else {
            Toast.makeText(this@Online, "Move Forward", Toast.LENGTH_LONG).show()
            down_angle = 0.0
            up_angle = 0.0
            touch_ground_switch!!.visibility = View.VISIBLE
        }
    }

    /**
     * I-take angles from sensors.
     */
    fun take_angles() {
        rolls += """
             ${Math.toDegrees(orientation[2].toDouble()) % 360 + 90}
             
             """.trimIndent()
        if (!touch_ground_switch!!.isChecked && angle_with_ground == 0.0) angle_with_ground =
            adjust_angle_rotation(
                Math.toDegrees(
                    orientation[2].toDouble()
                ) % 360 + 90
            ) else if (down_angle == 0.0) down_angle = adjust_angle_rotation(
            Math.toDegrees(
                orientation[2].toDouble()
            ) % 360 + 90
        ) else if (up_angle == 0.0) {
            up_angle = adjust_angle_rotation(Math.toDegrees(orientation[2].toDouble()) % 360 + 90)
            touch_ground_switch!!.visibility = View.INVISIBLE
            if (!touch_ground_switch!!.isChecked) object_calculations_doesnt_touch_ground() else object_calculations_touch_ground()
        }
    }

    /**
     * I-adjust angle rotation.
     */
    fun adjust_angle_rotation(angle: Double): Double {
        var temp: Double
        temp = angle
        if (temp > 90) {
            temp = 180 - temp
        }
        return temp
    }

    /**
     * I-choose automatically which method will be execute (on ground).
     */
    fun object_calculations_touch_ground() {
        if (down_angle < 0 && up_angle > 0) //base case
        {
            val temp = up_angle
            up_angle = down_angle
            down_angle = temp
            base_case()
        } else if ((down_angle > 0 && up_angle > 0) && (down_angle < up_angle)) //smaller object
        {
            val temp = up_angle
            up_angle = down_angle
            down_angle = temp
            measure_small_object()
        } else if (up_angle < 0 && down_angle > 0) //base case
            base_case() else  //smaller object
            measure_small_object()
    }

    /**
     * I-choose automatically which method will be execute (above ground).
     */
    fun object_calculations_doesnt_touch_ground() {
        if (angle_with_ground > 0 && (down_angle > 0) && up_angle < 0) object_on_eyes_level_calc() else if (angle_with_ground > 0 && down_angle < 0 && up_angle < 0) object_upper_eyes_level_calc() else if (angle_with_ground > 0 && down_angle > 0 && up_angle > 0) object_below_eyes_level_calc()
    }

    /**
     * I-measure object started above ground and on eye's level.
     */
    fun object_on_eyes_level_calc() {
        down_angle = Math.abs(down_angle)
        up_angle = Math.abs(up_angle)
        angle_with_ground = 90 - angle_with_ground
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground))
        val part_down = distance_from_object * Math.tan(Math.toRadians(down_angle))
        val part_up = distance_from_object * Math.tan(Math.toRadians(up_angle))
        length_of_object = part_down + part_up
        object_height_from_ground = human_length - part_down
        ORI!!.text = """length_of_object :
${String.format("%.2f", length_of_object / 100)} M
distance_from_object :
""" + String.format(
            "%.2f",
            distance_from_object / 100
        ) +
                " M" + "\n" + "height_from_ground :\n" + String.format(
            "%.2f",
            object_height_from_ground / 100
        ) + " M"
        ORI!!.visibility = View.VISIBLE
    }

    /**
     * I-measure object started above ground and upper than eye's level.
     */
    fun object_upper_eyes_level_calc() {
        down_angle = Math.abs(down_angle)
        up_angle = Math.abs(up_angle)
        angle_with_ground = 90 - angle_with_ground
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground))
        val part = distance_from_object * Math.tan(Math.toRadians(down_angle))
        val all = distance_from_object * Math.tan(Math.toRadians(up_angle))
        length_of_object = all - part
        object_height_from_ground = human_length + part
        ORI!!.text =
            """length_of_object :
${String.format("%.2f", length_of_object / 100)} M
distance_from_object :
${String.format("%.2f", distance_from_object / 100)} M
height_from_ground :
${String.format("%.2f", object_height_from_ground / 100)} M"""
        ORI!!.visibility = View.VISIBLE
    }

    /**
     * I-measure object started above ground and shorter than eye's level.
     */
    fun object_below_eyes_level_calc() {
        down_angle = Math.abs(down_angle)
        up_angle = Math.abs(up_angle)
        angle_with_ground = 90 - angle_with_ground
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground))
        val all = distance_from_object * Math.tan(Math.toRadians(down_angle))
        val part = distance_from_object * Math.tan(Math.toRadians(up_angle))
        length_of_object = all - part
        object_height_from_ground = human_length - all
        ORI!!.text =
            """length_of_object :
${String.format("%.2f", length_of_object / 100)} M
distance_from_object :
${String.format("%.2f", distance_from_object / 100)} M
height_from_ground :
${String.format("%.2f", object_height_from_ground / 100)} M"""
        ORI!!.visibility = View.VISIBLE
    }

    /**
     * I-darw measured line.
     */
    fun intialize_variables() {
        rolls = ""
        down_angle = 0.0
        up_angle = 0.0
        angle_with_ground = 0.0
        human_length = 162.0
        seek_human_length = findViewById(R.id.seekBar) as SeekBar?
        seek_human_length!!.progress = 160
        text_sek = findViewById(R.id.text_seek) as TextView?
        touch_ground_switch = findViewById(R.id.switch1) as Switch?
        cross_h = findViewById(R.id.crosshair) as ImageView?
        dm = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(dm)
        cross_h!!.layoutParams.width = dm!!.widthPixels * 15 / 100
        cross_h!!.layoutParams.height = dm!!.widthPixels * 15 / 100
    }
}