package com.jaidensiu.quickmaths.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SfxPcmDecoder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    class DecodedPcm(val monoFrames: FloatArray, val sampleRate: Int)

    fun decode(resId: Int): DecodedPcm? {
        return runCatching { decodeResource(resId) }
            .onFailure { Log.w(TAG, "Failed to decode SFX resource $resId", it) }
            .getOrNull()
    }

    private fun decodeResource(resId: Int): DecodedPcm? {
        val extractor = MediaExtractor()
        try {
            context.resources.openRawResourceFd(resId).use { fd ->
                extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            val trackFormat = selectAudioTrack(extractor) ?: return null
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(trackFormat, null, null, 0)
                codec.start()
                return drainCodec(extractor = extractor, codec = codec, format = trackFormat)
            } finally {
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): MediaFormat? {
        for (track in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(track)
            if (format.getString(MediaFormat.KEY_MIME).orEmpty().startsWith(prefix = "audio/")) {
                extractor.selectTrack(track)
                return format
            }
        }
        return null
    }

    private fun drainCodec(
        extractor: MediaExtractor,
        codec: MediaCodec,
        format: MediaFormat,
    ): DecodedPcm? {
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        val chunks = mutableListOf<FloatArray>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var idlePolls = 0

        while (!outputDone) {
            if (idlePolls++ > MAX_IDLE_POLLS) {
                Log.w(TAG, "Decoder stalled after $MAX_IDLE_POLLS polls; giving up")
                return null
            }
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: return null
                    val size = extractor.readSampleData(inputBuffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
            when {
                outputIndex >= 0 -> {
                    idlePolls = 0
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        chunks.add(
                            toMonoFloats(
                                buffer = outputBuffer,
                                channelCount = channelCount,
                                pcmEncoding = pcmEncoding,
                            )
                        )
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }

                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = codec.outputFormat
                    sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                }
            }
        }

        val totalFrames = chunks.sumOf { it.size }
        if (totalFrames == 0) {
            return null
        }
        val monoFrames = FloatArray(totalFrames)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(destination = monoFrames, destinationOffset = offset)
            offset += chunk.size
        }
        return DecodedPcm(monoFrames = monoFrames, sampleRate = sampleRate)
    }

    private fun toMonoFloats(
        buffer: ByteBuffer,
        channelCount: Int,
        pcmEncoding: Int,
    ): FloatArray {
        val channels = channelCount.coerceAtLeast(minimumValue = 1)
        return when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floats = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
                FloatArray(floats.remaining() / channels) { frame ->
                    var sum = 0f
                    repeat(channels) { channel -> sum += floats.get(frame * channels + channel) }
                    sum / channels
                }
            }

            else -> {
                val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                FloatArray(shorts.remaining() / channels) { frame ->
                    var sum = 0f
                    repeat(channels) { channel -> sum += shorts.get(frame * channels + channel) }
                    sum / (channels * 32768f)
                }
            }
        }
    }

    private companion object {
        const val TAG = "SfxPcmDecoder"
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val MAX_IDLE_POLLS = 1_000
    }
}
