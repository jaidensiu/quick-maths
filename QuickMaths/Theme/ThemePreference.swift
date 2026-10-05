//
//  ThemePreference.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import UIKit

enum ThemePreference: String, CaseIterable, Identifiable {
    case light
    case dark
    case system

    /// `UserDefaults` key. An unknown stored value falls back to the `@AppStorage` default (`.system`).
    static let storageKey = "theme_preference"

    var id: Self { self }

    var label: String {
        switch self {
        case .light: "Light"
        case .dark: "Dark"
        case .system: "System"
        }
    }

    /// Overrides the light/dark style of every window, so the palette, system controls,
    /// status bar, and sheets all follow the preference. `.system` clears the override.
    /// Also sets the window tint, which UIKit-drawn chrome (e.g. the back button) reads.
    func apply() {
        let style: UIUserInterfaceStyle = switch self {
        case .light: .light
        case .dark: .dark
        case .system: .unspecified
        }
        let tint = UIColor(named: "AccentColor")
        for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
            for window in scene.windows {
                window.overrideUserInterfaceStyle = style
                window.tintColor = tint
            }
        }
    }
}
