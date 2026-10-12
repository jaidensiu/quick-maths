//
//  StartView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

struct StartView: View {
    let onStartGame: () -> Void

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
                Button("Start game", action: onStartGame)
                    .buttonStyle(FilledButtonStyle())
                    .padding(.top, 16)
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
}
