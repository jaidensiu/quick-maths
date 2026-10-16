//
//  GameView.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import SwiftUI

/// Like Android's `GameScreen`: the question, what the recognizer read, and a canvas to write the
/// answer on. Wide windows put the question beside the canvas. Backgrounding the app pauses.
struct GameView: View {
    let onFinished: (_ elapsedTimeMs: Int) -> Void
    let onExit: () -> Void

    @StateObject private var game: GameModel
    @Environment(\.scenePhase) private var scenePhase

    /// Android's expanded window width class (840dp); a point is close enough to a dp.
    private static let expandedWidth: CGFloat = 840

    init(
        recognizer: NumberRecognizer,
        sounds: SoundManager,
        onFinished: @escaping (_ elapsedTimeMs: Int) -> Void,
        onExit: @escaping () -> Void
    ) {
        _game = StateObject(wrappedValue: GameModel(recognizer: recognizer, sounds: sounds))
        self.onFinished = onFinished
        self.onExit = onExit
    }

    var body: some View {
        Group {
            if game.finishedTimeMs != nil {
                Color.clear
            } else if game.isPaused {
                pausedContent
            } else {
                playingContent
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.background.ignoresSafeArea())
        // Away-time shouldn't count, like Android pausing on `ON_STOP`.
        .onChange(of: scenePhase) { phase in
            if phase == .background {
                game.pause()
            }
        }
        .onChange(of: game.finishedTimeMs) { timeMs in
            if let timeMs {
                onFinished(timeMs)
            }
        }
    }

    private var pausedContent: some View {
        VStack(spacing: 0) {
            Text("Paused")
                .font(.displayMedium)
                .foregroundStyle(Palette.onBackground)
            Button("Resume", action: game.resume)
                .buttonStyle(FilledButtonStyle())
                .padding(.top, 16)
            Button("Exit", action: onExit)
                .font(.labelLarge)
                .frame(minHeight: 44)
        }
    }

    private var playingContent: some View {
        GeometryReader { proxy in
            VStack(spacing: 0) {
                header
                if proxy.size.width >= Self.expandedWidth {
                    HStack(spacing: 0) {
                        VStack(spacing: 0) {
                            questionText
                            HStack(spacing: 8) {
                                answerLabel
                                recognizedText
                            }
                            clearButton
                                .padding(.top, 8)
                        }
                        .frame(width: proxy.size.width * 2 / 5)
                        .frame(maxHeight: .infinity)
                        canvas
                    }
                } else {
                    questionText
                    HStack(spacing: 8) {
                        answerLabel
                        recognizedText
                        Spacer(minLength: 0)
                        clearButton
                    }
                    .padding(.horizontal, 8)
                    canvas
                }
            }
        }
    }

    private var header: some View {
        ZStack {
            Text("\(game.questionNumber) / \(GameModel.totalQuestions)")
                .font(.labelLarge)
                .foregroundStyle(Palette.onBackground)
            HStack {
                Spacer()
                Button("Pause", action: game.pause)
                    .font(.labelLarge)
                    .frame(minHeight: 44)
                    .padding(.trailing, 8)
            }
        }
    }

    private var questionText: some View {
        Text(game.question.text)
            .font(.displayMedium)
            .foregroundStyle(Palette.onBackground)
    }

    private var answerLabel: some View {
        Text("Your answer:")
            .font(.titleLarge)
            .foregroundStyle(Palette.onBackground)
    }

    private var recognizedText: some View {
        // A space keeps the line's height while empty, as Compose does, so the canvas doesn't jump.
        Text(game.recognizedText.isEmpty ? " " : game.recognizedText)
            .font(.headlineLarge)
            .foregroundStyle(Palette.onBackground)
    }

    private var clearButton: some View {
        Button(action: game.clear) {
            Text("Clear")
                .font(.titleLarge)
        }
        .frame(minHeight: 44)
    }

    private var canvas: some View {
        GeometryReader { proxy in
            HandwritingCanvas(
                strokes: game.strokes,
                onStrokeFinished: game.onStrokeFinished,
                onStrokeStarted: game.onStrokeStarted,
                onStrokeMoved: { game.onStrokeMoved(speedPxPerMs: $0) },
                onStrokeCancelled: game.onStrokeCancelled
            )
                .onAppear { game.onCanvasSizeChanged(proxy.size) }
                .onChange(of: proxy.size) { size in game.onCanvasSizeChanged(size) }
        }
    }
}
