package com.memoryuniverse.ai.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val lineCount: Int,
    val confidenceScore: Float
)

/**
 * On-Device Optical Character Recognition (OCR) Engine using bundled ML Kit Text Recognition.
 * Operates strictly offline with zero remote network calls.
 */
class LocalOcrExtractor(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text asynchronously from a local content Uri.
     */
    suspend fun extractTextFromUri(uri: Uri): OcrResult = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val cleanText = visionText.text.trim()
                    val lines = visionText.textBlocks.sumOf { it.lines.size }
                    continuation.resume(
                        OcrResult(
                            fullText = cleanText,
                            lineCount = lines,
                            confidenceScore = if (cleanText.isNotEmpty()) 1.0f else 0.0f
                        )
                    )
                }
                .addOnFailureListener {
                    // Gracefully fallback to empty if image has no text or is unreadable
                    continuation.resume(OcrResult(fullText = "", lineCount = 0, confidenceScore = 0f))
                }
        } catch (e: Exception) {
            continuation.resume(OcrResult(fullText = "", lineCount = 0, confidenceScore = 0f))
        }
    }

    /**
     * Extracts text asynchronously from an in-memory Bitmap.
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val cleanText = visionText.text.trim()
                    val lines = visionText.textBlocks.sumOf { it.lines.size }
                    continuation.resume(
                        OcrResult(
                            fullText = cleanText,
                            lineCount = lines,
                            confidenceScore = if (cleanText.isNotEmpty()) 1.0f else 0.0f
                        )
                    )
                }
                .addOnFailureListener {
                    continuation.resume(OcrResult(fullText = "", lineCount = 0, confidenceScore = 0f))
                }
        } catch (e: Exception) {
            continuation.resume(OcrResult(fullText = "", lineCount = 0, confidenceScore = 0f))
        }
    }

    fun close() {
        recognizer.close()
    }
}
