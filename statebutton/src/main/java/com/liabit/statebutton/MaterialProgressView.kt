package com.liabit.statebutton

import android.content.Context
import android.graphics.*
import android.graphics.BlurMaskFilter.Blur
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.*
import android.view.animation.Interpolator
import kotlin.math.*

/**
 * Author:         songtao
 * CreateDate:     2020/12/17 11:33
 */
@Suppress("unused")
class MaterialProgressView : View {

    companion object {
        private val LINEAR_INTERPOLATOR: Interpolator = LinearInterpolator()
        private val END_CURVE_INTERPOLATOR: Interpolator = EndCurveInterpolator()
        private val START_CURVE_INTERPOLATOR: Interpolator = StartCurveInterpolator()

        /**
         * Layout info for the arrowhead in dp
         */
        private const val ARROW_WIDTH = 10
        private const val ARROW_HEIGHT = 5
        private const val ARROW_OFFSET_ANGLE = 5f

        /**
         * The duration of a single progress spin in milliseconds.
         */
        private const val ANIMATION_DURATION = 1000 * 80 / 60

        /**
         * The duration of a single progress spin in milliseconds.
         */
        private const val ROUNDED_RECTANGLE_ANIMATION_DURATION = 1000 * 80 / 40
        //private static final int ROUNDED_RECTANGLE_ANIMATION_DURATION = 20000;
        //private static final int ROUNDED_RECTANGLE_ANIMATION_DURATION = 20000;
        /**
         * The number of points in the progress "star".
         */
        private const val NUM_POINTS = 5f

        enum class Shape {
            ROUNDED_RECTANGLE, CIRCLE
        }

        private fun dp2px(context: Context, dp: Int): Float {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
        }
    }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }


    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        mRing = Ring(this)
        var innerRadius = dp2px(context, 10)
        var strokeWidth = dp2px(context, 10)
        var shape = Shape.CIRCLE
        var color = Color.BLACK
        var colorScheme = 0
        var strokeJoin = Paint.Join.MITER
        var strokeCap = Paint.Cap.SQUARE

        var trimStart = 0f
        var trimEnd = 0f
        var trimOffset = 0f
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaterialProgressView, defStyleAttr, defStyleRes)

            innerRadius = typedArray.getDimension(R.styleable.MaterialProgressView_innerRadius, innerRadius)
            strokeWidth = typedArray.getDimension(R.styleable.MaterialProgressView_strokeWidth, strokeWidth)
            mRingSize = typedArray.getDimension(R.styleable.MaterialProgressView_size, -1f)
            color = typedArray.getColor(R.styleable.MaterialProgressView_strokeColor, Color.BLACK)
            colorScheme = typedArray.getResourceId(R.styleable.MaterialProgressView_strokeColorScheme, 0)
            strokeJoin = when (typedArray.getInt(R.styleable.MaterialProgressView_strokeLineJoin, 0)) {
                Paint.Join.BEVEL.ordinal -> Paint.Join.BEVEL
                Paint.Join.ROUND.ordinal -> Paint.Join.ROUND
                else -> Paint.Join.MITER
            }
            strokeCap = when (typedArray.getInt(R.styleable.MaterialProgressView_strokeLineJoin, 0)) {
                Paint.Cap.BUTT.ordinal -> Paint.Cap.BUTT
                Paint.Cap.ROUND.ordinal -> Paint.Cap.ROUND
                else -> Paint.Cap.SQUARE
            }

            trimStart = typedArray.getFloat(R.styleable.MaterialProgressView_trimStart, 0f)
            trimEnd = typedArray.getFloat(R.styleable.MaterialProgressView_trimEnd, 0f)
            trimOffset = typedArray.getFloat(R.styleable.MaterialProgressView_trimOffset, 0f)

            shape = if (typedArray.getInt(R.styleable.MaterialProgressView_shape, 0) == 0) {
                Shape.CIRCLE
            } else {
                Shape.ROUNDED_RECTANGLE
            }
            typedArray.recycle()
        }
        mRing.ringInnerRadius = innerRadius
        mRing.strokeWidth = strokeWidth
        mRing.strokeCap = strokeCap
        mRing.strokeJoin = strokeJoin
        mRing.trimStart = trimStart
        mRing.trimEnd = trimEnd
        mRing.trimOffset = trimOffset
        mRing.setShape(shape)
        if (colorScheme != 0) {
            mRing.colorScheme = context.resources.getIntArray(colorScheme)
        } else {
            mRing.colorScheme = intArrayOf(color)
        }
        mRing.colorIndex = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minEdge = if (mRingSize > 0) {
            val left = (measuredWidth - mRingSize) / 2
            val top = (measuredHeight - mRingSize) / 2
            val right = (measuredWidth + mRingSize) / 2
            val bottom = (measuredWidth + mRingSize) / 2
            mRing.setBound(left, top, right, bottom)
            min(right - left, bottom - top)
        } else {
            mRing.setBound(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
            min(measuredWidth, measuredHeight).toFloat()
        }
        if (mRing.ringInnerRadius <= 0 || minEdge < 0) {
            mRing.strokeInset = ceil(mRing.strokeWidth / 2.0f)
        } else {
            mRing.strokeInset = minEdge / 2.0f - mRing.ringInnerRadius
        }
    }

    /**
     * The indicator ring, used to manage animation state.
     */
    private lateinit var mRing: Ring

    private var mRingSize = -1f

    fun setStrokeWidth(strokeWidth: Float) {
        mRing.strokeWidth = strokeWidth
        mRing.strokeInset = strokeWidth / 2
    }

    /**
     * @param show Set to true to display the arrowhead on the progress spinner.
     */
    fun showArrow(show: Boolean) {
        mRing.showArrow = show
    }

    /**
     * @param scale Set the scale of the arrowhead for the spinner.
     */
    fun setArrowScale(scale: Float) {
        mRing.arrowScale = scale
    }

    /**
     * Set the start and end trim for the progress spinner arc.
     *
     * @param startAngle start angle
     * @param endAngle   end angle
     */
    fun setTrim(startAngle: Float, endAngle: Float) {
        mRing.trimStart = startAngle
        mRing.trimEnd = endAngle
    }

    /**
     * Only works when hardware acceleration is turned off
     * [View.setLayerType] setLayerType(View.LAYER_TYPE_SOFTWARE, null)
     *
     * @param radius
     * @param style
     */
    fun setBlurMaskFilter(radius: Float, style: Blur?) {
        mRing.setBlurMaskFilter(radius, style)
    }

    /**
     * clear the BlurMaskFilter
     */
    fun clearBlurMaskFilter() {
        mRing.clearBlurMaskFilter()
    }

    /**
     * Only works when hardware acceleration is turned off
     *
     * @param radius
     * @param color
     */
    fun setShadowLayer(radius: Float, color: Int) {
        mRing.setShadowLayer(radius, color)
    }

    /**
     * clear the ShadowLayer
     */
    fun clearShadowLayer() {
        mRing.clearShadowLayer()
    }

    /**
     * Set the amount of rotation to apply to the progress spinner.
     *
     * @param rotation Rotation in degrees
     */
    fun setProgressRotation(rotation: Float) {
        mRing.rotation = rotation
    }

    /**
     * Set the colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colors scheme colors
     */
    fun setColorSchemeColors(vararg colors: Int) {
        mRing.colorScheme = colors
    }

    override fun onDraw(c: Canvas) {
        if (visibility != VISIBLE) {
            return
        }
        val saveCount = c.save()
        val centerX = width / 2f
        val centerY = height / 2f
        if (mRing.getShape() == Shape.CIRCLE) {
            c.rotate(mRing.groupRotation, centerX, centerY)
        } else {
            mRing.rotationExtra = mRing.groupRotation / 2f
        }
        mRing.draw(c, 0f, 0f, width.toFloat(), height.toFloat())
        c.restoreToCount(saveCount)
    }

    fun setColorFilter(colorFilter: ColorFilter?) {
        mRing.setColorFilter(colorFilter)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mRing.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mRing.stop()
    }


    private class Ring(private val view: View) {
        private val mTempBounds = RectF()
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val mArrowPaint = Paint()
        private var mStartingStartTrim = 0f
        private var mStartingEndTrim = 0f
        private var mStartingRotation = 0f
        private var mArrow: Path = Path()
        private var mPath: Path = Path()
        private var mRemainingPath: Path = Path()
        private var mRoundRectPath: Path = Path()
        private var mRoundRectPathLength = 0f
        private var mProgressMeasure: PathMeasure = PathMeasure()
        private var mArrowCopy: Path = Path()
        private var mArrowScaleMatrix: Matrix = Matrix()
        private var mBlurMaskFilter: BlurMaskFilter? = null
        private var mAnimating = true
        private var mShadowRadius = -1f
        private var mShadowColor = -1
        private var mShape = Shape.CIRCLE
        private var mRotationCount = 0f
        private var mAnimation: Animation? = null
        private var mFinishAnimation: Animation? = null

        private var mBound = RectF()

        var trimStart = 210.0f
        var trimEnd = 30.0f
        var trimOffset = 30.0f
        var rotation = 0.0f
        var groupRotation = 0.0f
        var rotationExtra = 0.0f
        var strokeWidth = 5.0f
        var strokeJoin = Paint.Join.MITER
        var strokeCap = Paint.Cap.SQUARE
        var strokeInset = 2.5f
        var colorScheme: IntArray = intArrayOf(Color.BLACK)
        var colorIndex = 0
        var showArrow = false
        var arrowScale = 0f
        var arrowWidth = 0
        var arrowHeight = 0
        var ringInnerRadius = 0f

        init {
            mPaint.isAntiAlias = true
            mPaint.strokeCap = Paint.Cap.ROUND
            mPaint.strokeJoin = Paint.Join.ROUND
            mPaint.style = Paint.Style.STROKE
            mArrowPaint.strokeWidth = 4f
            mArrowPaint.style = Paint.Style.FILL_AND_STROKE
            mArrowPaint.isAntiAlias = true

            arrowWidth = dp2px(view.context, ARROW_WIDTH).toInt()
            arrowHeight = dp2px(view.context, ARROW_HEIGHT).toInt()
        }

        /**
         * Draw the progress spinner
         */
        fun draw(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
            if (mBound.isEmpty) {
                mBound.set(left, top, right, bottom)
            }
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val arcBounds = mTempBounds
            arcBounds.set(mBound)
            arcBounds.inset(strokeInset, strokeInset)
            // Ensure the sweep angle isn't too small to draw.
            val diameter = min(arcBounds.width(), arcBounds.height())

            mPaint.color = colorScheme[colorIndex]
            mPaint.alpha = (view.alpha * 255).toInt()
            mPaint.strokeJoin = strokeJoin
            mPaint.strokeCap = strokeCap
            mPaint.strokeWidth = strokeWidth

            if (mBlurMaskFilter != null) {
                mPaint.maskFilter = mBlurMaskFilter
            }
            if (mShadowRadius > 0 && mShadowColor >= 0) {
                mPaint.setShadowLayer(mShadowRadius, 0f, 0f, mShadowColor)
            }

            val startAngle = (trimStart + rotation) * 360
            val endAngle = (trimEnd + rotation) * 360
            var sweepAngle = endAngle - startAngle
            val minAngle = (360.0 / (diameter * Math.PI)).toFloat()
            if (sweepAngle < minAngle && sweepAngle > -minAngle) {
                sweepAngle = sign(sweepAngle) * minAngle
            }
            if (mShape == Shape.ROUNDED_RECTANGLE) {
                val minR = diameter / 2
                /**
                 * 方案一，使用 clip path 裁剪画布
                 * calculateSectorClip(mClipArc, cx, cy, maxR, startAngle + mRotationExtra, sweepAngle);
                 * c.clipRect(0, 0, c.getWidth(), c.getHeight());
                 * c.clipPath(mClipArc);
                 * c.drawRoundRect(arcBounds, minR, minR, mPaint);
                 */
                if (mRoundRectPath.isEmpty) {
                    mRoundRectPath.addRoundRect(arcBounds, minR, minR, Path.Direction.CW)
                    mProgressMeasure.setPath(mRoundRectPath, false)
                    mRoundRectPathLength = mProgressMeasure.length
                }
                var angle = startAngle + rotationExtra
                while (angle >= 360) {
                    angle -= 360f
                }
                var startD = angle / 360 * mRoundRectPathLength
                var stopD = startD + sweepAngle / 360 * mRoundRectPathLength
                mPath.reset()
                mProgressMeasure.getSegment(startD, stopD, mPath, true)
                c.drawPath(mPath, mPaint)
                val remainingAngle = angle + sweepAngle - 360
                if (remainingAngle > 0) {
                    startD = 0f
                    stopD = remainingAngle / 360 * mRoundRectPathLength
                    mRemainingPath.reset()
                    mProgressMeasure.getSegment(startD, stopD, mRemainingPath, true)
                    c.drawPath(mRemainingPath, mPaint)
                }
            } else {
                c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint)
            }
            // Adjust the position of the triangle so that it is inset as much as the arc, but also centered on the arc.
            val inset = (strokeInset / 2 + arrowWidth / strokeWidth).toInt()
            val rad = Math.toRadians((startAngle + sweepAngle).toDouble())
            val x = (ringInnerRadius * cos(rad) + centerX).toFloat()
            val y = (ringInnerRadius * sin(rad) + centerY).toFloat()
            val a = Point((x - inset).toInt(), y.toInt())
            val b = Point((x - inset).toInt() + arrowWidth, y.toInt())
            val cPoint = Point((x - inset).toInt() + arrowWidth / 2, y.toInt() + arrowHeight)
            mArrow.rewind()
            mArrow.fillType = Path.FillType.EVEN_ODD
            mArrow.moveTo(a.x.toFloat(), a.y.toFloat())
            mArrow.lineTo(b.x.toFloat(), b.y.toFloat())
            mArrow.lineTo(cPoint.x.toFloat(), cPoint.y.toFloat())
            mArrow.lineTo(a.x.toFloat(), a.y.toFloat())
            mArrow.close()
            if (showArrow) {
                // draw a triangle
                val arrowRect = mTempBounds
                mArrow.computeBounds(arrowRect, true)
                mArrowScaleMatrix.setScale(arrowScale, arrowScale, arrowRect.centerX(), arrowRect.centerY())
                mArrow.transform(mArrowScaleMatrix, mArrowCopy)
                mArrowPaint.color = colorScheme[colorIndex]
                // Offset the arrow slightly so that it aligns with the cap on the arc
                c.rotate(startAngle + sweepAngle - ARROW_OFFSET_ANGLE, centerX, centerY)
                c.drawPath(mArrowCopy, mArrowPaint)
            }
        }

        /**
         * 获取一个扇形的 Path
         *
         * @param path
         * @param centerX
         * @param centerY
         * @param r
         * @param startAngle
         * @param sweepAngle
         */
        private fun calculateSectorClip(path: Path, centerX: Float, centerY: Float, r: Float, startAngle: Float, sweepAngle: Float) {
            //获得一个三角形的剪裁区
            path.reset()
            path.moveTo(centerX, centerY) //圆心
            path.lineTo((centerX + r * cos(startAngle / 180 * Math.PI)).toFloat(), (centerY + r * sin(startAngle / 180 * Math.PI)).toFloat())
            path.lineTo((centerX + r * cos((startAngle + sweepAngle) / 180 * Math.PI)).toFloat(), (centerY + r * sin((startAngle + sweepAngle) / 180 * Math.PI)).toFloat())
            path.close()
            //设置一个正方形，内切圆
            val rectF = RectF(centerX - r, centerY - r, centerX + r, centerY + r)
            //获得弧形剪裁区的方法
            path.addArc(rectF, startAngle, sweepAngle)
        }

        private fun calculateXY(rect: RectF, angle: Float): FloatArray {
            val cx = rect.centerX() //圆角矩形中心点 X 坐标
            val cy = rect.centerY() //圆角矩形中心点 Y 坐标
            val rx = (rect.width() / 2).toDouble()
            val ry = (rect.height() / 2).toDouble()
            val cc = (rect.width() - rect.height()) / 2.0 //矩形中心点到两边的圆形中心点的距离，此处是 width > height 的情况下
            val result = FloatArray(2)
            val radian = angle / 180 * Math.PI
            val ra = atan(ry / (rx - ry)) * 180 / Math.PI //点刚好落在圆弧上时与水平方向的夹角
            if (angle == 0f) { //右下方区域
                result[0] = rect.right
                result[1] = cy
            } else if (angle > 0 && angle < ra) { //右下方圆弧区域
                val d1 = cc * cos(radian)
                val f = cc * sin(radian)
                val d2 = sqrt(ry * ry - f * f)
                val d = d1 + d2
                result[0] = cx + (d * cos(radian)).toFloat()
                result[1] = cy + (d * sin(radian)).toFloat()
            } else if (angle >= ra && angle < 90) { //右下方
                result[0] = (ry / tan(radian)).toFloat() + cx
                result[1] = rect.bottom
            } else if (angle == 90f) {
                result[0] = cx
                result[1] = rect.bottom
            } else if (angle > 90 && angle <= 180 - ra) { //左下方
                result[0] = cx - (ry * tan(radian - Math.PI / 2)).toFloat()
                result[1] = rect.bottom
            } else if (angle > 180 - ra && angle < 180) {  //左下方圆弧区域
                val d1 = cc * cos(Math.PI - radian)
                val f = cc * sin(Math.PI - radian)
                val d2 = sqrt(ry * ry - f * f)
                val d = d1 + d2
                result[0] = cx - (d * cos(Math.PI - radian)).toFloat()
                result[1] = cy + (d * sin(Math.PI - radian)).toFloat()
            } else if (angle == 180f) {
                result[0] = rect.left
                result[1] = cy
            } else if (angle > 180 && angle < 180 + ra) { //左上方圆弧区域
                val d1 = cc * cos(radian - Math.PI)
                val f = cc * sin(radian - Math.PI)
                val d2 = sqrt(ry * ry - f * f)
                val d = d1 + d2
                result[0] = cx - (d * cos(radian - Math.PI)).toFloat()
                result[1] = cy - (d * sin(radian - Math.PI)).toFloat()
            } else if (angle >= 180 + ra && angle < 270) { //左上方
                result[0] = cx - (ry * tan(Math.PI * 3 / 2 - radian)).toFloat()
                result[1] = rect.top
            } else if (angle == 270f) {
                result[0] = cx
                result[1] = rect.top
            } else if (angle > 270 && angle <= 360 - ra) { //右上方
                result[0] = cx + (ry * tan(radian - Math.PI * 3 / 2)).toFloat()
                result[1] = rect.top
            } else { //右上方圆弧区域
                val d1 = cc * cos(2 * Math.PI - radian)
                val f = cc * sin(2 * Math.PI - radian)
                val d2 = sqrt(ry * ry - f * f)
                val d = d1 + d2
                result[0] = cx + (d * cos(2 * Math.PI - radian)).toFloat()
                result[1] = cy - (d * sin(2 * Math.PI - radian)).toFloat()
            }
            return result
        }

        fun setBound(left: Float, top: Float, right: Float, bottom: Float) {
            mBound.set(left, top, right, bottom)
        }

        /**
         * @param shape
         */
        fun setShape(shape: Shape) {
            mShape = shape
            setupAnimators()
        }

        fun getShape(): Shape {
            return mShape
        }

        /**
         * Only works when hardware acceleration is turned off
         * [View.setLayerType] setLayerType(View.LAYER_TYPE_SOFTWARE, null)
         *
         * @param radius
         * @param style
         */
        fun setBlurMaskFilter(radius: Float, style: Blur?) {
            mBlurMaskFilter = BlurMaskFilter(radius, style)
        }

        /**
         * clear the BlurMaskFilter
         */
        fun clearBlurMaskFilter() {
            mBlurMaskFilter = null
        }

        /**
         * @param radius
         * @param color
         */
        fun setShadowLayer(radius: Float, color: Int) {
            mShadowRadius = radius
            mShadowColor = color
        }

        /**
         * clear the ShadowLayer
         */
        fun clearShadowLayer() {
            mShadowRadius = -1f
            mShadowColor = -1
        }

        fun setColorFilter(filter: ColorFilter?) {
            mPaint.colorFilter = filter
        }

        /**
         * If the start / end trim are offset to begin with, store them so that
         * animation starts from that offset.
         */
        fun storeOriginals() {
            mStartingStartTrim = trimStart
            mStartingEndTrim = trimEnd
            mStartingRotation = rotation
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        private fun resetOriginals() {
            mStartingStartTrim = 0f
            mStartingEndTrim = 0f
            mStartingRotation = 0f
            trimStart = 0f
            trimEnd = 0f
            rotation = 0f
        }

        private fun setupAnimators() {
            val duration = if (getShape() == Shape.CIRCLE) ANIMATION_DURATION.toLong() else ROUNDED_RECTANGLE_ANIMATION_DURATION.toLong()
            val finishRingAnimation: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    // shrink back down and complete a full rotation before starting other circles
                    val targetRotation = (floor((mStartingRotation / 0.75f).toDouble()) + 1f).toFloat()
                    trimEnd = mStartingEndTrim + (mStartingStartTrim - mStartingEndTrim) * interpolatedTime
                    rotation = mStartingRotation + (targetRotation - mStartingRotation) * interpolatedTime
                    view.invalidate()
                }
            }
            finishRingAnimation.interpolator = LINEAR_INTERPOLATOR
            finishRingAnimation.duration = duration / 2
            finishRingAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    colorIndex = (colorIndex + 1) % colorScheme.size
                    resetOriginals()
                    view.invalidate()
                    view.startAnimation(mAnimation)
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            val animation: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    trimEnd = 0.75f * START_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime)
                    trimStart = 0.75f * END_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime)
                    rotation = 0.25f * interpolatedTime
                    groupRotation = 720.0f / NUM_POINTS * interpolatedTime + 720.0f * (mRotationCount / NUM_POINTS)

                    val diameter = min(mBound.width(), mBound.height())
                    val startAngle = (trimStart + rotation) * 360
                    val endAngle = (trimEnd + rotation) * 360
                    var sweepAngle = endAngle - startAngle
                    val minAngle = (360.0 / (diameter * Math.PI)).toFloat()
                    if (sweepAngle < minAngle && sweepAngle > -minAngle) {
                        sweepAngle = sign(sweepAngle) * minAngle
                    }
                    Log.d("TTTT", "start: $startAngle    end: $endAngle   $sweepAngle")

                    view.invalidate()
                }
            }
            //animation.repeatCount = Animation.INFINITE
            animation.repeatCount = 4
            animation.repeatMode = Animation.RESTART
            animation.interpolator = LINEAR_INTERPOLATOR
            animation.duration = duration
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    mRotationCount = 0f
                }

                override fun onAnimationEnd(animation: Animation) {
                    // do nothing
                }

                override fun onAnimationRepeat(animation: Animation) {
                    Log.d("TTTT","---------------------------------------------")
                    colorIndex = (colorIndex + 1) % colorScheme.size
                    resetOriginals()
                    mRotationCount = (mRotationCount + 1) % NUM_POINTS
                    view.invalidate()
                }
            })
            mFinishAnimation = finishRingAnimation
            mAnimation = animation
        }

        fun start() {
            if (mAnimation != null && mFinishAnimation != null) {
                mAnimating = true
                mAnimation?.reset()
                storeOriginals()
                if (mStartingStartTrim != 0f) {
                    view.startAnimation(mFinishAnimation)
                } else {
                    colorIndex = 0
                    resetOriginals()
                    view.invalidate()
                    view.startAnimation(mAnimation)
                }
            }
        }

        fun stop() {
            mAnimating = false
            view.clearAnimation()
            groupRotation = 0f
            colorIndex = 0
            resetOriginals()
            view.invalidate()
        }
    }

    /**
     * Squishes the interpolation curve into the second half of the animation.
     */
    private class EndCurveInterpolator : AccelerateDecelerateInterpolator() {
        override fun getInterpolation(input: Float): Float {
            return super.getInterpolation(max(0f, (input - 0.5f) * 2.0f))
        }
    }

    /**
     * Squishes the interpolation curve into the first half of the animation.
     */
    private class StartCurveInterpolator : AccelerateDecelerateInterpolator() {
        override fun getInterpolation(input: Float): Float {
            return super.getInterpolation(min(1f, input * 2.0f))
        }
    }
}