//
//  RootView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import SwiftUI

/// Hosts the screens, like Android's `QuickMathsApp` nav host. Settings is a navigation push, but
/// the game flow fades in over the start screen like Android's fade transitions, and covers the
/// navigation bar so there's no back button or swipe-back mid-game (Android's empty `BackHandler`).
struct RootView: View {
    private enum Screen {
        case countdown
        case game
    }

    @EnvironmentObject private var sounds: SoundManager
    @EnvironmentObject private var modelLoader: ModelLoader
    /// The game-flow screen covering the start screen, if any.
    @State private var screen: Screen?

    /// Android: `NAV_TRANSITION_MS = 300` with the default `tween` easing, FastOutSlowIn.
    private static let transition = Animation.timingCurve(0.4, 0, 0.2, 1, duration: 0.3)

    var body: some View {
        ZStack {
            NavigationView {
                StartView { show(.countdown) }
            }
            .navigationViewStyle(.stack)
            .accessibilityHidden(screen != nil)
            // Unlike Android's nav host, the start screen stays rendered underneath, so hide it
            // behind a backdrop that stays up while the countdown cross-fades into the game.
            if screen != nil {
                Palette.background
                    .ignoresSafeArea()
                    .transition(.opacity)
                    .zIndex(1)
            }
            switch screen {
            case .countdown:
                CountdownView { show(.game) }
                    .transition(.opacity)
                    // Keep each screen above the one it replaces while they cross-fade.
                    .zIndex(2)
            case .game:
                // Not wired to results yet: a finished game returns here until that screen exists.
                GameView(
                    recognizer: modelLoader.recognizer,
                    sounds: sounds,
                    onFinished: { _ in show(nil) },
                    onExit: { show(nil) }
                )
                .transition(.opacity)
                .zIndex(3)
            case nil:
                EmptyView()
            }
        }
        // Music stays off during the countdown and the game, like Android's `isMusicAllowed`.
        .onChange(of: screen) { screen in
            sounds.setMusicAllowed(screen == nil)
        }
    }

    private func show(_ screen: Screen?) {
        withAnimation(Self.transition) { self.screen = screen }
    }
}
