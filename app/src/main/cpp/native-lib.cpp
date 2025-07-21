#include <jni.h>
#include <string>
#include <opencv2/core.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_app_index_MainActivity_stringFromJNI(JNIEnv* env,jobject /* this */) {
    std::string versionString=cv::getVersionString();
    std::string mensajeOpenCV="OpenCV version: "+versionString;
    return env->NewStringUTF(mensajeOpenCV.c_str());
}