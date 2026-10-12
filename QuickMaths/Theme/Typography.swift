//
//  Typography.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import CoreText
import OSLog
import SwiftUI
import UIKit

/// The Android app's Material 3 type scale in Fredoka (`ui/theme/Type.kt`).
/// Each style scales with Dynamic Type relative to the closest iOS text style.
extension Font {
    static let displayLarge = Font.custom(Fredoka.regular, size: 57, relativeTo: .largeTitle)
    static let displayMedium = Font.custom(Fredoka.regular, size: 45, relativeTo: .largeTitle)
    static let headlineLarge = Font.custom(Fredoka.regular, size: 32, relativeTo: .title)
    static let titleLarge = Font.custom(Fredoka.regular, size: 22, relativeTo: .title2)
    static let titleSmall = Font.custom(Fredoka.medium, size: 14, relativeTo: .subheadline)
    static let bodyMedium = Font.custom(Fredoka.regular, size: 14, relativeTo: .subheadline)
    static let labelLarge = Font.custom(Fredoka.medium, size: 14, relativeTo: .subheadline)
}

enum Fredoka {
    static let regular = "Fredoka-Regular"
    static let medium = "Fredoka-Medium"

    private static let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "Fonts")

    /// Registers the bundled variable font. Must run before any view or UIKit appearance uses it;
    /// on failure, text falls back to the system font, so the cause is logged rather than hidden.
    static func register() {
        guard let url = Bundle.main.url(forResource: "Fredoka", withExtension: "ttf") else {
            logger.error("Fredoka.ttf is missing from the app bundle; using the system font")
            return
        }
        var error: Unmanaged<CFError>?
        if !CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error) {
            let reason = error?.takeRetainedValue().localizedDescription ?? "unknown error"
            logger.error("Failed to register Fredoka: \(reason, privacy: .public); using the system font")
        }
    }

    /// A UIKit font for appearance proxies, scaled with Dynamic Type like the SwiftUI styles above.
    static func uiFont(_ name: String, size: CGFloat, relativeTo style: UIFont.TextStyle) -> UIFont {
        let font = UIFont(name: name, size: size) ?? .systemFont(ofSize: size)
        return UIFontMetrics(forTextStyle: style).scaledFont(for: font)
    }
}
