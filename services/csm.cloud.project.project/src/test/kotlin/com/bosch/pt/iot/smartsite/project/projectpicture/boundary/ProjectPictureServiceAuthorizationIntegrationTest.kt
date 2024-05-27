/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.boundary

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFileBytes
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Project Picture Service")
open class ProjectPictureServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectPictureService

  @Autowired private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  private val projectPicture by lazy {
    repositories.findProjectPicture(getIdentifier("projectPicture"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectPicture()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `delete project picture is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.deleteProjectPicture(projectPicture.identifier!!) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `delete project picture by project is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.deleteProjectPictureByProject(project.identifier) }
  }

  @Suppress("SwallowedException")
  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `generate blob access url is granted to`(userType: UserTypeAccess) {
    every { azureBlobStorageRepository.generateSignedUrl(any()) } returns null

    checkAccessWith(userType) {
      try {
        cut.generateBlobAccessUrl(projectPicture.identifier!!, SMALL)
        fail("Exception expected")
      } catch (ex: AggregateNotFoundException) {
        // expected
      }
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find project picture is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val picture = cut.findProjectPicture(project.identifier)
      assertThat(picture).isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `save project picture is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val pictureIdentifier =
          cut.saveProjectPicture(multiPartFileBytes(), "image.png", project.identifier, null)
      assertThat(pictureIdentifier).isNotNull()
    }
  }
}
