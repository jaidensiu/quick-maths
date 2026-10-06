//
//  QuickMathsApp.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI
import UIKit

@main
struct QuickMathsApp: App {
    @AppStorage(ThemePreference.storageKey) private var theme = ThemePreference.system
    @Environment(\.scenePhase) private var scenePhase
    private let music = MusicPlayer()

    init() {
        Fredoka.register()
        Self.configureAppearance()
    }

    var body: some Scene {
        WindowGroup {
            NavigationView {
                StartView()
            }
            .navigationViewStyle(.stack)
            .tint(Palette.primary)
            .onAppear { theme.apply() }
            .onChange(of: theme) { newTheme in newTheme.apply() }
            // Like Android, music pauses only once the app is backgrounded, not while it's
            // briefly inactive (e.g. Control Center or the app switcher is showing).
            .onChange(of: scenePhase) { phase in
                switch phase {
                case .active: music.setForeground(true)
                case .background: music.setForeground(false)
                default: break
                }
            }
        }
    }

    /// Fonts for UIKit-drawn chrome that SwiftUI modifiers can't reach on iOS 15.
    private static func configureAppearance() {
        UINavigationBar.appearance().titleTextAttributes = [
            .font: Fredoka.uiFont(Fredoka.regular, size: 22, relativeTo: .title2),
            .foregroundColor: Palette.UIColors.onBackground,
        ]
        UISegmentedControl.appearance().setTitleTextAttributes(
            [.font: Fredoka.uiFont(Fredoka.medium, size: 14, relativeTo: .subheadline)],
            for: .normal
        )
    }
}
