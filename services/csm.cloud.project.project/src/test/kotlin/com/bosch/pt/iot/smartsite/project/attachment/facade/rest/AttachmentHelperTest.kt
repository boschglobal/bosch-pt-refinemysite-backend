/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.facade.rest

import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getMediaType
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getRawData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.IMAGE_JPEG
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.mock.web.MockMultipartFile

class AttachmentHelperTest {

  @Test
  fun `Verify that raw data is extracted from resource successfully`() {
    val multipartFile = MockMultipartFile("sample.jpg", JPG_DATA)
    val rawData = getRawData(multipartFile)
    assertThat(rawData).isEqualTo(JPG_DATA)
  }

  @Test
  fun `Verify that media type is detected correctly if input is null`() {
    val mediaType = getMediaType(null)
    assertThat(mediaType).isEqualTo(APPLICATION_OCTET_STREAM)
  }

  @Test
  fun `Verify that media type is detected correctly for JPG input`() {
    val mediaType = getMediaType(IMAGE_JPEG_VALUE)
    assertThat(mediaType).isEqualTo(IMAGE_JPEG)
  }

  @Test
  fun `Verify that media type is detected correctly for an invalid media type`() {
    val mediaType = getMediaType("invalid-media-type")
    assertThat(mediaType).isEqualTo(APPLICATION_OCTET_STREAM)
  }

  companion object {
    private val JPG_DATA = "<<jpg data>>".toByteArray()
  }
}
