package com.example.customviewproj

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.withStyledAttributes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * In order to properly draw a custom view that extends View, you need to:
 * - Calculate the view's size when it first appears, and each time that view's size changes,
 *   by overriding the onSizeChanged() method.
 * - Override the onDraw() method to draw the custom view,
 *   using a Canvas object styled by a Paint object.
 * - Call the invalidate() method when responding to a user click that changes how the view is drawn
 *   to invalidate the entire view, thereby forcing a call to onDraw() to redraw the view.
 */

// The @JvmOverloads annotation instructs the Kotlin compiler to generate
// overloads for this function that substitute default parameter values.
class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var fanSpeedLowColor = 0
    private var fanSpeedMediumColor = 0
    private var fanSpeedHighColor = 0
    private var fanSpeedOffColor = 0

    // Position variable which will be used to draw label and indicator circle position
    private val pointPosition: PointF = PointF(0.0f, 0.0f)

    private var radius = 0.0f                   // Radius of the circle.
    private var fanSpeed = FanSpeed.OFF         // The active selection.

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    init {
        isClickable = true

        context.withStyledAttributes(attrs, R.styleable.DialView) {
            fanSpeedLowColor = getColor(R.styleable.DialView_fanColorLow, 0)
            fanSpeedMediumColor = getColor(R.styleable.DialView_fanColorMedium, 0)
            fanSpeedHighColor = getColor(R.styleable.DialView_fanColorHigh, 0)
            fanSpeedOffColor = getColor(R.styleable.DialView_fanColorOff, 0)
        }
    }

    override fun performClick(): Boolean {
        // The call to super.performClick() must happen first,
        // which enables accessibility events as well as calls onClickListener().
        if (super.performClick()) return true

        fanSpeed = fanSpeed.next()
        updateContentDescription()
        setupAccessibility()

        invalidate()
        return true
    }

    /**
     * Override onSizeChanged() to calculate positions, dimensions, and any other values
     * related to your custom view's size, instead of recalculating them every time you draw.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Calculates the current radius of the dial's circle element.
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = when (fanSpeed) {
            FanSpeed.OFF -> fanSpeedOffColor
            FanSpeed.LOW -> fanSpeedLowColor
            FanSpeed.MEDIUM -> fanSpeedMediumColor
            FanSpeed.HIGH -> fanSpeedHighColor
        }

        // Draw the dial
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)

        // Draw the indicator circle.
        // Calculates the X,Y coordinates for the indicator center based on the current fan speed.
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius)
        paint.color = Color.BLACK
        canvas.drawCircle(pointPosition.x, pointPosition.y, radius / 12, paint)

        //Draw the text labels
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in FanSpeed.values()) {
            pointPosition.computeXYForSpeed(i, labelRadius)
            val label = resources.getString(i.label)
            canvas.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }

    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
        // Angles are in radians
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }

    private fun updateContentDescription() {
        contentDescription = resources.getString(fanSpeed.label)
    }

    /**
     * Every view has a tree of accessibility nodes, which may or may not correspond
     * to the actual layout components of the view.
     * Android's accessibility services navigates those nodes in order to find out
     * information about the view (such as speakable content descriptions, or possible actions
     * that can be performed on that view.)
     * When you create a custom view you may also need to override the node information
     * in order to provide custom information for accessibility.
     * In this case you will be overriding the node info to indicate that there is custom
     * information for the view's action.
     *
     * If TalkBack is turned on:
     *
     * Notice now that the phrase "Double-tap to activate" is now either "Double-tap to change"
     * (if the fan speed is less than high or 3)
     * or "Double-tap to reset"
     * (if the fan speed is already at high or 3).
     * Note that the prompt "Double-tap to..." is supplied by the TalkBack service itself.
     */
    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfoCompat?
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val labelResId = if (fanSpeed !=  FanSpeed.HIGH) R.string.change else R.string.reset

                // A typical action is a click or tap, as I'm use here,
                // but other actions can include gaining or losing the focus,
                // a clipboard operation (cut/copy/paste) or scrolling within the view.
                // The constructor for this class requires an action constant
                // (here, AccessibilityNodeInfo.ACTION_CLICK),
                // and a string that is used by TalkBack to indicate what the action is.

                val customClick = AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    context.getString(labelResId)
                )

                info?.addAction(customClick)
            }
        })
    }

    companion object {
        private const val RADIUS_OFFSET_LABEL = 30
        private const val RADIUS_OFFSET_INDICATOR = -35
    }
}