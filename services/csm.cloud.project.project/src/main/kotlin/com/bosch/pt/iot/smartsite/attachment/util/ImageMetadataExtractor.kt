/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.util

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMAGE_VALIDATION_ERROR_INVALID_IMAGE
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.MetadataException
import com.drew.metadata.bmp.BmpHeaderDirectory
import com.drew.metadata.exif.ExifDirectoryBase.TAG_DATETIME_ORIGINAL
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.gif.GifHeaderDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.png.PngDirectory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Date
import java.util.TimeZone
import org.springframework.http.MediaType.IMAGE_GIF_VALUE
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.stereotype.Component

@Component
open class ImageMetadataExtractor {

  private val mimeTypeDetector = MimeTypeDetector()

  /**
   * Read metadata from image.
   *
   * @param image where to read metadata from
   * @param captureTimeZone the assumed time zone in which the image was captured
   * @return the [ImageMetadataDto] read from image
   */
  @Suppress("ThrowsCount")
  open fun readMetadata(image: ByteArray, captureTimeZone: TimeZone?): ImageMetadataDto {
    val mimeType = detectImageMimeTypeOrThrowException(image)

    try {
      ByteArrayInputStream(image).use { imageInputStream ->
        val metadata = ImageMetadataReader.readMetadata(imageInputStream)
        val directory: Directory
        val width: Long
        val height: Long

        when (mimeType) {
          IMAGE_PNG_VALUE -> {
            directory = metadata.getFirstDirectoryOfType(PngDirectory::class.java)
            width = directory.getLong(PngDirectory.TAG_IMAGE_WIDTH)
            height = directory.getLong(PngDirectory.TAG_IMAGE_HEIGHT)
          }
          IMAGE_JPEG_VALUE -> {
            directory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
            width = directory.getLong(JpegDirectory.TAG_IMAGE_WIDTH)
            height = directory.getLong(JpegDirectory.TAG_IMAGE_HEIGHT)
          }
          IMAGE_GIF_VALUE -> {
            directory = metadata.getFirstDirectoryOfType(GifHeaderDirectory::class.java)
            width = directory.getLong(GifHeaderDirectory.TAG_IMAGE_WIDTH)
            height = directory.getLong(GifHeaderDirectory.TAG_IMAGE_HEIGHT)
          }
          IMAGE_BMP_VALUE -> {
            directory = metadata.getFirstDirectoryOfType(BmpHeaderDirectory::class.java)
            width = directory.getLong(BmpHeaderDirectory.TAG_IMAGE_WIDTH)
            height = directory.getLong(BmpHeaderDirectory.TAG_IMAGE_HEIGHT)
          }
          else -> throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_INVALID_IMAGE)
        }

        return ImageMetadataDto(
            image.size.toLong(),
            width,
            height,
            findMostAccurateCaptureTime(metadata, captureTimeZone))
      }
    } catch (ex: ImageProcessingException) {
      throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_INVALID_IMAGE, cause = ex)
    } catch (ex: MetadataException) {
      throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_INVALID_IMAGE, cause = ex)
    } catch (ex: IOException) {
      throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_INVALID_IMAGE, cause = ex)
    }
  }

  private fun detectImageMimeTypeOrThrowException(image: ByteArray): String =
      mimeTypeDetector.detect(image).let { mimeType ->
        if (mimeType == IMAGE_GIF_VALUE ||
            mimeType == IMAGE_JPEG_VALUE ||
            mimeType == IMAGE_PNG_VALUE ||
            mimeType == IMAGE_BMP_VALUE) {
          mimeType
        } else {
          throw PreconditionViolationException(Key.IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE)
        }
      }

  companion object {

    private const val IMAGE_BMP_VALUE = "image/bmp"

    /**
     * Tries several ways to obtain the capture date of the image starting with the most promising
     * attempt. If all attempts fail the method returns null.
     *
     * @param metadata image meta data
     * @param timeZone the assumed time zone in which the image was captured
     * @return a capture date or null if not found
     */
    private fun findMostAccurateCaptureTime(metadata: Metadata, timeZone: TimeZone?): Date? {
      val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
      val exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

      return if (gpsDir != null && gpsDir.gpsDate != null) {
        // try GPS time (already utc)
        gpsDir.gpsDate
      } else {
        // try exif time with time zone information
        exifDir?.getDate(TAG_DATETIME_ORIGINAL, timeZone)
      }
    }
  }
}
