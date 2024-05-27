/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.net.MalformedURLException
import java.net.URI
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.web.multipart.MultipartFile

@EnableAllKafkaListeners
class ProjectPictureIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var projectPictureController: ProjectPictureController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val projectPictureIdentifier by lazy { getIdentifier("projectPicture") }

  @MockkBean(relaxed = true) private lateinit var blobStoreService: BlobStoreService

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitProjectPicture()

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun saveProjectPicture() {
    val projectPictureCreated = setUpDefaultProjectPicture()

    assertThat(projectPictureCreated).isNotNull
    assertThat(projectPictureCreated.height).isEqualTo(0)
    assertThat(projectPictureCreated.width).isEqualTo(0)
    assertThat(projectPictureCreated.projectReference.identifier.asProjectId())
        .isEqualTo(projectIdentifier)
  }

  @Test
  fun findProjectPictureMetadata() {
    val projectPictureCreated = setUpDefaultProjectPicture()

    val response =
        projectPictureController.findProjectPictureMetadata(
            projectPictureCreated.projectReference.identifier.asProjectId())

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    assertThat(projectPictureCreated.id).isEqualTo(response.body!!.id)
  }

  @Test
  @Throws(MalformedURLException::class)
  fun findProjectPicture() {
    val projectPictureCreated = setUpDefaultProjectPicture()

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/" +
                    projectIdentifier +
                    "/" +
                    projectPictureIdentifier)
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    val response =
        projectPictureController.findProjectPicture(projectPictureCreated.id, ORIGINAL.toString())

    assertThat(response.statusCode).isEqualTo(FOUND)
    assertThat(response.body).isNull()
    assertThat(response.headers.location).isNotNull
  }

  @Test
  fun deleteProjectPicture_byProjectPicture() {
    val projectPictureCreated = setUpDefaultProjectPicture()

    // delete project picture
    assertThat(
            projectPictureController
                .deleteProjectPicture(projectIdentifier, projectPictureCreated.id)
                .statusCode)
        .isEqualTo(NO_CONTENT)

    assertThatThrownBy {
          projectPictureController.findProjectPictureMetadata(
              projectPictureCreated.projectReference.identifier.asProjectId())
        }
        .isInstanceOf(AggregateNotFoundException::class.java)
  }

  @Test
  fun deleteProjectPicture_byProject() {
    val projectPictureCreated = setUpDefaultProjectPicture()

    // delete project picture
    assertThat(projectPictureController.deleteProjectPicture(projectIdentifier, null).statusCode)
        .isEqualTo(NO_CONTENT)

    assertThatThrownBy {
          projectPictureController.findProjectPictureMetadata(
              projectPictureCreated.projectReference.identifier.asProjectId())
        }
        .isInstanceOf(AggregateNotFoundException::class.java)
  }

  private fun setUpDefaultProjectPicture(): ProjectPictureResource {
    val response =
        projectPictureController.saveProjectPicture(small2x2Picture, projectIdentifier, null)

    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.id).isNotNull

    return response.body!!
  }

  companion object {
    private val small2x2Picture: MultipartFile = multiPartFile()
  }
}
