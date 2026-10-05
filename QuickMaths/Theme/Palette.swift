//
//  Palette.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI
import UIKit

/// The Android app's monochrome ink-on-paper palette (`ui/theme/Color.kt` and `Theme.kt`).
/// Each role resolves to its light or dark value from the current trait collection, so it follows
/// whatever `ThemePreference` has applied to the window.
enum Palette {
    /// Ink / NightText. Defined in the `AccentColor` asset and applied as the app-wide tint,
    /// so system controls (back button, toolbar items) use it too.
    static let primary = Color("AccentColor")
    /// Paper / NightInk
    static let onPrimary = dynamic(light: 0xFFFFFF, dark: 0x191919)
    /// Paper / NightInk
    static let background = dynamic(light: 0xFFFFFF, dark: 0x191919)
    /// InkSoft / NightText
    static let onBackground = dynamic(light: 0x37352F, dark: 0xE6E5E3)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
