//
//  StartView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

struct StartView: View {
    let onStartGame: () -> Void

    @EnvironmentObject private var modelLoader: ModelLoader
    @State private var strokes: [HandwritingStroke] = []

    var body: some View {
        ZStack {
            HandwritingCanvas(strokes: strokes) { strokes.append($0) }
            VStack(spacing: 0) {
                // Touches pass through the artwork and title so you can doodle over them.
                Group {
                    Image("SadManPlaying")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 200)
                        .foregroundStyle(Palette.onBackground)
                        .padding(.bottom, 16)
                        .accessibilityHidden(true)
                    Text("Quick Maths")
                        .font(.displayMedium)
                        .foregroundStyle(Palette.onBackground)
                }
                .allowsHitTesting(false)
                Button(startLabel, action: onStartGame)
                    .buttonStyle(FilledButtonStyle())
                    .disabled(modelLoader.status != .ready)
                    .padding(.top, 16)
                modelMessage
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.background.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                NavigationLink(destination: SettingsView()) {
                    Image(systemName: "gearshape")
                }
                .accessibilityLabel("Settings")
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    strokes.removeAll()
                } label: {
                    Text("Clear")
                        .font(.titleLarge)
                }
            }
        }
    }

    private var startLabel: String {
        switch modelLoader.status {
        case .loading: "Loading model..."
        case .offline: "No connection"
        case .ready: "Start game"
        case .error: "Model loading error"
        }
    }

    @ViewBuilder
    private var modelMessage: some View {
        switch modelLoader.status {
        case .offline:
            errorText("Connect to the internet to download the handwriting model")
        case .error:
            errorText("Couldn't download the handwriting model")
            Button("Retry", action: modelLoader.retry)
                .font(.labelLarge)
                .frame(minHeight: 44)
        case .loading, .ready:
            EmptyView()
        }
    }

    private func errorText(_ text: String) -> some View {
        Text(text)
            .font(.bodyMedium)
            .foregroundStyle(Palette.error)
            .multilineTextAlignment(.center)
            .padding(.top, 16)
            .padding(.horizontal, 16)
            .allowsHitTesting(false)
    }
}
