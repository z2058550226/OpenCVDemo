package com.bybutter.opencvdemo.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bybutter.opencvdemo.R
import kotlinx.android.synthetic.main.activity_tutorial.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import timber.log.Timber


class TutorialActivity : AppCompatActivity(), CvCameraViewListener2 {
    companion object {
        private const val VIEW_MODE_RGBA = 0
        private const val VIEW_MODE_GRAY = 1
        private const val VIEW_MODE_CANNY = 2
        private const val VIEW_MODE_FEATURES = 5
    }

    private var mViewMode =
        VIEW_MODE_RGBA

    private lateinit var mItemPreviewRGBA: MenuItem
    private lateinit var mItemPreviewGray: MenuItem
    private lateinit var mItemPreviewCanny: MenuItem
    private lateinit var mItemPreviewFeatures: MenuItem

    private lateinit var mRgba: Mat
    private lateinit var mIntermediateMat: Mat
    private lateinit var mGray: Mat

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) = when (status) {
            LoaderCallbackInterface.SUCCESS -> {
                Timber.i("OpenCV loaded successfully")
                javaCameraView.enableView()
            }
            else -> super.onManagerConnected(status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_tutorial)
        javaCameraView.visibility = CameraBridgeViewBase.VISIBLE
        javaCameraView.setCvCameraViewListener(this)
        javaCameraView.setCameraPermissionGranted()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mItemPreviewRGBA = menu.add("Preview RGBA")
        mItemPreviewGray = menu.add("Preview GRAY")
        mItemPreviewCanny = menu.add("Canny")
        mItemPreviewFeatures = menu.add("Find features")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mViewMode = when (item) {
            mItemPreviewRGBA -> VIEW_MODE_RGBA
            mItemPreviewCanny -> VIEW_MODE_CANNY
            mItemPreviewFeatures -> VIEW_MODE_FEATURES
            mItemPreviewGray -> VIEW_MODE_GRAY
            else -> 0
        }
        return true
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mIntermediateMat = Mat(height, width, CvType.CV_8UC4)
        mGray = Mat(height, width, CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
        mGray.release()
        mIntermediateMat.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        when (mViewMode) {
            VIEW_MODE_GRAY ->  // input frame has gray scale format
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
            VIEW_MODE_RGBA ->  // input frame has RBGA format
                mRgba = inputFrame.rgba()
            VIEW_MODE_CANNY -> {
                // input frame has gray scale format
                mRgba = inputFrame.rgba()
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80.0, 100.0)
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
            }
            VIEW_MODE_FEATURES -> {
                // input frame has RGBA format
                mRgba = inputFrame.rgba()
                mGray = inputFrame.gray()
//                Imgproc.grabCut()
            }
        }

        return mRgba
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Timber.d("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Timber.d("OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onPause() {
        super.onPause()
        javaCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        javaCameraView?.disableView()
    }
}