//
//  CountdownView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import SwiftUI

/// Counts 3, 2, 1 before a game, like Android's `CountdownScreen`: each number ticks, then grows
/// and fades out over a second.
struct CountdownView: View {
    let onFinished: () -> Void

    @EnvironmentObject private var sounds: SoundManager
    @State private var count = Self.start

    private static let start = 3
    private static let countDurationNanos: UInt64 = 1_000_000_000

    var body: some View {
        CountdownNumber(value: count)
            // A fresh view per number restarts its animation.
            .id(count)
            .transition(.identity)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Palette.background.ignoresSafeArea())
            .task { await run() }
    }

    private func run() async {
        // Resumes from the current number if the task restarts instead of starting over at 3.
        for value in stride(from: count, through: 1, by: -1) {
            count = value
            sounds.playTick()
            do {
                try await Task.sleep(nanoseconds: Self.countDurationNanos)
            } catch {
                return // Cancelled: the view went away, so don't navigate.
            }
        }
        onFinished()
    }
}

private struct CountdownNumber: View {
    let value: Int

    @State private var progress: CGFloat = 0

    var body: some View {
        Text("\(value)")
            .font(.displayLarge)
            .foregroundStyle(Palette.onBackground)
            .scaleEffect(1 + 0.5 * progress)
            .opacity(1 - progress)
            .onAppear {
                // Compose's default `tween` easing, FastOutSlowIn.
                withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 1)) {
                    progress = 1
                }
            }
    }
}
