package com.bybutter.opencvdemo.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bybutter.opencvdemo.util.CppBridge
import com.bybutter.opencvdemo.util.getTempImagePath
import com.bybutter.opencvdemo.util.tempImgDir
import org.jetbrains.anko.dip
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import timber.log.Timber

class EraserView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        const val BGD_GRAY: Int = 0x3F
        const val IS_SCALE = true
    }

    private var sourceImage: Bitmap? = null
    private var imageDisplayRatio = 1f
    private val displayRect = Rect()

    private val bgdMarker = PaintMarker(0xFF000000.toInt(), Color.rgb(BGD_GRAY, BGD_GRAY, BGD_GRAY))
    private var marker: Marker = bgdMarker
    private val loading = Loading()

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
        loading.show()
        Thread {
            val srcImage: Mat = getImageMat() ?: return@Thread
            CppBridge.test(srcImage.nativeObjAddr)
            val bgdMask: Mat = getPaintMask(bgdMarker) ?: return@Thread
            val dstMat: Mat = Mat.zeros(srcImage.size(), CvType.CV_8UC3)

            val rowCount: Int = srcImage.rows()
            val colCount: Int = srcImage.cols()
            val mask = Mat(rowCount, colCount, CvType.CV_8UC1, Scalar(.0))
            val bgModel = Mat()
            val fgModel = Mat()
            val prBgd = Mat() // probable background
            val foreground =
                Mat(mask.size(), CvType.CV_8UC3, Scalar.all(.0)) // final foreground result
            mask.setTo(Scalar(Imgproc.GC_PR_FGD.toDouble()))

            for (row in 0..rowCount) {
                for (col in 0..colCount) {
                    val bgdGrayScalar = bgdMask[row, col] ?: continue
                    if (bgdGrayScalar[0] > .0) {
                        mask.put(row, col, byteArrayOf(0))
                    }
                }
            }

            @Suppress("ConstantConditionIf")
            if (IS_SCALE) {
                val startTime = Core.getTickCount()

                val scaleFactor = 0.5
                val scaledSize = Size(rowCount * scaleFactor, colCount * scaleFactor)
                val scaledImg = Mat.zeros(scaledSize, CvType.CV_8UC3)
                val scaledMask = Mat.zeros(scaledSize, CvType.CV_8UC3)
                Imgproc.resize(srcImage, scaledImg, scaledSize, .0, .0, Imgproc.INTER_AREA)
                Imgproc.resize(mask, scaledMask, scaledSize, .0, .0, Imgproc.INTER_LINEAR)

                Imgproc.grabCut(
                    scaledImg,
                    scaledMask,
                    org.opencv.core.Rect(),
                    bgModel,
                    fgModel,
                    1,
                    Imgproc.GC_INIT_WITH_MASK
                )
                Core.compare(scaledMask, Scalar(Imgproc.GC_PR_BGD.toDouble()), prBgd, Core.CMP_EQ)
                Core.subtract(scaledMask, prBgd, scaledMask)

                Imgproc.resize(scaledMask, mask, mask.size(), .0, .0, Imgproc.INTER_CUBIC)
                Core.multiply(mask, Scalar(85.0), mask)
                Imgproc.blur(mask, mask, Size(4.0, 4.0))

                val alphaStartTime = Core.getTickCount()
                val srcScalar = byteArrayOf(0, 0, 0)
                val maskScalar = byteArrayOf(0)
                val dstScalar = byteArrayOf(0, 0, 0)
                for (row in 0..rowCount) {
                    for (col in 0..colCount) {
                        srcImage.get(row, col, srcScalar)
                        mask.get(row, col, maskScalar)
                        for (i in 0..2) {
                            dstScalar[i] =
                                (srcScalar[i].toUByte().toFloat() * maskScalar[0].toUByte()
                                    .toFloat() / 255f).toByte()
                        }
                        foreground.put(row, col, dstScalar)
                    }
                }
                val alphaSpendTime = Core.getTickCount() - alphaStartTime
                Timber.e("alpha spend time: ${alphaSpendTime / Core.getTickFrequency() * 1000}")
                val spendTime = Core.getTickCount() - startTime
                Timber.e("Grab cut spend time: ${spendTime / Core.getTickFrequency() * 1000}")
            } else {
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
                Core.compare(
                    mask,
                    Scalar(Imgproc.GC_PR_BGD.toDouble()),
                    prBgd,
                    Core.CMP_EQ
                )
                Core.subtract(mask, prBgd, mask)

                srcImage.copyTo(foreground, mask)
            }
            Imgproc.cvtColor(foreground, foreground, Imgproc.COLOR_BGR2RGBA)

            val resultBmp =
                Bitmap.createBitmap(foreground.cols(), foreground.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(foreground, resultBmp)

            foreground.release()
            bgModel.release()
            fgModel.release()
            mask.release()
            srcImage.release()
            prBgd.release()

            post {
                loading.dismiss()
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
        canvas.drawPath(bgdMarker.path, bgdMarker.paint)
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
                    grabCut()
                    return true
                }
            }
            return false
        }

        fun reset() {
            path.reset()
        }
    }

    private inner class Loading {
        private val dialog by lazy {
            val activity = (context as? Activity) ?: return@lazy null
            AlertDialog.Builder(activity)
                .setMessage("I'm cutting something...")
                .create()
        }

        fun show() = dialog?.show()
        fun dismiss() = dialog?.dismiss()
    }

    annotation class OpenCvInitNeeded
}