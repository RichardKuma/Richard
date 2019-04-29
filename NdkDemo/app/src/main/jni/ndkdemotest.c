#include "com_rich_ndkdemo_NDKTools.h"

JNIEXPORT jstring JNICALL Java_com_rich_ndkdemo_NDKTools_getStringFromNDK(JNIEnv *env, jobject obj)
{
    return (*env)->NewStringUTF(env,"Hellow World，this is Richard first NDK Code");
}