package com.example.watermypot

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi


class PercentageBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var paint1: Paint? = null
    private var paint2: Paint? = null
    private var moistureValue: Float = 0F

    init {
        paint1 = Paint()
        paint1?.color = Color.parseColor("#000000")
        paint1?.flags = Paint.ANTI_ALIAS_FLAG

        paint2 = Paint()
        paint2?.color = Color.parseColor("#00AFFE")
        paint2?.flags = Paint.ANTI_ALIAS_FLAG



    }

    fun setMoistureValue(value: Float) {
        this.moistureValue = value
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDraw(canvas: Canvas?) {
        val circlePaint = Paint()
        circlePaint.color = Color.GREEN
        circlePaint.flags = Paint.ANTI_ALIAS_FLAG

        val h = this.height.toFloat()
        val w = this.width.toFloat()

        canvas!!.drawRoundRect(0F, h - 70 - 40,
            w, h - 70, 15F, 15F, paint1!!)

        val x2 = w * moistureValue
        println("moisture value ${moistureValue * 100}")

        canvas!!.drawRoundRect(0F, h - 70 - 40,
                 x2, h - 70, 15F, 15F, paint2!!)


        super.onDraw(canvas)
    }



}
