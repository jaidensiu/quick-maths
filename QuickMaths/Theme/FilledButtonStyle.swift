//
//  FilledButtonStyle.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

/// The Android app's Material 3 filled button: a primary-colored capsule with `labelLarge` text.
struct FilledButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.labelLarge)
            .foregroundStyle(Palette.onPrimary)
            .padding(.horizontal, 24)
            .frame(minHeight: 44)
            .background(Capsule().fill(Palette.primary))
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
