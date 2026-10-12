//
//  GameModel.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Combine
import CoreGraphics
import Foundation
import OSLog

/// One game's state and rules, like Android's `GameViewModel` and `GameState`: answer
/// `totalQuestions` questions by handwriting as fast as possible. Time spent paused doesn't count.
final class GameModel: ObservableObject {
    static let totalQuestions = 20

    @Published private(set) var question = MathQuestion.random()
    @Published private(set) var questionNumber = 1
    /// What the recognizer read; `"?"` when it read nothing number-like.
    @Published private(set) var recognizedText = ""
    /// The current question's strokes; the source of truth for both drawing and recognition.
    @Published private(set) var strokes: [HandwritingStroke] = []
    @Published private(set) var isPaused = false
    /// Set once the last question is answered.
    @Published private(set) var finishedTimeMs: Int?

    private let recognizer: NumberRecognizer
    private let sounds: SoundManager
    private let logger = Logger(subsystem: "com.jaidensiu.quickmaths", category: "Game")
    private let startTime = ProcessInfo.processInfo.systemUptime
    private var totalPausedTime: TimeInterval = 0
    private var pauseStartedAt: TimeInterval = 0
    private var writingArea: CGSize?
    private var recognition: Task<Void, Never>?
    private var pencilIdle: Task<Void, Never>?

    /// Android: below this the pen is resting, so it doesn't count as movement.
    private static let pencilMinSpeedPxPerMs: CGFloat = 0.01
    /// Android: `PENCIL_IDLE_TIMEOUT_MS`; a pen still for this long goes quiet.
    private static let pencilIdleNanos: UInt64 = 50_000_000

    init(recognizer: NumberRecognizer, sounds: SoundManager) {
        self.recognizer = recognizer
        self.sounds = sounds
    }

    func onCanvasSizeChanged(_ size: CGSize) {
        writingArea = size
    }

    func onStrokeStarted() {
        guard !isPaused, finishedTimeMs == nil else { return }
        // The answer isn't finished yet; the next read includes this stroke.
        recognition?.cancel()
        sounds.startPencil()
        scheduleIdleMute()
    }

    func onStrokeMoved(speedPxPerMs speed: CGFloat) {
        guard speed >= Self.pencilMinSpeedPxPerMs else { return }
        sounds.updatePencilSpeed(pxPerMs: speed)
        scheduleIdleMute()
    }

    func onStrokeCancelled() {
        stopPencil()
    }

    func onStrokeFinished(_ stroke: HandwritingStroke) {
        stopPencil()
        guard !isPaused, finishedTimeMs == nil else { return }
        strokes.append(stroke)
        recognize(strokes)
    }

    func clear() {
        recognition?.cancel()
        strokes = []
        recognizedText = ""
    }

    func pause() {
        guard !isPaused, finishedTimeMs == nil else { return }
        pauseStartedAt = ProcessInfo.processInfo.systemUptime
        recognition?.cancel()
        stopPencil()
        isPaused = true
    }

    func resume() {
        guard isPaused else { return }
        totalPausedTime += ProcessInfo.processInfo.systemUptime - pauseStartedAt
        isPaused = false
        // A stroke may have been cut off by the pause; re-read what's on the canvas.
        if !strokes.isEmpty {
            recognize(strokes)
        }
    }

    private func scheduleIdleMute() {
        pencilIdle?.cancel()
        pencilIdle = Task { [sounds] in
            try? await Task.sleep(nanoseconds: Self.pencilIdleNanos)
            guard !Task.isCancelled else { return }
            sounds.mutePencil()
        }
    }

    private func stopPencil() {
        pencilIdle?.cancel()
        sounds.stopPencil()
    }

    /// Re-reads every stroke written so far, superseding any read still in flight.
    private func recognize(_ strokes: [HandwritingStroke]) {
        recognition?.cancel()
        recognition = Task { [recognizer, writingArea] in
            let text: String
            do {
                text = try await recognizer.recognize(strokes: strokes, writingArea: writingArea)
            } catch {
                guard !Task.isCancelled else { return }
                logger.warning("""
                    Handwriting recognition failed for \(strokes.count, privacy: .public) stroke(s): \
                    \(error.localizedDescription, privacy: .public)
                    """)
                recognizedText = "?"
                return
            }
            // A newer stroke, clear, or pause arrived while this one was being read.
            guard !Task.isCancelled else { return }
            recognizedText = text.isEmpty ? "?" : text
            checkAnswer(text)
        }
    }

    private func checkAnswer(_ text: String) {
        guard text.trimmingCharacters(in: .whitespaces) == String(question.answer) else { return }
        strokes = []
        recognizedText = ""
        if questionNumber >= Self.totalQuestions {
            let elapsed = ProcessInfo.processInfo.systemUptime - startTime - totalPausedTime
            let timeMs = Int((elapsed * 1000).rounded())
            BestTime.record(timeMs)
            finishedTimeMs = timeMs
        } else {
            question = MathQuestion.random()
            questionNumber += 1
            sounds.playCorrect()
        }
    }
}
