#include <jni.h>
#include "ffmpeg/ffmpeg.h"

#include <android/log.h>
#define TAG "FFmpegCmd"
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO, TAG, FORMAT, ##__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR, TAG, FORMAT, ##__VA_ARGS__);

JNIEXPORT jint JNICALL Java_com_frank_ffmpeg_FFmpegCmd_handle
(JNIEnv *env, jclass obj, jobjectArray commands){
    int argc = (*env)->GetArrayLength(env, commands);
    char **argv = (char**)malloc(argc * sizeof(char*));
    int i;
    int result;

    LOGI("%s","ffmpeg_FFmpegCmd_handle jni test log");

    for (i = 0; i < argc; i++) {
        jstring jstr = (jstring) (*env)->GetObjectArrayElement(env, commands, i);
        char* temp = (char*) (*env)->GetStringUTFChars(env, jstr, 0);
        argv[i] = malloc(1024);
        strcpy(argv[i], temp);
        (*env)->ReleaseStringUTFChars(env, jstr, temp);
    }
    //执行ffmpeg命令
    result =  run(argc, argv);
    //释放内存
    for (i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    return result;
}