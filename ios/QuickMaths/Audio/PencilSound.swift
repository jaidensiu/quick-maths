//
//  PencilSound.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import AVFoundation
import OSLog

/// The pencil-scratch sound while writing an answer, like the pencil voice in Android's
/// `SoundManager`: it gets louder and a little higher as the pen moves faster.
///
/// The clip is shorter than most strokes, so a fresh copy starts every `retriggerInterval` while
/// the old one plays out. Two voices (player + varispeed) take turns, like the Android engine's
/// voices. The engine only runs during a game so it doesn't keep the audio hardware awake.
final class PencilSound {
    private static let minVolume: Float = 0.15
    private static let maxVolume: Float = 0.9
    /// Speeds are in pixels/ms to match Android's tuning.
    private static let fullSpeedPxPerMs: CGFloat = 1.5
    private static let retriggerInterval: TimeInterval = 0.45

    private struct Voice {
        let player = AVAudioPlayerNode()
        let varispeed = AVAudioUnitVarispeed()
    }

    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "PencilSound")
    private let engine = AVAudioEngine()
    private let voices = [Voice(), Voice()]
    private let buffer: AVAudioPCMBuffer?
    private var isEnabled = false
    private var isForeground = true
    /// The voice following the pen, and when it started; `nil` while the pen is up.
    private var current: (index: Int, startedAt: TimeInterval)?
    private var nextIndex = 0
    /// Logs a failing engine once per run of failures instead of on every stroke.
    private var didLogStartFailure = false

    init() {
        buffer = Self.loadBuffer(logger: logger)
        guard let buffer else { return }
        for voice in voices {
            engine.attach(voice.player)
            engine.attach(voice.varispeed)
            engine.connect(voice.player, to: voice.varispeed, format: buffer.format)
            engine.connect(voice.varispeed, to: engine.mainMixerNode, format: buffer.format)
        }
    }

    /// Runs the engine while a game is on screen and the app is in the foreground.
    func setEnabled(_ isEnabled: Bool) {
        self.isEnabled = isEnabled
        update()
    }

    func setForeground(_ isForeground: Bool) {
        self.isForeground = isForeground
        update()
    }

    func start() {
        guard current == nil else { return }
        play(volume: Self.minVolume, rate: 1)
    }

    func updateSpeed(pxPerMs speed: CGFloat) {
        guard let current else { return }
        let intensity = Float(min(max(speed / Self.fullSpeedPxPerMs, 0), 1))
        let volume = Self.minVolume + (Self.maxVolume - Self.minVolume) * intensity
        let rate = 0.9 + 0.2 * intensity
        if ProcessInfo.processInfo.systemUptime - current.startedAt >= Self.retriggerInterval {
            play(volume: volume, rate: rate)
        } else {
            voices[current.index].player.volume = volume
            voices[current.index].varispeed.rate = rate
        }
    }

    /// Silences the pen while it rests on the screen; moving again brings the sound back.
    func mute() {
        guard let current else { return }
        voices[current.index].player.volume = 0
    }

    func stop() {
        current = nil
        // Muted rather than stopped: cutting a playing buffer off mid-wave clicks, and the
        // silent tail runs out on its own within the clip's length.
        for voice in voices {
            voice.player.volume = 0
        }
    }

    private func play(volume: Float, rate: Float) {
        guard let buffer, ensureEngineRunning() else { return }
        let index = nextIndex
        nextIndex = (nextIndex + 1) % voices.count
        let voice = voices[index]
        voice.player.volume = volume
        voice.varispeed.rate = rate
        voice.player.scheduleBuffer(buffer, at: nil, options: .interrupts)
        voice.player.play()
        current = (index, ProcessInfo.processInfo.systemUptime)
    }

    private func update() {
        if isEnabled, isForeground {
            _ = ensureEngineRunning()
        } else {
            stop()
            for voice in voices {
                voice.player.stop()
            }
            engine.stop()
        }
    }

    /// Starts the engine if it should run and isn't. Also recovers from the system stopping it,
    /// e.g. after an audio interruption or a route change such as unplugging headphones.
    private func ensureEngineRunning() -> Bool {
        guard buffer != nil, isEnabled, isForeground else { return false }
        if engine.isRunning { return true }
        do {
            try engine.start()
            didLogStartFailure = false
            return true
        } catch {
            if !didLogStartFailure {
                didLogStartFailure = true
                logger.warning(
                    "Pencil audio engine failed to start; pencil sound off: \(error, privacy: .public)"
                )
            }
            return false
        }
    }

    private static func loadBuffer(logger: Logger) -> AVAudioPCMBuffer? {
        guard let url = Bundle.main.url(forResource: "PencilScratch", withExtension: "caf") else {
            logger.error("PencilScratch.caf is missing from the app bundle; pencil sound disabled")
            return nil
        }
        do {
            let file = try AVAudioFile(forReading: url)
            guard let buffer = AVAudioPCMBuffer(
                pcmFormat: file.processingFormat,
                frameCapacity: AVAudioFrameCount(file.length)
            ) else {
                logger.error("Couldn't allocate a buffer for PencilScratch.caf; pencil sound disabled")
                return nil
            }
            try file.read(into: buffer)
            return buffer
        } catch {
            logger.error("Failed to load PencilScratch.caf; pencil sound disabled: \(error, privacy: .public)")
            return nil
        }
    }
}
