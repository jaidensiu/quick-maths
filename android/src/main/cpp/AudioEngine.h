#pragma once

#include <oboe/Oboe.h>

#include <cstdint>
#include <memory>
#include <mutex>
#include <vector>

#include "Mixer.h"

namespace quickmaths {
    class AudioEngine
            : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
    public:
        static constexpr int kInvalidHandle = Mixer::kInvalidHandle;

        bool init(int32_t defaultSampleRate, int32_t defaultFramesPerBurst);

        bool loadSample(int32_t sampleId, std::vector<float> monoFrames, int32_t sampleRate);

        int32_t play(int32_t sampleId, float volume, float rate);

        void setVolume(int32_t handle, float volume);

        void setRate(int32_t handle, float rate);

        void stop(int32_t handle);

        void setForeground(bool foreground);

        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;

        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result error) override;

    private:
        bool openStream();

        std::mutex mLock;
        std::shared_ptr<oboe::AudioStream> mStream;
        Mixer mMixer;
        bool mForeground = true;
        bool mInitialized = false;
    };

}
