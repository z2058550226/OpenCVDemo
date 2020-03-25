package com.bybutter.opencvdemo.grabcut

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bybutter.opencvdemo.R
import com.bybutter.opencvdemo.util.*
import kotlinx.android.synthetic.main.activity_grabcut.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.dip
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import timber.log.Timber

/**
 * Created by suikajy on 2020.3.6
 */

class GrabCutActivity : CoroutineActivity() {
    companion object {
        private const val REQ_OPEN_IMAGE = 0x11
        private const val TEMP_IMAGE_FILE_NAME = "grabCutTmp.png"
    }

    private val p1 = Point()
    private val p2 = Point()

    private var targetChose = false
    private lateinit var mBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grabcut)

        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCv initialize error")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_img -> {
                val getPicIntent = Intent(Intent.ACTION_GET_CONTENT)
                getPicIntent.type = "image/*"
                val pickPicIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                val chooserIntent = Intent.createChooser(getPicIntent, "Select Image")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickPicIntent))
                startActivityForResult(chooserIntent, REQ_OPEN_IMAGE)
                true
            }
            R.id.action_choose_target -> {
                targetChose = false
                imgDisplay.setOnTouchListener(object : View.OnTouchListener {
                    private var touchCount = 0

                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (touchCount == 0) {
                                    p1.x = event.x.toDouble()
                                    p1.y = event.y.toDouble()
                                    touchCount++
                                } else if (touchCount == 1) {
                                    p2.x = event.x.toDouble()
                                    p2.y = event.y.toDouble()

                                    val rectPaint = Paint()
                                    rectPaint.color = Color.RED
                                    rectPaint.style = Paint.Style.STROKE
                                    rectPaint.strokeWidth = dip(2).toFloat()

                                    val tmpBitmap = Bitmap.createBitmap(
                                        mBitmap.width,
                                        mBitmap.height,
                                        Bitmap.Config.RGB_565
                                    )
                                    val tmpCanvas = Canvas(tmpBitmap)

                                    tmpCanvas.drawBitmap(mBitmap, 0f, 0f, null)
                                    tmpCanvas.drawRect(
                                        RectF(
                                            p1.x.toFloat(), p1.y.toFloat(),
                                            p2.x.toFloat(), p2.y.toFloat()
                                        ), rectPaint
                                    )
                                    imgDisplay.setImageDrawable(
                                        BitmapDrawable(resources, tmpBitmap)
                                    )

                                    targetChose = true
                                    touchCount = 0
                                    imgDisplay.setOnTouchListener(null)
                                }
                            }
                            MotionEvent.ACTION_UP -> v.performClick()
                        }
                        return true
                    }
                })
                true
            }
            R.id.action_cut_image -> {
                launch {
                    if (!targetChose || !getTempImageFile(TEMP_IMAGE_FILE_NAME).exists()) return@launch
                    val tempImagePath = getTempImagePath(TEMP_IMAGE_FILE_NAME)
                    val img: Mat = Imgcodecs.imread(tempImagePath)
                    var background = Mat(img.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
                    val firstMask = Mat()
                    val bgModel = Mat()
                    val fgModel = Mat()
                    val source = Mat(1, 1, CvType.CV_8U, Scalar(Imgproc.GC_PR_FGD.toDouble()))
                    val dst = Mat()
                    val rect = Rect(p1, p2)

                    Imgproc.grabCut(
                        img,
                        firstMask, rect, bgModel, fgModel,
                        5,
                        Imgproc.GC_INIT_WITH_RECT
                    )
                    Core.compare(firstMask, source, firstMask, Core.CMP_EQ)

                    val foreground = Mat(img.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
                    img.copyTo(foreground, firstMask)

                    val color = Scalar(255.0, 0.0, 0.0, 255.0)
                    Imgproc.rectangle(img, p1, p2, color)

                    Imgproc.resize(background, background, img.size())

                    val mask = Mat(foreground.size(), CvType.CV_8UC1, WhiteScalar)
                    Imgproc.cvtColor(foreground, mask, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.threshold(mask, mask, 254.0, 255.0, Imgproc.THRESH_BINARY_INV)

                    val vals = Mat(1, 1, CvType.CV_8UC3, Scalar(0.0))
                    background.copyTo(dst)

                    background.setTo(vals, mask)

                    Core.add(background, foreground, dst, mask)

                    firstMask.release()
                    source.release()
                    bgModel.release()
                    fgModel.release()
                    vals.release()

                    Imgcodecs.imwrite(getOutputImageFilePath(TEMP_IMAGE_FILE_NAME), dst)

                    withContext(Dispatchers.Main) {
                        val outputImg = BitmapFactory.decodeFile(
                            getOutputImageFilePath(TEMP_IMAGE_FILE_NAME)
                        )

                        imgDisplay.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        imgDisplay.adjustViewBounds = true
                        imgDisplay.setPadding(2, 2, 2, 2)
                        imgDisplay.setImageBitmap(outputImg)
                        imgDisplay.invalidate()

                        Toast.makeText(application, "cut over", Toast.LENGTH_SHORT).show()
                    }

                    targetChose = false
                }
                true
            }
            R.id.action_test -> {
                launch {
                    if (!getTempImageFile(TEMP_IMAGE_FILE_NAME).exists()) return@launch
                    val tempImagePath = getTempImagePath(TEMP_IMAGE_FILE_NAME)

                    Timber.e("tempImagePath: $tempImagePath")
                    val originalImage: Mat = Imgcodecs.imread(tempImagePath)
                    val rowCount = originalImage.rows()
                    val colCount = originalImage.cols()
                    val mask = Mat(
                        originalImage.rows(),
                        originalImage.cols(),
                        CvType.CV_8UC1,
                        Scalar(0.0)
                    )
                    val bgModel = Mat()
                    val fgModel = Mat()

                    val roiMask = Mat(mask, Rect(200, 0, 350, 750))
                    roiMask.setTo(Scalar(Imgproc.GC_PR_FGD.toDouble()))
                    Timber.e("roiMask.size().toString(): ${roiMask.size().toString()}")
                    Timber.e("mask: ${mask}")

                    val newMask = Mat(rowCount, colCount, CvType.CV_8UC1, Scalar(1.0))
                    newMask.submat(Range(75, 80), Range(457, 463)).setTo(Scalar(255.0))
                    newMask.submat(Range(90, 110), Range(610, 630)).setTo(Scalar(.0))

                    for (row in 0..originalImage.rows()) {
                        for (col in 0..originalImage.cols()) {
                            val gray = newMask[row, col] ?: continue
                            when (gray[0]) {
                                .0 -> mask.put(row, col, byteArrayOf(0))
                                255.0 -> mask.put(row, col, byteArrayOf(1))
                            }
                        }
                    }
                    val startTime = Core.getTickCount()
                    Imgproc.grabCut(
                        originalImage,
                        mask,
                        Rect(),
                        bgModel,
                        fgModel,
                        5,
                        Imgproc.GC_INIT_WITH_MASK
                    )
                    val spendTime = Core.getTickCount() - startTime
                    Timber.e("spendTime: ${spendTime / Core.getTickFrequency() * 1000}")

                    val prBgd = Mat()
                    Core.compare(
                        mask,
                        Scalar(Imgproc.GC_PR_BGD.toDouble()),
                        prBgd,
                        Core.CMP_EQ
                    )
                    Core.subtract(mask, prBgd, mask)
                    val foreground = Mat(mask.size(), CvType.CV_8UC4, Scalar(.0, .0, .0, .0))

                    originalImage.copyTo(foreground, mask)
                    Imgcodecs.imwrite(getOutputImageFilePath(TEMP_IMAGE_FILE_NAME), foreground)

                    foreground.release()
                    roiMask.release()
                    bgModel.release()
                    fgModel.release()
                    mask.release()
                    originalImage.release()
                    newMask.release()
                    prBgd.release()

                    withContext(Dispatchers.Main) {
                        val outputImg = BitmapFactory.decodeFile(
                            getOutputImageFilePath(TEMP_IMAGE_FILE_NAME)
                        )

                        imgDisplay.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        imgDisplay.adjustViewBounds = true
                        imgDisplay.setPadding(2, 2, 2, 2)
                        imgDisplay.setImageBitmap(outputImg)
                        imgDisplay.invalidate()

                        Toast.makeText(application, "cut over", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OPEN_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Timber.i("onActivityResult")
            val imgUri = data.data ?: throw IllegalStateException("#1")
            val imgFd =
                contentResolver.openFileDescriptor(imgUri, "r") ?: throw IllegalStateException("#2")
            mBitmap = BitmapFactory.decodeFileDescriptor(imgFd.fileDescriptor)

            mBitmap.saveAsTempFile(TEMP_IMAGE_FILE_NAME)

            imgDisplay.setImageBitmap(mBitmap)
        }
    }
}