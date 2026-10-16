//
//  PencilSound.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import CoreGraphics
import Foundation

/// The pencil-scratch sound while writing an answer, like the pencil voice in Android's
/// `SoundManager`: it gets louder and a little higher as the pen moves faster.
///
/// The clip is shorter than most strokes, so a fresh voice starts every `retriggerInterval` while
/// the old one plays out, the same as on Android.
final class PencilSound {
    private static let minVolume: Float = 0.15
    private static let maxVolume: Float = 0.9
    /// Speeds are in pixels/ms to match Android's tuning.
    private static let fullSpeedPxPerMs: CGFloat = 1.5
    private static let retriggerInterval: TimeInterval = 0.45

    private let sfx: SfxEngine
    /// The voice following the pen, and when it started; `nil` while the pen is up.
    private var current: (handle: Int32, startedAt: TimeInterval)?
    /// The voice the last retrigger left playing out, silenced with the current one.
    private var previous: Int32 = QM_INVALID_HANDLE

    init(sfx: SfxEngine) {
        self.sfx = sfx
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
            sfx.setVolume(current.handle, volume)
            sfx.setRate(current.handle, rate)
        }
    }

    /// Silences the pen while it rests on the screen; moving again brings the sound back.
    func mute() {
        guard let current else { return }
        sfx.setVolume(current.handle, 0)
        sfx.setVolume(previous, 0)
    }

    func stop() {
        guard let current else { return }
        sfx.stop(current.handle)
        sfx.stop(previous)
        self.current = nil
    }

    private func play(volume: Float, rate: Float) {
        let handle = sfx.play(.pencil, volume: volume, rate: rate)
        guard handle != QM_INVALID_HANDLE else { return }
        previous = current?.handle ?? QM_INVALID_HANDLE
        current = (handle, ProcessInfo.processInfo.systemUptime)
    }
}
