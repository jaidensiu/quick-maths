//
//  BestTime.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Foundation

/// The fastest finished game, like Android's `BestTimeRepository`. Views read it live with
/// `@AppStorage(BestTime.storageKey)`; unset until the first game is finished.
enum BestTime {
    static let storageKey = "best_time_ms"

    /// Stores `timeMs` if it beats the current best.
    static func record(_ timeMs: Int, in defaults: UserDefaults = .standard) {
        let current = defaults.object(forKey: storageKey) as? Int
        if current.map({ timeMs < $0 }) ?? true {
            defaults.set(timeMs, forKey: storageKey)
        }
    }

    /// Android's `Util.formatElapsedTime`: `S.cc s` under a minute and `M:SS.cc` above. Rounds to
    /// centiseconds first so the carry into minutes is exact (no `60.00s` or `1:60.00`).
    static func format(_ timeMs: Int) -> String {
        let totalCentis = (max(timeMs, 0) + 5) / 10
        let minutes = totalCentis / 6_000
        let seconds = totalCentis % 6_000 / 100
        let centis = totalCentis % 100
        return minutes > 0
            ? String(format: "%d:%02d.%02d", minutes, seconds, centis)
            : String(format: "%d.%02ds", seconds, centis)
    }
}
