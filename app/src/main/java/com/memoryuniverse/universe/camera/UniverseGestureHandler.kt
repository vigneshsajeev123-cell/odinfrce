package com.memoryuniverse.universe.camera

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Compose Modifier providing natural 3D Universe camera gesture handling:
 * - Pinch-to-zoom
 * - Pan across 3D universe space
 * - Tap to select memories
 * - Double-tap to focus
 */
fun Modifier.universeGestureHandler(
    cameraState: CameraState,
    onZoomChange: (Float) -> Unit,
    onPanChange: (Float, Float) -> Unit,
    onTap: (Float, Float) -> Unit,
    onDoubleTap: (Float, Float) -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            // Pinch-to-zoom: scale zoom distance logarithmically with bounds
            if (zoom != 1.0f) {
                val newZoom = (cameraState.zoomDistance / zoom).coerceIn(cameraState.minZoom, cameraState.maxZoom)
                onZoomChange(newZoom)
            }

            // Planar Pan: scale pixel delta by distance so panning speed is visually constant at any zoom level
            if (pan.x != 0f || pan.y != 0f) {
                val panSensitivity = (cameraState.zoomDistance / cameraState.viewportHeight) * 1.8f
                val deltaX = -pan.x * panSensitivity
                val deltaY = pan.y * panSensitivity
                onPanChange(deltaX, deltaY)
            }
        }
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                onTap(offset.x, offset.y)
            },
            onDoubleTap = { offset ->
                onDoubleTap(offset.x, offset.y)
            }
        )
    }
