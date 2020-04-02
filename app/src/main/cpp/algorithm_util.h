//
// Created by butter on 2020/4/2.
//

#ifndef OPENCVDEMO_ALGORITHM_UTIL_H
#define OPENCVDEMO_ALGORITHM_UTIL_H

#include <opencv2/core.hpp>
#include <android/log.h>

using namespace cv;

namespace au{
    void multiplyAsAlpha(const Mat &src, const Mat &alpha, Mat &dst);
}

#endif //OPENCVDEMO_ALGORITHM_UTIL_H
