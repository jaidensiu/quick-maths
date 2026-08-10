#include <jni.h>

#include <vector>

#include "AudioEngine.h"

#define QM_JNI_FN(ret, name)      \
    extern "C" JNIEXPORT ret JNICALL \
    Java_io_github_donburilabs_quickMaths_data_NativeSfxEngine_##name

namespace {
    quickmaths::AudioEngine &engine() {
        static quickmaths::AudioEngine instance;
        return instance;
    }
}

QM_JNI_FN(jboolean, nativeInit)(JNIEnv * /*env*/,jobject /*thiz*/, jint defaultSampleRate,
                                jint defaultFramesPerBurst) {
    return engine().init(defaultSampleRate, defaultFramesPerBurst) ? JNI_TRUE : JNI_FALSE;
}

QM_JNI_FN(jboolean, nativeLoadSample)(JNIEnv *env, jobject /*thiz*/, jint sampleId,
                                      jfloatArray monoFrames, jint sampleRate) {
    if (monoFrames == nullptr) {
        return JNI_FALSE;
    }
    const jsize length = env->GetArrayLength(monoFrames);
    if (length < 2) {
        return JNI_FALSE;
    }
    std::vector<float> pcm(static_cast<size_t>(length));
    env->GetFloatArrayRegion(monoFrames, 0, length, pcm.data());
    return engine().loadSample(sampleId, std::move(pcm), sampleRate) ? JNI_TRUE : JNI_FALSE;
}

QM_JNI_FN(jint, nativePlay)(JNIEnv * /*env*/, jobject /*thiz*/, jint sampleId, jfloat volume,
                            jfloat rate) {
    return engine().play(sampleId, volume, rate);
}

QM_JNI_FN(void, nativeSetVolume)(JNIEnv * /*env*/, jobject /*thiz*/, jint handle, jfloat volume) {
    engine().setVolume(handle, volume);
}

QM_JNI_FN(void, nativeSetRate)(JNIEnv * /*env*/, jobject /*thiz*/, jint handle, jfloat rate) {
    engine().setRate(handle, rate);
}

QM_JNI_FN(void, nativeStop)(JNIEnv * /*env*/, jobject /*thiz*/, jint handle) {
    engine().stop(handle);
}

QM_JNI_FN(void, nativeSetForeground)(JNIEnv * /*env*/, jobject /*thiz*/, jboolean foreground) {
    engine().setForeground(foreground == JNI_TRUE);
}
