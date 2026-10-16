//
//  SoundManager.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Combine
import CoreGraphics

/// The app's audio, shared through the environment like Android's singleton `SoundManager`.
/// The game flow reports when it's on screen (music is off then, matching Android's
/// `MainViewModel`) and the app reports foreground changes.
final class SoundManager: ObservableObject {
    private let music = MusicPlayer()
    private let sfx = SfxEngine()
    private lazy var pencil = PencilSound(sfx: sfx)

    func setForeground(_ isForeground: Bool) {
        music.setForeground(isForeground)
        sfx.setForeground(isForeground)
        if !isForeground {
            pencil.stop()
        }
    }

    /// While the countdown or a game is showing, the music stops and the sound effects get ready.
    func setGameActive(_ isActive: Bool) {
        music.setAllowed(!isActive)
        sfx.setEnabled(isActive)
        if !isActive {
            pencil.stop()
        }
    }

    func playTick() {
        sfx.play(.tick)
    }

    func playCorrect() {
        sfx.play(.correct)
    }

    func startPencil() {
        pencil.start()
    }

    func updatePencilSpeed(pxPerMs speed: CGFloat) {
        pencil.updateSpeed(pxPerMs: speed)
    }

    func mutePencil() {
        pencil.mute()
    }

    func stopPencil() {
        pencil.stop()
    }
}
