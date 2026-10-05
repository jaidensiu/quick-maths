//
//  StartView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

struct StartView: View {
    var body: some View {
        VStack(spacing: 0) {
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
            // Not wired up yet: the countdown and game screens come later.
            Button("Start game") {}
                .buttonStyle(FilledButtonStyle())
                .padding(.top, 16)
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
                // Not wired up yet: clears the handwriting canvas once drawing exists.
                Button {} label: {
                    Text("Clear")
                        .font(.titleLarge)
                }
            }
        }
    }
}
