package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import java.util.concurrent.ConcurrentHashMap

object MapMarkerFactory {

    private val markerCache = ConcurrentHashMap<String, Drawable>()

    fun getRestaurantMarker(
        context: Context,
        verdict: Verdict,
        type: RestaurantType
    ): Drawable {
        val cacheKey = "${verdict.name}_${type.name}"
        return markerCache.getOrPut(cacheKey) {
            createRestaurantMarkerBitmap(context, verdict, type)
        }
    }

    fun getUserLocationMarker(context: Context): Drawable {
        return markerCache.getOrPut("USER_LOCATION") {
            createUserLocationMarkerBitmap(context)
        }
    }

    fun getCustomLocationMarker(context: Context): Drawable {
        return markerCache.getOrPut("CUSTOM_LOCATION") {
            createCustomLocationMarkerBitmap(context)
        }
    }

    private fun createRestaurantMarkerBitmap(
        context: Context,
        verdict: Verdict,
        type: RestaurantType
    ): Drawable {
        val width = 100
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pinColor = when (verdict) {
            Verdict.GO -> Color.parseColor("#10B981") // Emerald Green
            Verdict.NO_GO -> Color.parseColor("#EF4444") // Crimson Red
            Verdict.CAUTION -> Color.parseColor("#F59E0B") // Amber Yellow
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val centerX = width / 2f
        val circleRadius = 38f
        val circleCenterY = 44f
        val bottomTipY = 114f

        val pinPath = Path().apply {
            addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
            moveTo(centerX - circleRadius + 6f, circleCenterY + circleRadius - 10f)
            lineTo(centerX, bottomTipY)
            lineTo(centerX + circleRadius - 6f, circleCenterY + circleRadius - 10f)
            close()
        }

        canvas.drawPath(pinPath, paint)
        canvas.drawPath(pinPath, strokePaint)

        val iconRes = when (type) {
            RestaurantType.DINE_IN -> R.drawable.ic_marker_restaurant
            RestaurantType.DHABA -> R.drawable.ic_marker_dhaba
            RestaurantType.CLOUD_KITCHEN -> R.drawable.ic_marker_delivery
            RestaurantType.CAFE -> R.drawable.ic_marker_cafe
            RestaurantType.DRIVE_THRU -> R.drawable.ic_marker_takeaway
            RestaurantType.STREET_FOOD -> R.drawable.ic_marker_restaurant
        }

        val iconDrawable = ContextCompat.getDrawable(context, iconRes)
        if (iconDrawable != null) {
            val iconSize = 44
            val left = (centerX - iconSize / 2).toInt()
            val top = (circleCenterY - iconSize / 2).toInt()
            iconDrawable.setBounds(left, top, left + iconSize, top + iconSize)
            iconDrawable.setTint(Color.WHITE)
            iconDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createUserLocationMarkerBitmap(context: Context): Drawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = size / 2f

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333B82F6")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 36f, outerPaint)

        val middlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 22f, middlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(center, center, 22f, borderPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 8f, innerPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createCustomLocationMarkerBitmap(context: Context): Drawable {
        val width = 90
        val height = 110
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val circleRadius = 34f
        val circleCenterY = 40f
        val bottomTipY = 104f

        val pinPath = Path().apply {
            addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
            moveTo(centerX - circleRadius + 4f, circleCenterY + circleRadius - 8f)
            lineTo(centerX, bottomTipY)
            lineTo(centerX + circleRadius - 4f, circleCenterY + circleRadius - 8f)
            close()
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        canvas.drawPath(pinPath, fillPaint)
        canvas.drawPath(pinPath, strokePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, circleCenterY, 12f, dotPaint)

        val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, circleCenterY, 6f, innerDotPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
