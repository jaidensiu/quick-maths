//
//  FilledButtonStyle.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

/// The Android app's Material 3 filled button: a primary-colored capsule with `labelLarge` text.
/// Disabled, it uses Material's disabled colors: `onSurface` at 12% behind `onSurface` at 38%.
struct FilledButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        FilledButton(configuration: configuration)
    }
}

/// A view, unlike `ButtonStyle`, can read whether the button is enabled.
private struct FilledButton: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .font(.labelLarge)
            .foregroundStyle(isEnabled ? Palette.onPrimary : Palette.onBackground.opacity(0.38))
            .padding(.horizontal, 24)
            .frame(minHeight: 44)
            .background(Capsule().fill(isEnabled ? Palette.primary : Palette.onBackground.opacity(0.12)))
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
