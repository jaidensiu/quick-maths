//
//  ModelLoader.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Combine
import OSLog

enum ModelStatus {
    case loading
    case offline
    case ready
    case error
}

/// Gets the handwriting model ready at launch and gates the start button on it, like the model
/// handling in Android's `StartViewModel`. Shared through the environment so the game uses the
/// same prepared `recognizer`.
final class ModelLoader: ObservableObject {
    @Published private(set) var status = ModelStatus.loading

    let recognizer = NumberRecognizer()

    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "ModelLoader")
    private let networkMonitor = NetworkMonitor()
    /// `nil` until the first network update, which kicks off the first preparation.
    private var isOnline: Bool?

    init() {
        networkMonitor.start { [weak self] isOnline in
            self?.onNetworkChanged(isOnline: isOnline)
        }
    }

    func retry() {
        if status == .error {
            prepare()
        }
    }

    private func onNetworkChanged(isOnline: Bool) {
        let isFirstUpdate = self.isOnline == nil
        self.isOnline = isOnline
        if isFirstUpdate {
            prepare()
        } else if isOnline, status == .offline {
            prepare()
        } else if !isOnline, status == .loading, !recognizer.isModelDownloaded {
            // ML Kit's download waits silently when the network drops, so surface it ourselves.
            // The download keeps going and still moves the status on if it finishes.
            status = .offline
        }
    }

    private func prepare() {
        if !recognizer.isModelDownloaded, isOnline == false {
            status = .offline
            return
        }
        status = .loading
        Task {
            do {
                // Concurrent calls share one download, so reconnecting mid-download doesn't
                // start a second one.
                try await recognizer.prepare()
                status = .ready
            } catch {
                let isOnline = isOnline ?? false
                logger.warning("""
                    Handwriting model preparation failed (online=\(isOnline, privacy: .public)): \
                    \(error.localizedDescription, privacy: .public)
                    """)
                status = isOnline ? .error : .offline
            }
        }
    }
}
