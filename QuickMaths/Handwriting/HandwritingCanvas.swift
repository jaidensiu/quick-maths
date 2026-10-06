//
//  HandwritingCanvas.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import SwiftUI
import UIKit

/// Low-latency handwriting surface, the iOS counterpart of Android's `HandwritingCanvas`.
///
/// The owner holds the finished strokes: the canvas reports each one through `onStrokeFinished`
/// and draws whatever is in `strokes`, so clearing is just emptying the owner's list.
struct HandwritingCanvas: UIViewRepresentable {
    let strokes: [HandwritingStroke]
    let onStrokeFinished: (HandwritingStroke) -> Void
    /// Android: `strokeWidth = 4.dp`.
    var strokeWidth: CGFloat = 4
    /// Android: `MaterialTheme.colorScheme.onSurface` (InkSoft / NightText).
    var strokeColor: UIColor = Palette.UIColors.onBackground

    func makeUIView(context: Context) -> HandwritingCanvasView {
        HandwritingCanvasView()
    }

    func updateUIView(_ view: HandwritingCanvasView, context: Context) {
        view.strokeWidth = strokeWidth
        view.strokeColor = strokeColor
        view.onStrokeFinished = onStrokeFinished
        view.strokes = strokes
    }
}

/// Draws finished strokes in one shape layer and the stroke in progress in another, so each touch
/// only rebuilds the current stroke's path. Uses every coalesced touch sample (Android's historical
/// points) and extends the stroke in progress with predicted touches, like Android's
/// `MotionEventPredictor`, to cut perceived latency.
final class HandwritingCanvasView: UIView {
    var onStrokeFinished: (HandwritingStroke) -> Void = { _ in }

    var strokes: [HandwritingStroke] = [] {
        didSet {
            guard strokes != oldValue else { return }
            withoutAnimation { finishedLayer.path = Self.combinedPath(of: strokes) }
        }
    }

    var strokeWidth: CGFloat = 4 {
        didSet { applyStyle() }
    }

    var strokeColor: UIColor = .label {
        didSet { applyStyle() }
    }

    private let finishedLayer = CAShapeLayer()
    private let activeLayer = CAShapeLayer()
    private var activeTouch: UITouch?
    private var activePoints: [HandwritingPoint] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        isOpaque = false
        clipsToBounds = true
        for shapeLayer in [finishedLayer, activeLayer] {
            shapeLayer.fillColor = nil
            shapeLayer.lineCap = .round
            shapeLayer.lineJoin = .round
            layer.addSublayer(shapeLayer)
        }
        applyStyle()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        withoutAnimation {
            finishedLayer.frame = bounds
            activeLayer.frame = bounds
        }
    }

    /// Shape layers hold a resolved `CGColor`, so re-resolve the ink when light/dark changes.
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) {
            applyStyle()
        }
    }

    // MARK: - Touches (single touch; extra fingers are ignored, like Android's single pointer)

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard activeTouch == nil, let touch = touches.first else { return }
        activeTouch = touch
        activePoints = [point(from: touch)]
        renderActiveStroke(predicted: [])
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        let samples = event?.coalescedTouches(for: touch) ?? [touch]
        activePoints.append(contentsOf: samples.map(point(from:)))
        let predicted = event?.predictedTouches(for: touch) ?? []
        renderActiveStroke(predicted: predicted.map(point(from:)))
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        activePoints.append(point(from: touch))
        let stroke = HandwritingStroke(points: activePoints)
        resetActiveStroke()
        // Show it right away; the owner's updated `strokes` list redraws the same path.
        withoutAnimation {
            let path = finishedLayer.path?.mutableCopy() ?? CGMutablePath()
            path.addPath(stroke.points.smoothedPath())
            finishedLayer.path = path
        }
        onStrokeFinished(stroke)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        resetActiveStroke()
    }

    // MARK: - Drawing

    private func renderActiveStroke(predicted: [HandwritingPoint]) {
        withoutAnimation {
            activeLayer.path = (activePoints + predicted).smoothedPath()
        }
    }

    private func resetActiveStroke() {
        activeTouch = nil
        activePoints = []
        withoutAnimation { activeLayer.path = nil }
    }

    private func applyStyle() {
        let color = strokeColor.resolvedColor(with: traitCollection).cgColor
        withoutAnimation {
            for shapeLayer in [finishedLayer, activeLayer] {
                shapeLayer.strokeColor = color
                shapeLayer.lineWidth = strokeWidth
            }
        }
    }

    private func point(from touch: UITouch) -> HandwritingPoint {
        HandwritingPoint(location: touch.location(in: self), timestamp: touch.timestamp)
    }

    private static func combinedPath(of strokes: [HandwritingStroke]) -> CGPath {
        let path = CGMutablePath()
        for stroke in strokes {
            path.addPath(stroke.points.smoothedPath())
        }
        return path
    }

    /// Standalone layers animate property changes by default; ink must update instantly.
    private func withoutAnimation(_ changes: () -> Void) {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        changes()
        CATransaction.commit()
    }
}
