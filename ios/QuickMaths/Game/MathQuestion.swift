//
//  MathQuestion.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

enum MathOperation: CaseIterable {
    case addition
    case subtraction
    case multiplication
    case division

    var symbol: String {
        switch self {
        case .addition: "+"
        case .subtraction: "−"
        case .multiplication: "×"
        case .division: "÷"
        }
    }
}

/// A single-digit arithmetic question, like Android's `MathQuestion`. Every answer is a whole,
/// non-negative number: subtraction never goes below zero and division always divides evenly.
struct MathQuestion: Equatable {
    let left: Int
    let right: Int
    let operation: MathOperation

    var answer: Int {
        switch operation {
        case .addition: left + right
        case .subtraction: left - right
        case .multiplication: left * right
        case .division: left / right
        }
    }

    var text: String { "\(left) \(operation.symbol) \(right) =" }

    static func random() -> MathQuestion {
        let digit = { Int.random(in: 1...9) }
        switch MathOperation.allCases.randomElement() ?? .addition {
        case .addition:
            return MathQuestion(left: digit(), right: digit(), operation: .addition)
        case .subtraction:
            let a = digit()
            let b = digit()
            return MathQuestion(left: max(a, b), right: min(a, b), operation: .subtraction)
        case .multiplication:
            return MathQuestion(left: digit(), right: digit(), operation: .multiplication)
        case .division:
            let divisor = digit()
            let quotient = digit()
            return MathQuestion(left: divisor * quotient, right: divisor, operation: .division)
        }
    }
}
