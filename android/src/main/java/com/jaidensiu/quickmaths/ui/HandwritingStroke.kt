package com.jaidensiu.quickmaths.ui

import androidx.compose.ui.graphics.Path
import com.jaidensiu.quickmaths.domain.HandwritingPoint

data class HandwritingStroke(
    val path: Path,
    val points: List<HandwritingPoint>,
)
