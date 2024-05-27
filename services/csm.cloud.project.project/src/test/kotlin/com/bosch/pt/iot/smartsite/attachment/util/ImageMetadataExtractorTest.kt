/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.util

import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import java.util.TimeZone
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class ImageMetadataExtractorTest {

  private val cut = ImageMetadataExtractor()

  @Test
  fun verifyReadMetadataWithJpegMimeType() {
    val resource = ClassPathResource("/img/1-bbm-boxberg.jpg")
    val imageJpeg =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }

    val result = cut.readMetadata(imageJpeg, null)

    assertThat(result).isInstanceOf(ImageMetadataDto::class.java)
  }

  @Test
  fun verifyReadMetadataWithPngMimeType() {
    val resource = ClassPathResource("/img/bosch_smart_home_system_07.png")
    val imagePng =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }

    val result = cut.readMetadata(imagePng, null)

    assertThat(result).isInstanceOf(ImageMetadataDto::class.java)
  }

  @Test
  fun verifyReadMetadataWithGifMimeType() {
    val resource = ClassPathResource("/gif/bosch.gif")
    val imageGif =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }

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
  fun verifyReadMetadataWithInvalidImage() {
    val resource = ClassPathResource("/img/false_image.jpg")
    val imageInvalid =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }

    assertThatThrownBy { cut.readMetadata(imageInvalid, null) }
        .isInstanceOf(PreconditionViolationException::class.java)
  }

  @Test
  fun verifyFindMostAccurateCaptureTimeWithGpsTime() {
    val resource = ClassPathResource("/img/gps.jpg")
    val imageGps =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }
    val result = cut.readMetadata(imageGps, null)

    assertThat(result.imageCreationDate).isNotNull
  }

  @Test
  fun verifyFindMostAccurateCaptureTimeWithExifTimezone() {
    val resource = ClassPathResource("/img/exif.jpg")
    val imageJpeg =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }
    val result = cut.readMetadata(imageJpeg, TimeZone.getTimeZone("GMT-1"))

    assertThat(result.imageCreationDate).isNotNull
  }

  @Test
  fun verifyFindMostAccurateCaptureTimeWithoutGpsTimeOrTimezone() {
    val resource = ClassPathResource("/img/noexif.jpg")
    val imageJpeg =
        ByteArray(resource.contentLength().toInt()).also { resource.inputStream.read(it) }
    val result = cut.readMetadata(imageJpeg, null)

    assertThat(result.imageCreationDate).isNull()
  }
}
