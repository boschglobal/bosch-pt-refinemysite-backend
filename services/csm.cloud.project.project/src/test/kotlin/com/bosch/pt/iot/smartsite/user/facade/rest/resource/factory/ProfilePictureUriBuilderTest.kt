/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.FILE_NAME_DEFAULT_PICTURE
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class ProfilePictureUriBuilderTest {

  @Autowired private lateinit var apiVersionProperties: ApiVersionProperties

  private val user = user().build()
  private val profilePicture =
      ProfilePicture(
          randomUUID(), 0L, user, 200L, 200L, 200L, smallAvailable = true, fullAvailable = true)

  @BeforeEach
  fun mockApiVersioning() {
    setFakeUrlWithApiVersion()
  }

  @Test
  fun verifyBuildWithFallbackGeneratesRealUri() {
    val pictureUri = ProfilePictureUriBuilder.buildWithFallback(profilePicture)
    assertThat(pictureUri.toString())
        .isEqualTo(
            "http://localhost/v${apiVersionProperties.user!!.version}" +
                "/users/${profilePicture.user!!.identifier}" +
                "/picture/${profilePicture.identifier}/${SMALL.name}")
  }

  @Test
  fun verifyBuildWithFallbackGeneratesFallback() {
    val pictureUri = ProfilePictureUriBuilder.buildWithFallback(null)
    assertThat(pictureUri.toString()).isEqualTo("http://localhost/$FILE_NAME_DEFAULT_PICTURE")
  }

  @Test
  fun verifyBuildProfilePictureUriFailsForNullUser() {
    assertThrows<IllegalArgumentException> {
      ProfilePictureUriBuilder.buildProfilePictureUri(ProfilePicture())
    }
  }
}
