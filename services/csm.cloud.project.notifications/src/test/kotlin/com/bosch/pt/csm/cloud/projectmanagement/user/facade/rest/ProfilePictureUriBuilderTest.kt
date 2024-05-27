/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils
import com.bosch.pt.csm.cloud.projectmanagement.user.model.GenderEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import io.mockk.MockKAnnotations.init
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import java.net.URI
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProfilePictureUriBuilderTest {

  @RelaxedMockK private lateinit var apiVersionProperties: ApiVersionProperties

  @InjectMockKs private lateinit var cut: ProfilePictureUriBuilder

  @BeforeEach
  fun setup() {
    HttpTestUtils.setFakeUrlWithApiVersion(1)
    init(this)
    every { apiVersionProperties.user!!.version }.returns(1)
  }

  @AfterEach
  internal fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun profilePictureUriIsGeneratedSuccessfully() {
    val user =
        User(
            identifier = randomUUID(),
            admin = false,
            userPictureIdentifier = randomUUID(),
            displayName = "test",
            gender = GenderEnum.MALE)
    assertThat(cut.buildProfilePictureUri(user))
        .isEqualTo(
            URI.create(
                "http://localhost/v1/users/${user.identifier}/picture/${user.userPictureIdentifier}/SMALL"))
  }

  @Test
  fun profilePictureUriIsDefaulted() {
    val user =
        User(
            identifier = randomUUID(),
            admin = false,
            userPictureIdentifier = null,
            displayName = "test",
            gender = GenderEnum.FEMALE)
    assertThat(cut.buildProfilePictureUri(user))
        .isEqualTo(URI.create("http://localhost/default-profile-picture.png"))
  }
}
