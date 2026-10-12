//
//  NumberRecognizer.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import CoreGraphics
import Foundation
import MLKitCommon
import MLKitDigitalInkRecognition

/// Reads a handwritten number with ML Kit Digital Ink, the same recognizer and en-US model as
/// Android's `NumberRecognizer`. The model is downloaded once by `prepare()`.
final class NumberRecognizer {
    enum Failure: LocalizedError {
        case unsupportedLanguage(String)
        case downloadFailed(underlying: Error?)
        case downloadTimedOut(seconds: UInt64)
        case notPrepared
        case recognitionFailed(underlying: Error?)

        var errorDescription: String? {
            switch self {
            case .unsupportedLanguage(let tag):
                "ML Kit has no handwriting model for \(tag)"
            case .downloadFailed(let underlying):
                "Model download failed: \(underlying?.localizedDescription ?? "no error given")"
            case .downloadTimedOut(let seconds):
                "Model download didn't finish within \(seconds)s"
            case .notPrepared:
                "Recognition was requested before the model was prepared"
            case .recognitionFailed(let underlying):
                "Recognition failed: \(underlying?.localizedDescription ?? "no result or error given")"
            }
        }
    }

    private static let languageTag = "en-US"
    /// ML Kit's download never fails on its own while offline (it waits for a connection), so
    /// bound it; `ModelLoader` also reports offline as soon as the network drops.
    private static let downloadTimeoutSeconds: UInt64 = 120
    private static let numberPattern = #"^-?[0-9]+([.,][0-9]+)?$"#
    private static let digitLookalikes: [Character: Character] = [
        "O": "0", "o": "0", "D": "0",
        "l": "1", "I": "1", "i": "1", "|": "1", "!": "1",
        "Z": "2", "z": "2",
        "S": "5", "s": "5",
        "G": "6", "b": "6",
        "B": "8",
        "g": "9", "q": "9",
    ]

    private let model = DigitalInkRecognitionModelIdentifier(forLanguageTag: languageTag)
        .map(DigitalInkRecognitionModel.init(modelIdentifier:))
    private let modelManager = ModelManager.modelManager()
    private var recognizer: DigitalInkRecognizer?
    /// The preparation in flight, so concurrent callers share one download (Android's mutex).
    private var preparation: Task<Void, Error>?

    var isModelDownloaded: Bool {
        guard let model else { return false }
        return recognizer != nil || modelManager.isModelDownloaded(model)
    }

    /// Downloads the model if needed, then warms the recognizer up so the first answer is fast.
    /// Safe to call repeatedly; a failed attempt can be retried.
    func prepare() async throws {
        if recognizer != nil { return }
        if let preparation { return try await preparation.value }
        let preparation = Task { try await performPreparation() }
        self.preparation = preparation
        defer { self.preparation = nil }
        try await preparation.value
    }

    /// The best candidate that reads as a number (after mapping letters that look like digits),
    /// falling back to the digits in the top candidate; `""` when nothing digit-like was written.
    func recognize(strokes: [HandwritingStroke], writingArea: CGSize?) async throws -> String {
        guard let recognizer else { throw Failure.notPrepared }
        let ink = Ink(strokes: strokes.map { stroke in
            Stroke(points: stroke.points.map { point in
                StrokePoint(
                    x: Float(point.location.x),
                    y: Float(point.location.y),
                    t: Int(point.timestamp * 1000)
                )
            })
        })
        let area = writingArea.map { WritingArea(width: Float($0.width), height: Float($0.height)) }
        let context = DigitalInkRecognitionContext(preContext: "", writingArea: area)
        let candidates = try await Self.candidates(for: ink, context: context, using: recognizer)
        return candidates.lazy.compactMap(Self.asNumber).first
            ?? candidates.first.map { String($0.filter { $0.isASCII && $0.isNumber }) }
            ?? ""
    }

    private func performPreparation() async throws {
        guard let model else { throw Failure.unsupportedLanguage(Self.languageTag) }
        if !modelManager.isModelDownloaded(model) {
            try await download(model)
        }
        let recognizer = DigitalInkRecognizer.digitalInkRecognizer(
            options: DigitalInkRecognizerOptions(model: model)
        )
        let warmUp = Ink(strokes: [Stroke(points: [
            StrokePoint(x: 0, y: 0, t: 0),
            StrokePoint(x: 1, y: 1, t: 16),
        ])])
        _ = try await Self.candidates(
            for: warmUp,
            context: DigitalInkRecognitionContext(preContext: "", writingArea: nil),
            using: recognizer
        )
        self.recognizer = recognizer
    }

    /// `ModelManager` reports completion only through notifications, so wait for the one about
    /// this model, bounded by `downloadTimeoutSeconds`.
    private func download(_ model: DigitalInkRecognitionModel) async throws {
        let waiter = DownloadWaiter(model: model, timeoutSeconds: Self.downloadTimeoutSeconds)
        try await waiter.wait {
            let conditions = ModelDownloadConditions(
                allowsCellularAccess: true,
                allowsBackgroundDownloading: true
            )
            modelManager.download(model, conditions: conditions)
        }
    }

    private static func candidates(
        for ink: Ink,
        context: DigitalInkRecognitionContext,
        using recognizer: DigitalInkRecognizer
    ) async throws -> [String] {
        try await withCheckedThrowingContinuation { continuation in
            recognizer.recognize(ink: ink, context: context) { result, error in
                if let result {
                    continuation.resume(returning: result.candidates.map(\.text))
                } else {
                    continuation.resume(throwing: Failure.recognitionFailed(underlying: error))
                }
            }
        }
    }

    private static func asNumber(_ text: String) -> String? {
        let normalized = String(text.filter { !$0.isWhitespace }.map { digitLookalikes[$0] ?? $0 })
        return normalized.range(of: numberPattern, options: .regularExpression) != nil ? normalized : nil
    }
}

/// Resumes exactly once with the first of: this model's success or failure notification, or the
/// timeout. Observers are registered before the download starts so a fast finish isn't missed.
private final class DownloadWaiter {
    private let model: DigitalInkRecognitionModel
    private let timeoutSeconds: UInt64
    private var continuation: CheckedContinuation<Void, Error>?
    private var observers: [NSObjectProtocol] = []
    private var timeout: Task<Void, Never>?

    init(model: DigitalInkRecognitionModel, timeoutSeconds: UInt64) {
        self.model = model
        self.timeoutSeconds = timeoutSeconds
    }

    func wait(startingWith start: () -> Void) async throws {
        try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            let center = NotificationCenter.default
            observers.append(center.addObserver(
                forName: .mlkitModelDownloadDidSucceed, object: nil, queue: .main
            ) { [weak self] notification in
                let name = Self.modelName(in: notification)
                MainActor.assumeIsolated {
                    guard let self, name == self.model.name else { return }
                    self.finish(.success(()))
                }
            })
            observers.append(center.addObserver(
                forName: .mlkitModelDownloadDidFail, object: nil, queue: .main
            ) { [weak self] notification in
                let name = Self.modelName(in: notification)
                let error = notification.userInfo?[ModelDownloadUserInfoKey.error.rawValue] as? Error
                MainActor.assumeIsolated {
                    guard let self, name == self.model.name else { return }
                    self.finish(.failure(NumberRecognizer.Failure.downloadFailed(underlying: error)))
                }
            })
            timeout = Task { [weak self, timeoutSeconds] in
                try? await Task.sleep(nanoseconds: timeoutSeconds * 1_000_000_000)
                guard !Task.isCancelled else { return }
                self?.finish(.failure(NumberRecognizer.Failure.downloadTimedOut(seconds: timeoutSeconds)))
            }
            start()
        }
    }

    private func finish(_ result: Result<Void, Error>) {
        guard let continuation else { return }
        self.continuation = nil
        observers.forEach(NotificationCenter.default.removeObserver)
        observers = []
        timeout?.cancel()
        continuation.resume(with: result)
    }

    private nonisolated static func modelName(in notification: Notification) -> String? {
        let model = notification.userInfo?[ModelDownloadUserInfoKey.remoteModel.rawValue] as? RemoteModel
        return model?.name
    }
}
