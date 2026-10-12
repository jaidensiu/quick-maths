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
    private enum Screen: Equatable {
        case countdown
        case game
        case results(elapsedTimeMs: Int)
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
            // behind a backdrop that stays up while the game-flow screens cross-fade.
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
                GameView(
                    recognizer: modelLoader.recognizer,
                    sounds: sounds,
                    onFinished: { show(.results(elapsedTimeMs: $0)) },
                    onExit: { show(nil) }
                )
                .transition(.opacity)
                .zIndex(3)
            case .results(let elapsedTimeMs):
                ResultsView(
                    elapsedTimeMs: elapsedTimeMs,
                    onPlayAgain: { show(.countdown) },
                    onBackToHome: { show(nil) }
                )
                .transition(.opacity)
                .zIndex(4)
            case nil:
                EmptyView()
            }
        }
        // Music stays off during the countdown and the game, like Android's `isMusicAllowed`.
        .onChange(of: screen) { screen in
            sounds.setMusicAllowed(screen != .countdown && screen != .game)
        }
    }

    private func show(_ screen: Screen?) {
        withAnimation(Self.transition) { self.screen = screen }
    }
}
