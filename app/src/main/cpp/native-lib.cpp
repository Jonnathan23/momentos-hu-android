#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <opencv2/core/core.hpp>
// ImgProc: cvtColor, threshold, HuMoments…
#include <opencv2/imgproc/imgproc.hpp>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_app_index_MainActivity_computeHuDescriptor(JNIEnv* env, jobject /* this */, jobject bitmap) {
    // 1. Obtener información del Bitmap
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)                   return nullptr;

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;

    cv::Mat matRGBA(info.height, info.width, CV_8UC4, pixels);
    cv::Mat matGray;
    cv::cvtColor(matRGBA, matGray, cv::COLOR_RGBA2GRAY);

    // 1) Binarizar con **umbral fijo de 128** y THRESH_BINARY_INV
    cv::Mat matBin;
    cv::threshold(matGray, matBin, 128, 255, cv::THRESH_BINARY_INV);

    // 2) Esqueletización (morphological thinning)
    cv::Mat skeleton = cv::Mat::zeros(matBin.size(), CV_8UC1);
    cv::Mat element = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3, 3));
    cv::Mat temp, eroded, working = matBin.clone();
    while (true) {
        cv::erode(working, eroded, element);
        cv::dilate(eroded, temp, element);
        cv::subtract(working, temp, temp);
        cv::bitwise_or(skeleton, temp, skeleton);
        working = eroded.clone();
        if (cv::countNonZero(working) == 0) break;
    }

    // 3) Calcular momentos sobre el esqueleto
    cv::Moments moms;
    moms = cv::moments(skeleton, true);
    double huRaw[7];
    cv::HuMoments(moms, huRaw);

    // 4) **Sin normalización log** si tu CSV también está en crudo
    jfloat huOut[7];
    for (int i = 0; i < 7; ++i) {
        huOut[i] = static_cast<jfloat>(huRaw[i]);
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    // 5) Devolver array de 7 floats
    jfloatArray output = env->NewFloatArray(7);
    if (!output) return nullptr;
    env->SetFloatArrayRegion(output, 0, 7, huOut);
    return output;
}


extern "C" JNIEXPORT jfloatArray JNICALL
Java_app_index_MainActivity_computeShapeSignature(JNIEnv* env, jobject /* this */,jobject bitmap) {    // 1. Obtain bitmap info
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }

    // 2. Lock pixels for native access
    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return nullptr;
    }

    // 3. Wrap pixels into OpenCV Mat
    cv::Mat matRGBA(info.height, info.width, CV_8UC4, pixels);
    cv::Mat matGray;
    cv::cvtColor(matRGBA, matGray, cv::COLOR_RGBA2GRAY);

    // 4. Threshold (invert: black stroke on white background)
    cv::Mat matBin;
    cv::threshold(matGray, matBin, 128, 255, cv::THRESH_BINARY_INV);

    // 5. Skeletonization (morphological thinning)
    cv::Mat skeleton = cv::Mat::zeros(matBin.size(), CV_8UC1);
    cv::Mat element = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3, 3));
    cv::Mat temp, eroded, working = matBin.clone();
    while (true) {
        cv::erode(working, eroded, element);
        cv::dilate(eroded, temp, element);
        cv::subtract(working, temp, temp);
        cv::bitwise_or(skeleton, temp, skeleton);
        working = eroded.clone();
        if (cv::countNonZero(working) == 0) break;
    }

    // 6. Find contours
    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;
    cv::findContours(skeleton, contours, hierarchy, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_NONE);
    if (contours.empty()) {
        AndroidBitmap_unlockPixels(env, bitmap);
        return nullptr;
    }

    // 7. Select largest contour
    size_t largestIndex = 0;
    double maxArea = 0.0;
    for (size_t i = 0; i < contours.size(); ++i) {
        double area = cv::contourArea(contours[i]);
        if (area > maxArea) {
            maxArea = area;
            largestIndex = i;
        }
    }
    const std::vector<cv::Point>& contour = contours[largestIndex];

    // 8. Compute centroid
    cv::Moments cm = cv::moments(contour);
    cv::Point2f centroid(cm.m10 / cm.m00, cm.m01 / cm.m00);

    // 9. Sample distances along contour
    constexpr int sampleCount = 100;
    int rawSize = static_cast<int>(contour.size());
    std::vector<jfloat> signature(sampleCount);
    for (int i = 0; i < sampleCount; ++i) {
        int idx = (i * rawSize) / sampleCount;
        cv::Point2f pt = contour[idx];
        double dist = cv::norm(pt - centroid);
        signature[i] = static_cast<jfloat>(dist);
    }

    // 10. Unlock pixels
    AndroidBitmap_unlockPixels(env, bitmap);

    // 11. Create and fill Java float array
    jfloatArray out = env->NewFloatArray(sampleCount);
    if (out == nullptr) {
        return nullptr;
    }
    env->SetFloatArrayRegion(out, 0, sampleCount, signature.data());
    return out;
}
