package com.jaidensiu.quickmaths.ui

import com.jaidensiu.quickmaths.domain.MathQuestion

const val TOTAL_QUESTIONS = 20

data class GameState(
    val recognizedText: String = "",
    val question: MathQuestion? = null,
    val questionNumber: Int = 1,
    val totalQuestions: Int = TOTAL_QUESTIONS,
    val isFinished: Boolean = false,
    val isPaused: Boolean = false,
    /** False while the vehicle is not confirmed parked; the pause menu's Resume stays disabled. */
    val canResume: Boolean = true,
    val elapsedTimeMs: Long = 0L,
    val canvasClearKey: Int = 0,
    /** Completed strokes for the current question; the single source of truth for drawing and recognition. */
    val strokes: List<HandwritingStroke> = emptyList(),
)
