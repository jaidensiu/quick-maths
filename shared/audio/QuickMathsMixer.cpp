#include "QuickMathsMixer.h"

#include "Mixer.h"

struct QMMixer {
    quickmaths::Mixer mixer;
};

QMMixer *qm_mixer_create(void) {
    return new QMMixer();
}

void qm_mixer_destroy(QMMixer *mixer) {
    delete mixer;
}

bool qm_mixer_load_sample(QMMixer *mixer, int32_t sample_id, const float *mono_frames,
                          int32_t frame_count, int32_t sample_rate) {
    if (mono_frames == nullptr || frame_count < 2) {
        return false;
    }
    std::vector<float> frames(mono_frames, mono_frames + frame_count);
    return mixer->mixer.loadSample(sample_id, std::move(frames), sample_rate);
}

int32_t qm_mixer_play(QMMixer *mixer, int32_t sample_id, float volume, float rate) {
    return mixer->mixer.play(sample_id, volume, rate);
}

void qm_mixer_set_volume(QMMixer *mixer, int32_t handle, float volume) {
    mixer->mixer.setVolume(handle, volume);
}

void qm_mixer_set_rate(QMMixer *mixer, int32_t handle, float rate) {
    mixer->mixer.setRate(handle, rate);
}

void qm_mixer_stop(QMMixer *mixer, int32_t handle) {
    mixer->mixer.stop(handle);
}

void qm_mixer_stop_all(QMMixer *mixer) {
    mixer->mixer.stopAll();
}

void qm_mixer_render(QMMixer *mixer, float *interleaved_stereo, int32_t frame_count,
                     float sample_rate) {
    mixer->mixer.render(interleaved_stereo, frame_count, sample_rate);
}
