//
//  SoundManager.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Combine

/// The app's audio, shared through the environment like Android's singleton `SoundManager`.
/// Screens report whether music is allowed (it isn't during the countdown or a game, matching
/// Android's `MainViewModel`) and the app reports foreground changes.
final class SoundManager: ObservableObject {
    private let music = MusicPlayer()
    private let tick = SoundEffect(resource: "Tick")
    private let correct = SoundEffect(resource: "Correct")

    func setForeground(_ isForeground: Bool) {
        music.setForeground(isForeground)
    }

    func setMusicAllowed(_ isAllowed: Bool) {
        music.setAllowed(isAllowed)
    }

    func playTick() {
        tick.play()
    }

    func playCorrect() {
        correct.play()
    }
}
