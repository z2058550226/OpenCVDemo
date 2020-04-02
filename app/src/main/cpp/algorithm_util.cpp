//
// Created by butter on 2020/4/2.
//

#include "algorithm_util.h"

namespace au {
    void multiplyAsAlpha(const Mat &src, const Mat &alpha, Mat &dst) {
        double tt = getTickCount();
        for (size_t row = 0; row < src.rows; row++) {
            for (size_t col = 0; col < src.cols; col++) {
                Vec3b scalar = src.at<Vec3b>(row, col);
                uchar alphaGray = alpha.at<uchar>(row, col);
                dst.at<Vec3b>(row, col)[0] = (uchar) (scalar[0] * 1.0f * alphaGray / 255);
                dst.at<Vec3b>(row, col)[1] = (uchar) (scalar[1] * 1.0f * alphaGray / 255);
                dst.at<Vec3b>(row, col)[2] = (uchar) (scalar[2] * 1.0f * alphaGray / 255);
            }
        }
        tt = getTickCount() - tt;
        __android_log_print(ANDROID_LOG_ERROR, "SUIKA", "multiplyAsAlpha spend time: %g ms",
                            tt / getTickFrequency() * 1000);
    }
}