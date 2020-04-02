#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include "algorithm_util.h"

using namespace cv;

const char *TAG = "SUIKA";

extern "C" JNIEXPORT jstring JNICALL
Java_com_bybutter_opencvdemo_util_CppBridge_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bybutter_opencvdemo_util_CppBridge_test(JNIEnv *env, jobject thiz, jlong mat_addr) {
    cv::Mat *srcImage = (cv::Mat *) mat_addr;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%i", srcImage->cols);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bybutter_opencvdemo_util_CppBridge_eraserCut(JNIEnv *env, jobject thiz, jlong src_addr,
                                                      jlong bgd_mask_addr, jlong dst_addr) {
    Mat *srcImage = (Mat *) src_addr;
    Mat *bgdMask = (Mat *) bgd_mask_addr;
    Mat *dstImage = (Mat *) dst_addr;

    const int rowCount = srcImage->rows;
    const int colCount = srcImage->cols;

    Mat mask(rowCount, colCount, CV_8UC1, Scalar(GC_PR_FGD));

    for (size_t row = 0; row < rowCount; row++) {
        for (size_t col = 0; col < colCount; col++) {
            uchar gray = bgdMask->at<uchar>(row, col);
            if (gray > 0) {
                mask.at<uchar>(row, col) = GC_BGD;
            }
        }
    }

    double time = getTickCount();

    const float scaleFactor = 0.5;
    const int scaledRows = static_cast<const int>(rowCount * scaleFactor);
    const int scaledCols = static_cast<const int>(colCount * scaleFactor);
    Size scaledSize(scaledRows, scaledCols);

    Mat scaledMat = Mat::zeros(scaledSize, CV_8UC3);
    Mat scaledMask = Mat::zeros(scaledSize, CV_8UC1);

    resize(*srcImage, scaledMat, scaledSize, 0, 0, INTER_AREA);
    resize(mask, scaledMask, scaledSize, 0, 0, INTER_LINEAR);

    double grabCutTotalTime = getTickCount();
    grabCut(scaledMat, scaledMask, Rect(), Mat(), Mat(), 3, GC_INIT_WITH_MASK);
    grabCutTotalTime = getTickCount() - grabCutTotalTime;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "grab cut spend time: %g ms",
                        grabCutTotalTime / getTickFrequency() * 1000);

    Mat probableBGD = Mat::zeros(scaledSize, CV_8UC1);
    cv::compare(scaledMask, GC_PR_BGD, probableBGD, cv::CMP_EQ);
    cv::subtract(scaledMask, probableBGD, scaledMask);

    resize(scaledMask, mask, mask.size(), 0, 0, INTER_CUBIC);

    Mat maskFactor = Mat::ones(mask.size(), CV_8UC1) * 85;
    multiply(mask, maskFactor, mask);
    blur(mask, mask, Size(4, 4));

    au::multiplyAsAlpha(*srcImage, mask, *dstImage);

    time = getTickCount() - time;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "total spend time: %g ms",
                        time / getTickFrequency() * 1000);

    mask.release();
    scaledMask.release();
    scaledMat.release();
    probableBGD.release();
    maskFactor.release();
}