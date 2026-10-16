// C interface to quickmaths::Mixer for Swift, which can't call C++ without enabling
// C++ interop for the whole target. Also the iOS bridging header, which warns on #pragma once.
#ifndef QUICKMATHS_MIXER_H
#define QUICKMATHS_MIXER_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define QM_INVALID_HANDLE (-1)

typedef struct QMMixer QMMixer;

QMMixer *qm_mixer_create(void);
void qm_mixer_destroy(QMMixer *mixer);

bool qm_mixer_load_sample(QMMixer *mixer, int32_t sample_id, const float *mono_frames,
                          int32_t frame_count, int32_t sample_rate);
int32_t qm_mixer_play(QMMixer *mixer, int32_t sample_id, float volume, float rate);
void qm_mixer_set_volume(QMMixer *mixer, int32_t handle, float volume);
void qm_mixer_set_rate(QMMixer *mixer, int32_t handle, float rate);
void qm_mixer_stop(QMMixer *mixer, int32_t handle);
void qm_mixer_stop_all(QMMixer *mixer);
void qm_mixer_render(QMMixer *mixer, float *interleaved_stereo, int32_t frame_count,
                     float sample_rate);

#ifdef __cplusplus
}
#endif

#endif
