//
//  ResultsView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import SwiftUI

/// Like Android's `ResultsScreen`: the finished game's time and the best time.
struct ResultsView: View {
    let elapsedTimeMs: Int
    let onPlayAgain: () -> Void
    let onBackToHome: () -> Void

    @AppStorage(BestTime.storageKey) private var storedBestTimeMs: Int?

    /// This game counts even if its record hasn't been stored yet.
    private var bestTimeMs: Int {
        min(storedBestTimeMs ?? elapsedTimeMs, elapsedTimeMs)
    }

    var body: some View {
        VStack(spacing: 0) {
            Group {
                Text("All \(GameModel.totalQuestions) solved!")
                    .font(.headlineMedium)
                Text("Time: \(BestTime.format(elapsedTimeMs))")
                    .font(.headlineMedium)
                    .padding(.top, 16)
                Text("Best time: \(BestTime.format(bestTimeMs))")
                    .font(.headlineSmall)
                    .padding(.top, 16)
            }
            .foregroundStyle(Palette.onBackground)
            .multilineTextAlignment(.center)
            Button("Play again", action: onPlayAgain)
                .buttonStyle(FilledButtonStyle())
                .padding(.top, 32)
            Button("Back to home", action: onBackToHome)
                .font(.labelLarge)
                .frame(minHeight: 44)
                .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.background.ignoresSafeArea())
    }
}
