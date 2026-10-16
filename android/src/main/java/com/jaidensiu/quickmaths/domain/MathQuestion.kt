package com.jaidensiu.quickmaths.domain

import kotlin.random.Random

data class MathQuestion(
    val left: Int,
    val right: Int,
    val operation: Operation,
) {
    val answer = when (operation) {
        Operation.ADDITION -> left + right
        Operation.SUBTRACTION -> left - right
        Operation.MULTIPLICATION -> left * right
        Operation.DIVISION -> left / right
    }
    val text: String = "$left ${operation.symbol} $right ="

    companion object {
        fun random(): MathQuestion {
            val random = Random.Default

            return when (Operation.entries.random(random)) {
                Operation.ADDITION -> {
                    MathQuestion(
                        left = random.nextInt(from = 1, until = 10),
                        right = random.nextInt(from = 1, until = 10),
                        operation = Operation.ADDITION,
                    )
                }

                Operation.SUBTRACTION -> {
                    val a = random.nextInt(from = 1, until = 10)
                    val b = random.nextInt(from = 1, until = 10)
                    MathQuestion(
                        left = maxOf(a, b),
                        right = minOf(a, b),
                        operation = Operation.SUBTRACTION,
                    )
                }

                Operation.MULTIPLICATION -> {
                    MathQuestion(
                        left = random.nextInt(from = 1, until = 10),
                        right = random.nextInt(from = 1, until = 10),
                        operation = Operation.MULTIPLICATION,
                    )
                }

                Operation.DIVISION -> {
                    val divisor = random.nextInt(from = 1, until = 10)
                    val quotient = random.nextInt(from = 1, until = 10)
                    MathQuestion(
                        left = divisor * quotient,
                        right = divisor,
                        operation = Operation.DIVISION,
                    )
                }
            }
        }
    }
}
