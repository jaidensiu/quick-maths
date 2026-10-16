#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <vector>

namespace quickmaths {
    // Platform-independent SFX mixer shared by Android (Oboe) and iOS (AVAudioSourceNode).
    // Control calls may come from any thread; render() runs on the platform's audio thread.
    class Mixer {
    public:
        static constexpr int kMaxVoices = 12;
        static constexpr int kInvalidHandle = -1;

        bool loadSample(int32_t sampleId, std::vector<float> monoFrames, int32_t sampleRate);

        int32_t play(int32_t sampleId, float volume, float rate);

        void setVolume(int32_t handle, float volume);

        void setRate(int32_t handle, float rate);

        void stop(int32_t handle);

        // Drops every voice at once. Call only while render() can't run, i.e. the stream is
        // stopped, or the audio thread may keep reading a voice that play() is reusing.
        void stopAll();

        // Mixes all voices into interleaved stereo `out`, overwriting it.
        void render(float *out, int32_t numFrames, float outSampleRate);

    private:
        struct Sample {
            std::vector<float> frames;
            int32_t sampleRate = 0;
        };

        struct Voice {
            std::atomic<bool> active{false};
            std::atomic<bool> stopRequested{false};
            std::atomic<float> targetVolume{0.0f};
            std::atomic<float> rate{1.0f};
            const Sample *sample = nullptr;
            double position = 0.0;
            float currentVolume = 0.0f;
            uint32_t generation = 0;
        };

        Voice *voiceForHandle(int32_t handle);

        std::mutex mLock;
        std::unordered_map<int32_t, std::unique_ptr<Sample>> mSamples;
        std::array<Voice, kMaxVoices> mVoices;
    };

}
