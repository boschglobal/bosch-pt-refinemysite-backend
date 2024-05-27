/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory.DefaultProfilePictureUriBuilder.FILE_NAME_DEFAULT_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory.DefaultProfilePictureUriBuilder.build
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class DefaultProfilePictureUriBuilderTest {

  @BeforeEach
  fun setup() {
    val request = MockHttpServletRequest()
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
  }

  @Test
  fun verifyUriToDefaultProfilePicture() {
    val pictureUri = build()
    assertThat(pictureUri.toString()).isEqualTo("http://localhost/$FILE_NAME_DEFAULT_PICTURE")
  }
}
