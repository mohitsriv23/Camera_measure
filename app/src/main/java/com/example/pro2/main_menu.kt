package com.example.pro2

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.view.View
import android.widget.Button


/**
 * Created by EL-MUSLIM on 4/27/2017.
 */
class main_menu : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.options)
        val btn_offline = findViewById<View>(R.id.btn_off) as Button
        val btn_online = findViewById<View>(R.id.btn_on) as Button
        val shapedrawable = ShapeDrawable()
        shapedrawable.shape = RectShape()
        shapedrawable.paint.color = Color.LTGRAY
        shapedrawable.paint.strokeWidth = 10f
        shapedrawable.paint.style = Paint.Style.STROKE
        btn_offline.background = shapedrawable
        btn_online.background = shapedrawable
        btn_offline.setOnClickListener {
            startActivity(
                Intent(
                    this@main_menu,
                    ImageAccess::class.java
                )
            )
        }
        btn_online.setOnClickListener { startActivity(Intent(this@main_menu, Online::class.java)) }
    }
}