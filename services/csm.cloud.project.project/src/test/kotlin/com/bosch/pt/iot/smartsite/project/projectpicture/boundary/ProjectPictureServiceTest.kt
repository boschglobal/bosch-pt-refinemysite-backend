/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.boundary

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.attachment.model.BlobBuilder.Companion.blob
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPictureBuilder
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPictureBuilder.Companion.projectPicture
import com.bosch.pt.iot.smartsite.project.projectpicture.repository.ProjectPictureRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.spyk
import io.mockk.verify
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID.randomUUID
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.springframework.util.IdGenerator

@SmartSiteMockKTest
class ProjectPictureServiceTest {

  @RelaxedMockK private lateinit var blobStoreService: BlobStoreService

  @RelaxedMockK private lateinit var idGenerator: IdGenerator

  @RelaxedMockK private lateinit var projectPictureRepository: ProjectPictureRepository

  @RelaxedMockK private lateinit var projectRepository: ProjectRepository

  @SpyK @InjectMockKs private lateinit var cut: ProjectPictureService

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class, mode = EXCLUDE, names = ["ORIGINAL", "MEDIUM"])
  internal annotation class SupportedImageResolutionsSource

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class, mode = EXCLUDE, names = ["MEDIUM"])
  internal annotation class SupportedImageResolutionsWithOriginalSource

  @AfterEach
  fun cleanUp() {
    clearMocks(blobStoreService, idGenerator, projectPictureRepository, projectRepository)
  }

  @Nested
  @DisplayName("save project picture")
  inner class SaveProjectPicture {

    var blobData = "myBlobData".toByteArray()

    @BeforeEach
    fun setup() {
      every { blobStoreService.saveImage(any(), any(), any()) } answers
          {
            blob()
                .withData(it.invocation.args[0] as ByteArray)
                .withMetadata(it.invocation.args[2] as BlobMetadata)
                .build()
          }

      every { idGenerator.generateId() } returns randomUUID()

      every { projectPictureRepository.save(any(), any()) } answers
          {
            it.invocation.args[0] as ProjectPicture
          }

      every { projectRepository.findOneByIdentifier(any()) } returns project().build()
    }

    @Nested
    @DisplayName("that is new")
    inner class NewProjectPicture {

      @Test
      fun savesEntity() {
        cut.saveProjectPicture(blobData, "image.jpg", ProjectId(), null)

        verify { projectPictureRepository.save(any(), CREATED) }
      }
    }

    @Nested
    @DisplayName("that exists already")
    inner class ExistingProjectPicture {

      lateinit var projectPicture: ProjectPicture

      @BeforeEach
      fun setup() {
        projectPicture = spyk(projectPicture().withResolutionsAvailable(SMALL, FULL).build())

        every { projectPictureRepository.findOneByProjectIdentifier(any()) } returns projectPicture
      }

      @ParameterizedTest
      @SupportedImageResolutionsWithOriginalSource
      fun deletesBlobs(resolution: ImageResolution) {
        cut.saveProjectPicture(ByteArray(0), "image.jpg", ProjectId(), null)

        verify { blobStoreService.deleteImageIfExists(any(), resolution) }
      }

      @Test
      fun deletesExistingEntity() {
        cut.saveProjectPicture(ByteArray(0), "image.jpg", ProjectId(), null)

        verify { projectPictureRepository.delete(any(), DELETED) }
      }

      @Test
      fun createsEntity() {
        cut.saveProjectPicture(ByteArray(0), "image.jpg", ProjectId(), null)

        verify { projectPictureRepository.save(any(), CREATED) }
      }

      @Test
      fun createsBlob() {
        cut.saveProjectPicture(ByteArray(0), "image.jpg", ProjectId(), null)

        verify { blobStoreService.saveImage(any(), any(), any()) }
      }
    }
  }

  @Nested
  @DisplayName("delete project picture")
  inner class DeleteProjectPicture {
    lateinit var projectPicture: ProjectPicture

    @BeforeEach
    fun setup() {
      projectPicture = spyk(projectPicture().withResolutionsAvailable(SMALL, FULL).build())
    }

    @Nested
    @DisplayName("by project picture")
    inner class ByProjectPicture {

      @Nested
      @DisplayName("that exists")
      inner class WithExistingProjectPicture {

        @BeforeEach
        fun setup() {
          every { projectPictureRepository.findOneByIdentifier(any()) } returns projectPicture
        }

        @Test
        fun deletesEntity() {
          cut.deleteProjectPicture(randomUUID())

          verify { projectPictureRepository.delete(any(), DELETED) }
        }

        @ParameterizedTest
        @SupportedImageResolutionsWithOriginalSource
        fun deletesBlob(resolution: ImageResolution) {
          cut.deleteProjectPicture(randomUUID())

          verify { blobStoreService.deleteImageIfExists(any(), resolution) }
        }
      }

      @Nested
      @DisplayName("that does not exist")
      inner class WithNonexistingProjectPicture {

        @Test
        fun throwsException() {
          every { projectPictureRepository.findOneByIdentifier(any()) } returns null

          assertThrows(
              AggregateNotFoundException::class.java,
              { cut.deleteProjectPicture(randomUUID()) },
              PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)
        }
      }
    }

    @Nested
    @DisplayName("by project")
    inner class ByProject {

      @Nested
      @DisplayName("that exists")
      inner class WithExistingProjectPicture {

        @BeforeEach
        fun setup() {
          every { projectPictureRepository.findOneByProjectIdentifier(any()) } returns
              projectPicture
        }

        @Test
        fun deletesEntity() {
          cut.deleteProjectPictureByProject(ProjectId())

          verify { projectPictureRepository.delete(any(), DELETED) }
        }

        @ParameterizedTest
        @SupportedImageResolutionsWithOriginalSource
        fun deletesBlob(resolution: ImageResolution) {
          cut.deleteProjectPictureByProject(ProjectId())

          verify { blobStoreService.deleteImageIfExists(any(), resolution) }
        }
      }

      @Nested
      @DisplayName("that does not exist")
      inner class WithNonexistingProjectPicture {

        @Test
        fun throwsException() {
          every { projectPictureRepository.findOneByProjectIdentifier(any()) } returns null

          assertThrows(
              AggregateNotFoundException::class.java,
              { cut.deleteProjectPictureByProject(ProjectId()) },
              PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)
        }
      }
    }
  }

  @Nested
  @DisplayName("find project picture")
  inner class FindProjectPicture {

    @Test
    fun returnsProjectPicture() {
      val projectIdentifier = ProjectId()
      val projectPicture: ProjectPicture = projectPicture().build()

      every { projectPictureRepository.findOneByProjectIdentifier(any()) } returns projectPicture

      val foundProjectPicture = cut.findProjectPicture(projectIdentifier)
      assertThat(foundProjectPicture).isEqualTo(projectPicture)

      verify { projectPictureRepository.findOneByProjectIdentifier(projectIdentifier) }
    }

    @Test
    fun throwsExceptionWhenProjectPictureNotExists() {
      every { projectPictureRepository.findOneByProjectIdentifier(any()) } returns null

      assertThrows(
          AggregateNotFoundException::class.java,
          { cut.findProjectPicture(ProjectId()) },
          PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)
    }
  }

  @Nested
  @DisplayName("generate blob url")
  inner class GenerateBlobAccessUrl {

    private lateinit var projectPictureBuilder: ProjectPictureBuilder
    private lateinit var accessUrl: URL

    @BeforeEach
    fun setup() {
      projectPictureBuilder = projectPicture()
      accessUrl =
          try {
            URL("https://blob-store")
          } catch (e: MalformedURLException) {
            throw IllegalStateException(e)
          }
    }

    @Test
    fun throwsExceptionWhenProjectPictureDoesNotExist() {
      every { projectPictureRepository.findOneByIdentifier(any()) } returns null

      assertThrows(
          AggregateNotFoundException::class.java,
          { cut.generateBlobAccessUrl(randomUUID(), ORIGINAL) },
          PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)
    }

    @ParameterizedTest
    @SupportedImageResolutionsWithOriginalSource
    fun throwsExceptionWhenBlobDoesNotExist(resolution: ImageResolution) {
      every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns null
      every { projectPictureRepository.findOneByIdentifier(any()) } returns
          projectPictureBuilder.build()

      assertThrows(
          AggregateNotFoundException::class.java,
          { cut.generateBlobAccessUrl(randomUUID(), resolution) },
          PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)
    }

    @ParameterizedTest
    @SupportedImageResolutionsSource
    fun returnsOriginalWhenResolutionIsUnavailable(resolution: ImageResolution) {
      val projectPicture = projectPictureBuilder.withResolutionsAvailable(resolution).build()

      every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
      every { projectPictureRepository.findOneByIdentifier(any()) } returns projectPicture

      val url = cut.generateBlobAccessUrl(randomUUID(), ORIGINAL)
      assertThat(url).isEqualTo(accessUrl)

      verify { blobStoreService.generateSignedUrlForImage(any(), ORIGINAL) }
    }

    @ParameterizedTest
    @EnumSource(value = ImageResolution::class, mode = EXCLUDE, names = ["MEDIUM"])
    fun returnsGivenResolutionWhenAvailable(resolution: ImageResolution) {
      val projectPicture = projectPictureBuilder.withResolutionsAvailable(resolution).build()

      every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
      every { projectPictureRepository.findOneByIdentifier(any()) } returns projectPicture

      val url = cut.generateBlobAccessUrl(randomUUID(), resolution)
      assertThat(url).isEqualTo(accessUrl)

      verify { blobStoreService.generateSignedUrlForImage(any(), resolution) }
    }
  }

  @Nested
  @DisplayName("update available resolution")
  inner class UpdateResolutionAvailable {

    lateinit var projectPicture: ProjectPicture

    @BeforeEach
    fun setup() {
      projectPicture = spyk(projectPicture().build())
      every { projectPictureRepository.findOneByIdentifier(any()) } returns projectPicture
      every { projectPictureRepository.save(any(), any()) } answers
          {
            it.invocation.args[0] as ProjectPicture
          }
    }

    @Test
    @DisplayName("SMALL without setting file size")
    fun updatesProjectPictureSmallResolution() {
      cut.updateResolutionAvailable(randomUUID(), SMALL)

      verify { projectPicture.setResolutionAvailable(SMALL) }
      verify { projectPictureRepository.save(any(), UPDATED) }
      verify(exactly = 0) { projectPicture.fileSize = 100L }
    }

    @Test
    @DisplayName("FULL without setting file size")
    fun updatesProjectPictureFullResolution() {
      cut.updateResolutionAvailable(randomUUID(), FULL)

      verify { projectPicture.setResolutionAvailable(FULL) }
      verify { projectPictureRepository.save(any(), UPDATED) }
      verify(exactly = 0) { projectPicture.fileSize = 100L }
    }
  }
}
