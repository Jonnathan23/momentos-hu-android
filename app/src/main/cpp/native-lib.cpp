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
