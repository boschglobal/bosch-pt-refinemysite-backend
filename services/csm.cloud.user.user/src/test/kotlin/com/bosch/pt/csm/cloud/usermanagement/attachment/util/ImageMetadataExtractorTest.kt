/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.util

import com.bosch.pt.csm.cloud.usermanagement.attachment.util.dto.ImageMetadataDto
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import java.io.IOException
import java.util.TimeZone
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/** Test to verify expected behavior of the image metadata extractor. */
class ImageMetadataExtractorTest {

  private val cut = ImageMetadataExtractor()

  @Test
  @Throws(IOException::class)
  fun verifyReadMetadataWithJpegMimeType() {
    // JPEG image
    val resource = ClassPathResource("/img/1-bbm-boxberg.jpg")
    val imageJpeg = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageJpeg)

    val result = cut.readMetadata(imageJpeg, null)
    assertThat(result).isInstanceOf(ImageMetadataDto::class.java)
  }

  @Test
  @Throws(IOException::class)
  fun verifyReadMetadataWithPngMimeType() {
    // PNG image
    val resource = ClassPathResource("/img/bosch_smart_home_system_07.png")
    val imagePng = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imagePng)

    val result = cut.readMetadata(imagePng, null)
    assertThat(result).isInstanceOf(ImageMetadataDto::class.java)
  }

  @Test
  @Throws(IOException::class)
  fun verifyReadMetadataWithGifMimeType() {
    // GIF image
    val resource = ClassPathResource("/gif/bosch.gif")
    val imageGif = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageGif)

    val result = cut.readMetadata(imageGif, null)
    assertThat(result).isInstanceOf(ImageMetadataDto::class.java)
  }

  @Test
  fun verifyReadMetadataWithInvalidMimeType() {
    val data = ByteArray(0)
    assertThatThrownBy { cut.readMetadata(data, null) }
        .isInstanceOf(PreconditionViolationException::class.java)
  }

  @Test
  @Throws(IOException::class)
  fun verifyReadMetadataWithInvalidImage() {
    // Invalid image
    val resource = ClassPathResource("/img/false_image.jpg")
    val imageInvalid = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageInvalid)

    assertThatThrownBy { cut.readMetadata(imageInvalid, null) }
        .isInstanceOf(PreconditionViolationException::class.java)
  }

  @Test
  fun verifyReadMetadataWithWithoutImage() {
    assertThatThrownBy { cut.readMetadata(null, null) }
        .isInstanceOf(PreconditionViolationException::class.java)
  }

  @Test
  @Throws(IOException::class)
  fun verifyFindMostAccurateCaptureTimeWithGpsTime() {
    // Image with GPS data
    val resource = ClassPathResource("/img/gps.jpg")
    val imageGps = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageGps)

    val (_, _, _, imageCreationDate) = cut.readMetadata(imageGps, null)
    assertThat(imageCreationDate).isNotNull
  }

  @Test
  @Throws(IOException::class)
  fun verifyFindMostAccurateCaptureTimeWithTimezone() {
    // JPEG image with exif information
    val resource = ClassPathResource("/img/exif.jpg")
    val imageJpeg = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageJpeg)

    val (_, _, _, imageCreationDate) = cut.readMetadata(imageJpeg, TimeZone.getTimeZone("GMT-1"))
    assertThat(imageCreationDate).isNotNull
  }

  @Test
  @Throws(IOException::class)
  fun verifyFindMostAccurateCaptureTimeWithoutGpsTimeOrTimezone() {
    // JPEG image without exif information
    val resource = ClassPathResource("/img/noexif.jpg")
    val imageJpeg = ByteArray(resource.contentLength().toInt())
    resource.inputStream.read(imageJpeg)

    val (_, _, _, imageCreationDate) = cut.readMetadata(imageJpeg, null)
    assertThat(imageCreationDate).isNull()
  }
}
