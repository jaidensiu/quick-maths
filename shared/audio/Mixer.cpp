#include "Mixer.h"

#include <algorithm>

#if defined(__ANDROID__)
#include <android/log.h>
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, "QuickMathsAudio", __VA_ARGS__)
#else
#include <os/log.h>
#define LOGW(...) os_log_error(OS_LOG_DEFAULT, __VA_ARGS__)
#endif

namespace quickmaths {
    namespace {
        constexpr int kSlotBits = 4;
        constexpr int32_t kSlotMask = (1 << kSlotBits) - 1;
        static_assert(Mixer::kMaxVoices <= kSlotMask + 1, "slot bits too small");
        constexpr float kVolumeRampSeconds = 0.005f;
    }

    bool Mixer::loadSample(int32_t sampleId, std::vector<float> monoFrames, int32_t sampleRate) {
        std::lock_guard<std::mutex> lock(mLock);
        if (monoFrames.size() < 2 || sampleRate <= 0) {
            LOGW("Rejecting sample %d: %zu frames at %d Hz", sampleId, monoFrames.size(),
                 sampleRate);
            return false;
        }
        if (mSamples.count(sampleId) > 0) {
            LOGW("Rejecting reload of sample %d", sampleId);
            return false;
        }
        auto sample = std::make_unique<Sample>();
        sample->frames = std::move(monoFrames);
        sample->sampleRate = sampleRate;
        mSamples[sampleId] = std::move(sample);
        return true;
    }

    int32_t Mixer::play(int32_t sampleId, float volume, float rate) {
        std::lock_guard<std::mutex> lock(mLock);
        auto it = mSamples.find(sampleId);
        if (it == mSamples.end()) {
            return kInvalidHandle;
        }
        for (int slot = 0; slot < kMaxVoices; slot++) {
            Voice &voice = mVoices[slot];
            if (voice.active.load(std::memory_order_acquire)) {
                continue;
            }
            voice.generation = (voice.generation + 1) & (UINT32_MAX >> (kSlotBits + 1));
            voice.sample = it->second.get();
            voice.position = 0.0;
            voice.currentVolume = 0.0f;
            voice.stopRequested.store(false, std::memory_order_relaxed);
            voice.targetVolume.store(volume, std::memory_order_relaxed);
            voice.rate.store(rate, std::memory_order_relaxed);
            voice.active.store(true, std::memory_order_release);
            return static_cast<int32_t>((voice.generation << kSlotBits) | slot);
        }
        LOGW("All %d voices busy; dropping sample %d", kMaxVoices, sampleId);
        return kInvalidHandle;
    }

    Mixer::Voice *Mixer::voiceForHandle(int32_t handle) {
        if (handle < 0) {
            return nullptr;
        }
        const int slot = handle & kSlotMask;
        const auto generation = static_cast<uint32_t>(handle) >> kSlotBits;
        if (slot >= kMaxVoices) {
            return nullptr;
        }
        Voice &voice = mVoices[slot];
        if (!voice.active.load(std::memory_order_acquire) || voice.generation != generation) {
            return nullptr;
        }
        return &voice;
    }

    void Mixer::setVolume(int32_t handle, float volume) {
        std::lock_guard<std::mutex> lock(mLock);
        if (Voice *voice = voiceForHandle(handle)) {
            voice->targetVolume.store(volume, std::memory_order_relaxed);
        }
    }

    void Mixer::setRate(int32_t handle, float rate) {
        std::lock_guard<std::mutex> lock(mLock);
        if (Voice *voice = voiceForHandle(handle)) {
            voice->rate.store(rate, std::memory_order_relaxed);
        }
    }

    void Mixer::stop(int32_t handle) {
        std::lock_guard<std::mutex> lock(mLock);
        if (Voice *voice = voiceForHandle(handle)) {
            voice->stopRequested.store(true, std::memory_order_relaxed);
        }
    }

    void Mixer::stopAll() {
        std::lock_guard<std::mutex> lock(mLock);
        for (Voice &voice: mVoices) {
            voice.active.store(false, std::memory_order_release);
        }
    }

    void Mixer::render(float *out, int32_t numFrames, float outSampleRate) {
        std::fill(out, out + numFrames * 2, 0.0f);

        const float rampStep = 1.0f / (kVolumeRampSeconds * outSampleRate);

        for (Voice &voice: mVoices) {
            if (!voice.active.load(std::memory_order_acquire)) {
                continue;
            }
            const Sample &sample = *voice.sample;
            const bool stopping = voice.stopRequested.load(std::memory_order_relaxed);
            const float target = stopping ? 0.0f : voice.targetVolume.load(
                    std::memory_order_relaxed);
            const double increment =
                    static_cast<double>(voice.rate.load(std::memory_order_relaxed)) *
                    sample.sampleRate / outSampleRate;
            const auto lastFrame = sample.frames.size() - 1;
            bool finished = false;

            for (int32_t i = 0; i < numFrames; i++) {
                const auto index = static_cast<size_t>(voice.position);
                if (index >= lastFrame) {
                    finished = true;
                    break;
                }
                voice.currentVolume += std::clamp(target - voice.currentVolume, -rampStep,
                                                  rampStep);
                const auto frac = static_cast<float>(voice.position - static_cast<double>(index));
                const float frame = sample.frames[index] +
                                    (sample.frames[index + 1] - sample.frames[index]) * frac;
                const float value = frame * voice.currentVolume;
                out[i * 2] += value;
                out[i * 2 + 1] += value;
                voice.position += increment;
            }

            if (finished || (stopping && voice.currentVolume <= 0.001f)) {
                voice.active.store(false, std::memory_order_release);
            }
        }

        for (int32_t i = 0; i < numFrames * 2; i++) {
            out[i] = std::clamp(out[i], -1.0f, 1.0f);
        }
    }

}
