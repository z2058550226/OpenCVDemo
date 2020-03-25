package com.bybutter.opencvdemo.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bybutter.opencvdemo.util.getTempImagePath
import com.bybutter.opencvdemo.util.tempImgDir
import org.jetbrains.anko.dip
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.math.roundToInt

class GrabCutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        const val FGD_GRAY: Int = 0x7F
        const val BGD_GRAY: Int = 0x3F
    }

    private var sourceImage: Bitmap? = null
    private var imageDisplayRatio = 1f
    private val displayRect = Rect()

    private val roiMarker = RoiRectMarker(Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = 0xFFFF0000.toInt()
        pathEffect = DashPathEffect(floatArrayOf(dip(4).toFloat(), dip(2).toFloat()), 0f)
        strokeWidth = dip(2).toFloat()
    })
    private val fgdMarker = PaintMarker(0xFFFFFFFF.toInt(), Color.rgb(FGD_GRAY, FGD_GRAY, FGD_GRAY))
    private val bgdMarker = PaintMarker(0xFF000000.toInt(), Color.rgb(BGD_GRAY, BGD_GRAY, BGD_GRAY))
    private var marker: Marker = fgdMarker

    fun markForeground() {
        marker = fgdMarker
    }

    fun markBackground() {
        marker = bgdMarker
    }

    fun markRoi() {
        marker = roiMarker
    }

    fun loadBitmap(path: String) {
        loadBitmap(BitmapFactory.decodeFile(getTempImagePath(path)))
    }

    private fun loadBitmap(srcImage: Bitmap) {
        sourceImage = srcImage
        val originImgAspectRatio = srcImage.width * 1f / srcImage.height
        val viewAspectRatio = width * 1f / height
        val isDisplayWidthAdapt = originImgAspectRatio > viewAspectRatio
        imageDisplayRatio = if (isDisplayWidthAdapt) {
            width * 1f / srcImage.width
        } else {
            height * 1f / srcImage.height
        }

        val displayWidth = (srcImage.width * imageDisplayRatio).toInt()
        val displayHeight = (srcImage.height * imageDisplayRatio).toInt()
        if (isDisplayWidthAdapt) {
            displayRect.set(0, (height - displayHeight) / 2, width, (height + displayHeight) / 2)
        } else {
            displayRect.set((width - displayWidth) / 2, 0, (width + displayWidth) / 2, height)
        }
        postInvalidate()
    }

    fun reset() {
        fgdMarker.reset()
        roiMarker.reset()
        bgdMarker.reset()
        postInvalidate()
    }

    @OpenCvInitNeeded
    private fun getPaintMask(marker: PaintMarker): Mat? {
        val srcImg = sourceImage ?: return null

        val pathBitmap = Bitmap.createBitmap(srcImg.width, srcImg.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(pathBitmap)
        val foregroundPath = Path()
        marker.path.offset(
            (-displayRect.left).toFloat(),
            (-displayRect.top).toFloat(),
            foregroundPath
        )
        Matrix().apply {
            val scaleFactor = 1 / imageDisplayRatio
            preScale(scaleFactor, scaleFactor)
            foregroundPath.transform(this)
        }
        canvas.drawPath(foregroundPath, marker.maskPaint)

        val mat = Mat()
        try {
            Utils.bitmapToMat(pathBitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
        } catch (e: Exception) {
            Timber.e(e, "Convert bitmap to mat fail: ")
            return null
        } finally {
            pathBitmap.recycle()
        }
        return mat
    }

    private fun getRoiRect(): org.opencv.core.Rect? {
        val srcImage = sourceImage ?: return null
        Timber.e("roiMarker.rect: ${roiMarker.rect}")
        val rectF = RectF(roiMarker.rect)
        rectF.offset((-displayRect.left).toFloat(), (-displayRect.top).toFloat())
        Matrix().apply {
            val scaleFactor = 1 / imageDisplayRatio
            preScale(scaleFactor, scaleFactor)
            mapRect(rectF)
        }
        fun Int.squeeze(range: IntRange): Int {
            if (this < range.first) return range.first
            if (this > range.last) return range.last
            return this
        }
        Timber.e("roi rectF: $rectF")
        return org.opencv.core.Rect(
            rectF.left.roundToInt().squeeze(0..srcImage.width),
            rectF.top.roundToInt().squeeze(0..srcImage.height),
            rectF.width().toInt().squeeze(0..srcImage.width),
            rectF.height().toInt().squeeze(0..srcImage.height)
        )
    }

    @OpenCvInitNeeded
    private fun getImageMat(): Mat? {
        val srcImg = sourceImage ?: return null
        val mat = Mat()
        try {
            Utils.bitmapToMat(srcImg, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        } catch (e: Exception) {
            Timber.e(e, "Convert the bitmap to mat fail: ")
            return null
        }
        return mat
    }

    @OpenCvInitNeeded
    fun grabCut() {
        Thread {
            val srcImage: Mat = getImageMat() ?: return@Thread
            val fgdMask: Mat = getPaintMask(fgdMarker) ?: return@Thread
            debugMat(fgdMask)
            val bgdMask: Mat = getPaintMask(bgdMarker) ?: return@Thread
            val roiRect = getRoiRect() ?: return@Thread
            val rowCount: Int = srcImage.rows()
            val colCount: Int = srcImage.cols()
            val mask = Mat(rowCount, colCount, CvType.CV_8UC1, Scalar(.0))
            val bgModel = Mat()
            val fgModel = Mat()
            val roiMask = Mat(mask, roiRect)
            roiMask.setTo(Scalar(Imgproc.GC_PR_FGD.toDouble()))

            for (row in roiRect.y..(roiRect.height + roiRect.y)) {
                for (col in roiRect.x..(roiRect.width + roiRect.x)) {
                    val bgdGrayScalar = bgdMask[row, col] ?: continue
                    if (bgdGrayScalar[0] > .0) {
                        mask.put(row, col, byteArrayOf(0))
                    }
                    val fgdGrayScalar = fgdMask[row, col] ?: continue
                    if (fgdGrayScalar[0] > .0) {
                        Timber.e("fgdGrayScalar[0].toString(): ${fgdGrayScalar[0].toString()}")
                        mask.put(row, col, byteArrayOf(1))
                    }
                }
            }

            val startTime = Core.getTickCount()
            Imgproc.grabCut(
                srcImage,
                mask,
                org.opencv.core.Rect(),
                bgModel,
                fgModel,
                5,
                Imgproc.GC_INIT_WITH_MASK
            )
            val spendTime = Core.getTickCount() - startTime
            Timber.e("Grab cut spend time: ${spendTime / Core.getTickFrequency() * 1000}")

            val prBgd = Mat()
            Core.compare(
                mask,
                Scalar(Imgproc.GC_PR_BGD.toDouble()),
                prBgd,
                Core.CMP_EQ
            )
            Core.subtract(mask, prBgd, mask)

            val foreground = Mat(mask.size(), CvType.CV_8UC4, Scalar.all(.0))
            srcImage.copyTo(foreground, mask)
            Imgproc.cvtColor(foreground, foreground, Imgproc.COLOR_BGR2RGBA)
            val resultBmp =
                Bitmap.createBitmap(foreground.cols(), foreground.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(foreground, resultBmp)

            foreground.release()
            roiMask.release()
            bgModel.release()
            fgModel.release()
            mask.release()
            srcImage.release()
            fgdMask.release()
            prBgd.release()

            post {
                reset()
                loadBitmap(resultBmp)
            }
        }.start()
    }

    private fun debugMat(mat: Mat) = Imgcodecs.imwrite("$tempImgDir/myMask.png", mat)
    private fun debugMask(mat: Mat) {
        val factorMat = Mat(mat.size(), CvType.CV_8UC1, Scalar(80.0))
        val displayMat = Mat(mat.size(), CvType.CV_8UC1)
        Core.multiply(mat, factorMat, displayMat)
        Imgcodecs.imwrite("$tempImgDir/myMask.png", displayMat)
    }

    fun release() {
        sourceImage?.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        sourceImage?.let {
            canvas.drawBitmap(it, null, displayRect, null)
        }
        canvas.drawRect(roiMarker.rect, roiMarker.paint)
        canvas.drawPath(bgdMarker.path, bgdMarker.paint)
        canvas.drawPath(fgdMarker.path, fgdMarker.paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        sourceImage ?: return super.onTouchEvent(event)

        if (marker.onTouchEvent(event)) return true

        return super.onTouchEvent(event)
    }

    private interface Marker {
        fun onTouchEvent(event: MotionEvent): Boolean
    }

    private inner class PaintMarker(val paintColor: Int, val maskColor: Int) : Marker {
        private var isDepicting = false
        private val prePoint = PointF()
        val path = Path()
        val paint: Paint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = paintColor
            strokeWidth = dip(2).toFloat()
        }
        val maskPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = maskColor
            strokeWidth = dip(2).toFloat()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val valid = displayRect.contains(event.x.toInt(), event.y.toInt())
            if (valid.not()) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDepicting = true
                    path.moveTo(event.x, event.y)
                    prePoint.x = event.x
                    prePoint.y = event.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDepicting.not()) return false
                    val endX = (prePoint.x + event.x) / 2
                    val endY = (prePoint.y + event.y) / 2
                    path.quadTo(prePoint.x, prePoint.y, endX, endY)
                    prePoint.x = event.x
                    prePoint.y = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDepicting.not()) return false
                    isDepicting = false
                    return true
                }
            }
            return false
        }

        fun reset() {
            path.reset()
        }
    }

    private inner class RoiRectMarker(val paint: Paint) : Marker {
        private var isDepicting = false
        val rect = RectF()

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDepicting = true
                    rect.setEmpty()
                    rect.left = event.x
                    rect.top = event.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDepicting.not()) return false
                    rect.right = event.x
                    rect.bottom = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDepicting.not()) return false
                    isDepicting = false
                    return true
                }
            }
            return false
        }

        fun reset() {
            rect.setEmpty()
        }
    }

    annotation class OpenCvInitNeeded
}