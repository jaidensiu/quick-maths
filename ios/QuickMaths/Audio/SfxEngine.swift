//
//  SfxEngine.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-16.
//

import AVFoundation
import OSLog

/// Low-latency sound effects through the C++ mixer in `shared/audio`, the same one Android's
/// `NativeSfxEngine` drives through Oboe. The mixer fills an `AVAudioSourceNode` on the audio
/// thread; this class loads the clips and starts or stops the engine.
///
/// Plays through the shared `.ambient` session `MusicPlayer` sets up, so it obeys the Ring/Silent
/// switch and mixes with the music. The engine only runs during a game so it doesn't keep the
/// audio hardware awake.
final class SfxEngine {
    enum Sample: Int32, CaseIterable {
        case correct, tick, pencil

        var resource: String {
            switch self {
            case .correct: "Correct"
            case .tick: "Tick"
            case .pencil: "PencilScratch"
            }
        }
    }

    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "SfxEngine")
    private let mixer = qm_mixer_create()!
    private let engine = AVAudioEngine()
    private var isEnabled = false
    private var isForeground = true
    /// Logs a failing engine once per run of failures instead of on every play.
    private var didLogStartFailure = false

    init() {
        for sample in Sample.allCases {
            load(sample)
        }
        let hardwareRate = engine.outputNode.outputFormat(forBus: 0).sampleRate
        // The simulator or a missing route can report 0; the main mixer resamples either way.
        let sampleRate = hardwareRate > 0 ? hardwareRate : 48_000
        let format = AVAudioFormat(
            commonFormat: .pcmFormatFloat32, sampleRate: sampleRate, channels: 2, interleaved: true
        )!
        let source = AVAudioSourceNode(
            format: format, renderBlock: Self.renderBlock(mixer: mixer, sampleRate: Float(sampleRate))
        )
        engine.attach(source)
        engine.connect(source, to: engine.mainMixerNode, format: format)
    }

    deinit {
        engine.stop()
        qm_mixer_destroy(mixer)
    }

    /// Runs the engine while a game is on screen.
    func setEnabled(_ isEnabled: Bool) {
        self.isEnabled = isEnabled
        update()
    }

    func setForeground(_ isForeground: Bool) {
        self.isForeground = isForeground
        update()
    }

    /// Returns a handle for `setVolume`/`setRate`/`stop`, or `QM_INVALID_HANDLE` if it didn't play.
    @discardableResult
    func play(_ sample: Sample, volume: Float = 1, rate: Float = 1) -> Int32 {
        guard ensureEngineRunning() else { return QM_INVALID_HANDLE }
        return qm_mixer_play(mixer, sample.rawValue, volume, rate)
    }

    func setVolume(_ handle: Int32, _ volume: Float) {
        qm_mixer_set_volume(mixer, handle, volume)
    }

    func setRate(_ handle: Int32, _ rate: Float) {
        qm_mixer_set_rate(mixer, handle, rate)
    }

    /// Fades the voice out over a few milliseconds so it doesn't click.
    func stop(_ handle: Int32) {
        qm_mixer_stop(mixer, handle)
    }

    private func update() {
        if isEnabled, isForeground {
            _ = ensureEngineRunning()
        } else {
            engine.stop()
            // After the engine stops, so the audio thread isn't reading a voice that's cleared.
            qm_mixer_stop_all(mixer)
        }
    }

    /// Runs on the audio thread. Built outside the main-actor init so Swift doesn't infer the block
    /// as main-actor isolated, which Swift 6 mode would enforce with a crashing runtime check.
    private nonisolated static func renderBlock(
        mixer: OpaquePointer, sampleRate: Float
    ) -> AVAudioSourceNodeRenderBlock {
        { _, _, frameCount, bufferList in
            let buffer = UnsafeMutableAudioBufferListPointer(bufferList)[0]
            if let data = buffer.mData?.assumingMemoryBound(to: Float.self) {
                qm_mixer_render(mixer, data, Int32(frameCount), sampleRate)
            }
            return noErr
        }
    }

    /// Starts the engine if it should run and isn't. Also recovers from the system stopping it,
    /// e.g. after an audio interruption or a route change such as unplugging headphones.
    private func ensureEngineRunning() -> Bool {
        guard isEnabled, isForeground else { return false }
        if engine.isRunning { return true }
        do {
            try engine.start()
            didLogStartFailure = false
            return true
        } catch {
            if !didLogStartFailure {
                didLogStartFailure = true
                logger.warning("SFX engine failed to start; sound effects off: \(error, privacy: .public)")
            }
            return false
        }
    }

    /// Decodes the bundled clip to mono floats, the format the mixer takes on both platforms.
    private func load(_ sample: Sample) {
        let name = sample.resource
        guard let url = Bundle.main.url(forResource: name, withExtension: "caf") else {
            logger.error("\(name, privacy: .public).caf is missing from the app bundle; sound disabled")
            return
        }
        do {
            let file = try AVAudioFile(forReading: url)
            let format = file.processingFormat
            guard
                let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(file.length))
            else {
                logger.error("Couldn't allocate a buffer for \(name, privacy: .public); sound disabled")
                return
            }
            try file.read(into: buffer)
            guard let channels = buffer.floatChannelData else {
                logger.error("\(name, privacy: .public) didn't decode to floats; sound disabled")
                return
            }
            let channelCount = Int(format.channelCount)
            let mono = (0..<Int(buffer.frameLength)).map { frame in
                (0..<channelCount).reduce(Float(0)) { $0 + channels[$1][frame] } / Float(channelCount)
            }
            if !qm_mixer_load_sample(mixer, sample.rawValue, mono, Int32(mono.count), Int32(format.sampleRate)) {
                logger.error("The mixer rejected \(name, privacy: .public); sound disabled")
            }
        } catch {
            logger.error("Failed to load \(name, privacy: .public); sound disabled: \(error, privacy: .public)")
        }
    }
}
