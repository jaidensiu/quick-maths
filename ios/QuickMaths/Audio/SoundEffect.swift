//
//  SoundEffect.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import AVFoundation
import OSLog

/// A short bundled clip played on demand, like one of the Android app's SFX samples.
///
/// Plays through the shared `.ambient` session `MusicPlayer` sets up, so it obeys the Ring/Silent
/// switch and mixes with the music. Loaded and prepared up front so the first play isn't delayed.
final class SoundEffect {
    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "SoundEffect")
    private let name: String
    private let player: AVAudioPlayer?

    init(resource name: String, withExtension ext: String = "caf") {
        self.name = name
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else {
            logger.error(
                "\(name, privacy: .public).\(ext, privacy: .public) is missing from the app bundle; sound disabled"
            )
            player = nil
            return
        }
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            self.player = player
        } catch {
            logger.error("Failed to load \(name, privacy: .public); sound disabled: \(error, privacy: .public)")
            player = nil
        }
    }

    /// Restarts the clip from the beginning, cutting off a play that's still going.
    func play() {
        guard let player else { return }
        player.currentTime = 0
        if !player.play() {
            // Expected while e.g. a phone call holds the audio hardware.
            logger.warning("\(self.name, privacy: .public) playback failed to start")
        }
    }
}
