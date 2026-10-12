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
    @State private var isPlaying = false

    /// Android: `NAV_TRANSITION_MS = 300` with the default `tween` easing, FastOutSlowIn.
    private static let transition = Animation.timingCurve(0.4, 0, 0.2, 1, duration: 0.3)

    var body: some View {
        ZStack {
            NavigationView {
                StartView { setPlaying(true) }
            }
            .navigationViewStyle(.stack)
            .accessibilityHidden(isPlaying)
            if isPlaying {
                // Not wired to a game yet: the countdown returns here until the game screen exists.
                CountdownView { setPlaying(false) }
                    .transition(.opacity)
                    // Keep it above the start screen while it fades out.
                    .zIndex(1)
            }
        }
    }

    private func setPlaying(_ isPlaying: Bool) {
        withAnimation(Self.transition) { self.isPlaying = isPlaying }
    }
}
