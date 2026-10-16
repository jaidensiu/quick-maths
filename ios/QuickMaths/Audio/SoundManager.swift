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
    private let tick = SoundEffect(resource: "Tick")
    private let correct = SoundEffect(resource: "Correct")
    private let pencil = PencilSound()

    func setForeground(_ isForeground: Bool) {
        music.setForeground(isForeground)
        pencil.setForeground(isForeground)
    }

    /// While the countdown or a game is showing, the music stops and the pencil gets ready.
    func setGameActive(_ isActive: Bool) {
        music.setAllowed(!isActive)
        pencil.setEnabled(isActive)
    }

    func playTick() {
        tick.play()
    }

    func playCorrect() {
        correct.play()
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
