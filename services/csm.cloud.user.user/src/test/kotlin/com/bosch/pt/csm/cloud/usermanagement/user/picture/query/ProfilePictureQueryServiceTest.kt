/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.ImageMetadataExtractor
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.dto.ImageMetadataDto
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePictureBuilder.Companion.profilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository.ProfilePictureRepository
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import com.bosch.pt.csm.cloud.usermanagement.util.withMessageKey
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.util.IdGenerator

@SmartSiteMockKTest
class ProfilePictureQueryServiceTest {

  @MockK(relaxed = true) private lateinit var idGenerator: IdGenerator

  @MockK(relaxed = true) private lateinit var blobStoreService: BlobStoreService

  @MockK(relaxed = true) private lateinit var userRepository: UserRepository

  @MockK(relaxed = true) private lateinit var profilePictureRepository: ProfilePictureRepository

  @MockK(relaxed = true) private lateinit var imageMetadataExtractor: ImageMetadataExtractor

  @SpyK @InjectMockKs private lateinit var cut: ProfilePictureQueryService

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(
      value = ImageResolution::class,
      mode = EnumSource.Mode.EXCLUDE,
      names = ["ORIGINAL", "MEDIUM"])
  internal annotation class SupportedImageResolutionsSource

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class, mode = EnumSource.Mode.EXCLUDE, names = ["MEDIUM"])
  internal annotation class SupportedImageResolutionsWithOriginalSource

  @Nested
  @DisplayName("save profile picture")
  inner class SaveProfilePicture {

    @BeforeEach
    fun setup() {
      every { imageMetadataExtractor.readMetadata(any(), any()) } returns buildImageMetadata()
      every { userRepository.findOneByIdentifier(any()) } returns defaultUser()
      every { blobStoreService.saveImage(any(), any(), any()) } answers
          {
            buildBlob(it.invocation.args[0] as ByteArray?, it.invocation.args[2] as BlobMetadata?)
          }
      every { idGenerator.generateId() } returns randomUUID()
      every { profilePictureRepository.save(any()) } answers
          {
            it.invocation.args[0] as ProfilePicture
          }
    }

    private fun buildImageMetadata() = ImageMetadataDto(100, 10L, 10L)

    private fun buildBlob(data: ByteArray?, metadata: BlobMetadata?) =
        Blob("myBlob", data!!, metadata!!, "image/jpeg")

    @Nested
    @DisplayName("find profile picture")
    inner class FindProfilePicture {
      @Test
      fun returnsProfilePicture() {
        val userIdentifier = UserId()
        val profilePicture = profilePicture().build()
        every { profilePictureRepository.findOneWithDetailsByUserIdentifier(any()) } returns
            profilePicture

        val foundProfilePicture = cut.findProfilePictureByUser(userIdentifier)
        verify { profilePictureRepository.findOneWithDetailsByUserIdentifier(userIdentifier) }
        assertThat(foundProfilePicture).isEqualTo(profilePicture)
      }

      @Test
      fun returnNullWhenProfilePictureNotExists() {
        every { profilePictureRepository.findOneWithDetailsByUserIdentifier(any()) } returns null
        assertThat(cut.findProfilePictureByUser(UserId())).isNull()
      }
    }

    @Nested
    @DisplayName("generate blob url")
    inner class GenerateBlobAccessUrl {

      private val profilePictureBuilder = profilePicture()
      private lateinit var accessUrl: URL

      @BeforeEach
      fun setup() {
        accessUrl =
            try {
              URL("https://blob-store")
            } catch (e: MalformedURLException) {
              throw IllegalStateException(e)
            }
      }

      @Test
      fun throwsExceptionWhenProfilePictureDoesNotExist() {
        every { profilePictureRepository.findOneWithDetailsByIdentifier(any()) } returns null

        assertThatExceptionOfType(AggregateNotFoundException::class.java)
            .isThrownBy { cut.generateBlobAccessUrl(ProfilePictureId(), ORIGINAL) }
            .withMessageKey(USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND)
      }

      @ParameterizedTest
      @SupportedImageResolutionsWithOriginalSource
      fun throwsExceptionWhenBlobDoesNotExist(resolution: ImageResolution) {
        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns null
        every { profilePictureRepository.findOneWithDetailsByIdentifier(any()) } returns
            profilePictureBuilder.build()

        assertThatExceptionOfType(AggregateNotFoundException::class.java)
            .isThrownBy { cut.generateBlobAccessUrl(ProfilePictureId(), resolution) }
            .withMessageKey(USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND)
      }

      @ParameterizedTest
      @SupportedImageResolutionsSource
      fun returnsOriginalWhenResolutionIsUnavailable(resolution: ImageResolution) {
        val profilePicture = profilePictureBuilder.withResolutionsAvailable(resolution).build()
        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
        every { profilePictureRepository.findOneWithDetailsByIdentifier(any()) } returns
            profilePicture

        val url = cut.generateBlobAccessUrl(ProfilePictureId(), ORIGINAL)
        verify { blobStoreService.generateSignedUrlForImage(any(), ORIGINAL) }

        assertThat(url).isEqualTo(accessUrl)
      }

      @ParameterizedTest
      @EnumSource(
          value = ImageResolution::class, mode = EnumSource.Mode.EXCLUDE, names = ["MEDIUM"])
      fun returnsGivenResolutionWhenAvailable(resolution: ImageResolution) {
        val profilePicture = profilePictureBuilder.withResolutionsAvailable(resolution).build()
        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
        every { profilePictureRepository.findOneWithDetailsByIdentifier(any()) } returns
            profilePicture

        val url = cut.generateBlobAccessUrl(ProfilePictureId(), resolution)
        verify { blobStoreService.generateSignedUrlForImage(any(), resolution) }

        assertThat(url).isEqualTo(accessUrl)
      }
    }
  }
}
