//
//  SettingsView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI

struct SettingsView: View {
    @AppStorage(ThemePreference.storageKey) private var theme = ThemePreference.system

    var body: some View {
        VStack(spacing: 16) {
            Text("Theme")
                .font(.titleSmall)
                .foregroundStyle(Palette.onBackground)
            Picker("Theme", selection: $theme) {
                ForEach(ThemePreference.allCases) { preference in
                    Text(preference.label).tag(preference)
                }
            }
            .pickerStyle(.segmented)
            .fixedSize()
        }
        .padding(.top, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}
