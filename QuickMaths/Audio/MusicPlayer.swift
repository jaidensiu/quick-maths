//
//  MusicPlayer.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import AVFoundation
import OSLog

/// Loops the background music while the app is in the foreground, like the Android app's
/// `SoundManager` music player.
///
/// Uses the `.ambient` audio session, Apple's recommended category for game soundtracks: it is
/// muted by the Ring/Silent switch and mixes with other apps instead of stopping them. When the
/// user is already listening to something else, iOS hints that secondary audio like this should
/// stay silent, so the music waits until that audio stops.
final class MusicPlayer {
    private static let volume: Float = 0.35

    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "Music")
    private let session = AVAudioSession.sharedInstance()
    private var player: AVAudioPlayer?
    private var observers: [NSObjectProtocol] = []
    private var isForeground = false
    private var isInterrupted = false

    init() {
        do {
            try session.setCategory(.ambient)
        } catch {
            logger.error("Failed to set the ambient audio session: \(error, privacy: .public)")
        }
        player = makePlayer()
        observeSession()
    }

    deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    func setForeground(_ isForeground: Bool) {
        self.isForeground = isForeground
        update()
    }

    private func update() {
        guard let player else { return }
        let shouldPlay = isForeground && !isInterrupted && !session.secondaryAudioShouldBeSilencedHint
        if shouldPlay, !player.isPlaying {
            if !player.play() {
                logger.error("Music playback failed to start")
            }
        } else if !shouldPlay, player.isPlaying {
            player.pause()
        }
    }

    private func makePlayer() -> AVAudioPlayer? {
        guard let url = Bundle.main.url(forResource: "GameMusic", withExtension: "m4a") else {
            logger.error("GameMusic.m4a is missing from the app bundle; music disabled")
            return nil
        }
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.numberOfLoops = -1
            player.volume = Self.volume
            player.prepareToPlay()
            return player
        } catch {
            logger.error("Failed to load GameMusic.m4a; music disabled: \(error, privacy: .public)")
            return nil
        }
    }

    private func observeSession() {
        let center = NotificationCenter.default
        // A phone call or alarm pauses the player; resume once it ends.
        observers.append(center.addObserver(
            forName: AVAudioSession.interruptionNotification, object: session, queue: .main
        ) { [weak self] notification in
            let rawType = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let type = rawType.flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            MainActor.assumeIsolated {
                self?.isInterrupted = type == .began
                self?.update()
            }
        })
        // Another app started or stopped its audio; re-check the silence hint.
        observers.append(center.addObserver(
            forName: AVAudioSession.silenceSecondaryAudioHintNotification, object: session, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated {
                self?.update()
            }
        })
    }
}
