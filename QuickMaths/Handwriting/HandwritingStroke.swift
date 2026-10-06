//
//  HandwritingStroke.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-05.
//

import CoreGraphics
import Foundation

/// One sampled touch position, like Android's `HandwritingPoint`.
struct HandwritingPoint: Equatable {
    let location: CGPoint
    /// Seconds since boot, from `UITouch.timestamp`.
    let timestamp: TimeInterval
}

/// A finished stroke, like Android's `HandwritingStroke`. Keeps the raw points (with timing) for
/// handwriting recognition; the rendered shape comes from `smoothedPath()`.
struct HandwritingStroke: Identifiable, Equatable {
    let id = UUID()
    let points: [HandwritingPoint]
}

extension Array where Element == HandwritingPoint {
    /// Android's `toSmoothedPath`: a quadratic curve through the midpoints between consecutive
    /// points, using each point as the control point, then a line to the final point. A single
    /// point becomes a zero-length line, which the round cap draws as a dot.
    func smoothedPath() -> CGPath {
        let path = CGMutablePath()
        guard let first else { return path }
        path.move(to: first.location)
        var last = first.location
        for point in dropFirst() {
            let midpoint = CGPoint(x: (last.x + point.location.x) / 2, y: (last.y + point.location.y) / 2)
            path.addQuadCurve(to: midpoint, control: last)
            last = point.location
        }
        path.addLine(to: last)
        return path
    }
}
