#include "AudioEngine.h"

#include <android/log.h>

#define LOG_TAG "QuickMathsAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace quickmaths {
    bool AudioEngine::init(int32_t defaultSampleRate, int32_t defaultFramesPerBurst) {
        std::lock_guard<std::mutex> lock(mLock);
        if (mInitialized) {
            return true;
        }
        if (defaultSampleRate > 0) {
            oboe::DefaultStreamValues::SampleRate = defaultSampleRate;
        }
        if (defaultFramesPerBurst > 0) {
            oboe::DefaultStreamValues::FramesPerBurst = defaultFramesPerBurst;
        }
        if (!openStream()) {
            return false;
        }
        mInitialized = true;
        return true;
    }

    bool AudioEngine::openStream() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
                ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                ->setSharingMode(oboe::SharingMode::Shared)
                ->setFormat(oboe::AudioFormat::Float)
                ->setFormatConversionAllowed(true)
                ->setChannelCount(oboe::ChannelCount::Stereo)
                ->setUsage(oboe::Usage::Game)
                ->setContentType(oboe::ContentType::Sonification)
                ->setDataCallback(this)
                ->setErrorCallback(this);

        std::shared_ptr<oboe::AudioStream> stream;
        oboe::Result result = builder.openStream(stream);
        if (result != oboe::Result::OK) {
            LOGW("Failed to open output stream: %s", oboe::convertToText(result));
            return false;
        }

        int32_t burst = stream->getFramesPerBurst();
        if (burst > 0) {
            stream->setBufferSizeInFrames(burst * 2);
        }

        if (mForeground) {
            result = stream->requestStart();
            if (result != oboe::Result::OK) {
                LOGW("Failed to start output stream: %s", oboe::convertToText(result));
                stream->close();
                return false;
            }
        }

        LOGI("Stream open: sampleRate=%d framesPerBurst=%d bufferSize=%d perfMode=%s api=%s",
             stream->getSampleRate(), stream->getFramesPerBurst(),
             stream->getBufferSizeInFrames(),
             oboe::convertToText(stream->getPerformanceMode()),
             oboe::convertToText(stream->getAudioApi()));

        mStream = std::move(stream);
        return true;
    }

    bool
    AudioEngine::loadSample(int32_t sampleId, std::vector<float> monoFrames, int32_t sampleRate) {
        return mMixer.loadSample(sampleId, std::move(monoFrames), sampleRate);
    }

    int32_t AudioEngine::play(int32_t sampleId, float volume, float rate) {
        std::lock_guard<std::mutex> lock(mLock);
        // Without a stream the voice would sit in a slot and play stale once one reopens.
        if (!mInitialized || !mStream) {
            return kInvalidHandle;
        }
        return mMixer.play(sampleId, volume, rate);
    }

    void AudioEngine::setVolume(int32_t handle, float volume) {
        mMixer.setVolume(handle, volume);
    }

    void AudioEngine::setRate(int32_t handle, float rate) {
        mMixer.setRate(handle, rate);
    }

    void AudioEngine::stop(int32_t handle) {
        mMixer.stop(handle);
    }

    void AudioEngine::setForeground(bool foreground) {
        std::lock_guard<std::mutex> lock(mLock);
        mForeground = foreground;
        if (!mInitialized) {
            return;
        }
        if (foreground) {
            if (!mStream) {
                openStream();
            } else {
                const oboe::StreamState state = mStream->getState();
                if (state != oboe::StreamState::Starting && state != oboe::StreamState::Started) {
                    oboe::Result result = mStream->requestStart();
                    if (result != oboe::Result::OK) {
                        LOGW("Failed to restart stream: %s", oboe::convertToText(result));
                    }
                }
            }
        } else if (mStream) {
            mStream->stop();
            mMixer.stopAll();
        }
    }

    oboe::DataCallbackResult
    AudioEngine::onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) {
        mMixer.render(static_cast<float *>(audioData), numFrames,
                      static_cast<float>(stream->getSampleRate()));
        return oboe::DataCallbackResult::Continue;
    }

    void AudioEngine::onErrorAfterClose(oboe::AudioStream * /*stream*/, oboe::Result error) {
        if (error != oboe::Result::ErrorDisconnected) {
            LOGW("Stream closed with error: %s", oboe::convertToText(error));
            return;
        }
        LOGI("Stream disconnected; reopening");
        std::lock_guard<std::mutex> lock(mLock);
        mStream.reset();
        if (mInitialized && !openStream()) {
            LOGW("Reopen after disconnect failed; native SFX unavailable until next retry");
        }
    }

}
