package com.memoryuniverse.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class RawPhotoRecord(
    val mediaStoreId: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val focalLength: Double? = null,
    val iso: Int? = null,
    val exposureTime: Double? = null
)

class MediaStoreFetcher(private val contentResolver: ContentResolver) {

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.ORIENTATION
    )

    /**
     * Scans local photos from MediaStore asynchronously.
     * Does NOT duplicate files into app storage.
     */
    suspend fun fetchLocalPhotos(limit: Int = 1000): List<RawPhotoRecord> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<RawPhotoRecord>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "photo_$id"
                    val mimeType = cursor.getString(mimeColumn) ?: "image/jpeg"
                    val size = cursor.getLong(sizeColumn)
                    var dateTaken = cursor.getLong(dateTakenColumn)
                    if (dateTaken <= 0) {
                        dateTaken = cursor.getLong(dateAddedColumn) * 1000L
                    }
                    if (dateTaken <= 0) {
                        dateTaken = System.currentTimeMillis()
                    }

                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val orientation = cursor.getInt(orientationColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Extract EXIF data (GPS and camera info) if accessible
                    val exifInfo = extractExifInfo(contentUri)

                    photos.add(
                        RawPhotoRecord(
                            mediaStoreId = id,
                            uri = contentUri,
                            displayName = name,
                            mimeType = mimeType,
                            sizeBytes = size,
                            timestamp = dateTaken,
                            width = width,
                            height = height,
                            orientation = orientation,
                            latitude = exifInfo.latitude,
                            longitude = exifInfo.longitude,
                            cameraMake = exifInfo.cameraMake,
                            cameraModel = exifInfo.cameraModel,
                            focalLength = exifInfo.focalLength,
                            iso = exifInfo.iso,
                            exposureTime = exifInfo.exposureTime
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        photos
    }

    private data class ExifInfo(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val cameraMake: String? = null,
        val cameraModel: String? = null,
        val focalLength: Double? = null,
        val iso: Int? = null,
        val exposureTime: Double? = null
    )

    private fun extractExifInfo(uri: Uri): ExifInfo {
        var inputStream: InputStream? = null
        return try {
            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    MediaStore.setRequireOriginal(uri)
                } catch (e: Exception) {
                    uri
                }
            } else {
                uri
            }

            inputStream = contentResolver.openInputStream(targetUri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)

                ExifInfo(
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null,
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 },
                    iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 },
                    exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }
                )
            } else {
                ExifInfo()
            }
        } catch (e: Exception) {
            ExifInfo()
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: Exception) {}
        }
    }
}
