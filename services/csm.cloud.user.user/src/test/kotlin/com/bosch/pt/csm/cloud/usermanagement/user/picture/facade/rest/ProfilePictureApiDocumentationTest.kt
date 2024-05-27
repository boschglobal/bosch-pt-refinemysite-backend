/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.usermanagement.attachment.boundary.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.CURRENT_USER_PICTURE_BY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.CURRENT_USER_PICTURE_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.PATH_VARIABLE_PICTURE_ID
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.PATH_VARIABLE_USER_ID
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.REQUEST_PARAM_FILE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.USER_PICTURE_BY_USER_ID_ENDPOINT_PATH
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.net.URI
import java.util.Base64
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProfilePictureApiDocumentationTest : AbstractApiDocumentationTest() {

  @MockkBean(relaxed = true) lateinit var blobStoreService: BlobStoreService

  @Test
  fun `verify and document find current users profile picture`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")

    setAuthentication("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(CURRENT_USER_PICTURE_ENDPOINT_PATH)).accept(HAL_JSON_VALUE)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "users/document-find-own-profile-picture",
                preprocessResponse(prettyPrint()),
                responseFields(PROFILE_PICTURE_FIELD_DESCRIPTORS),
                links(PROFILE_PICTURE_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find profile picture of another user`() {
    eventStreamGenerator.submitUser("user1").submitUser("user2").submitProfilePicture("picture")

    setAuthentication("user1")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                    get(
                        latestVersionOf(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH),
                        eventStreamGenerator.getIdentifier("user2")))
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isOk)
        .andDo(
            document(
                "users/document-find-someones-profile-picture",
                preprocessResponse(prettyPrint()),
                pathParameters(USER_ID_PATH_PARAMETER_DESCRIPTOR),
                responseFields(PROFILE_PICTURE_FIELD_DESCRIPTORS),
                links(PROFILE_PICTURE_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find current users profile picture data`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")

    val profilePictureIdentifier = eventStreamGenerator.getIdentifier("picture")
    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/user/image/small/" +
                    randomUUID().toString() +
                    "/" +
                    profilePictureIdentifier)
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    setAuthentication("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(CURRENT_USER_PICTURE_BY_ID_ENDPOINT_PATH + "/small"),
                    profilePictureIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "users/document-find-profile-picture-data",
                preprocessResponse(prettyPrint()),
                pathParameters(PROFILE_PICTURE_ID_PATH_PARAMETER_DESCRIPTOR)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find profile picture data of another user`() {
    eventStreamGenerator.submitUser("user1").submitUser("user2").submitProfilePicture("picture")

    val profilePictureIdentifier = eventStreamGenerator.getIdentifier("picture")
    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/user/image/original/" +
                    randomUUID().toString() +
                    "/" +
                    profilePictureIdentifier)
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    setAuthentication("user1")

    userEventStoreUtils.verifyEmpty()

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_ENDPOINT_PATH + "/full"),
                    randomUUID(),
                    profilePictureIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "users/document-find-someones-profile-picture-data",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    USER_ID_PATH_PARAMETER_DESCRIPTOR,
                    PROFILE_PICTURE_ID_PATH_PARAMETER_DESCRIPTOR)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document save current users profile picture`() {
    eventStreamGenerator.submitUser("user")
    val file = multiPartFile()

    setAuthentication("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                multipart(latestVersionOf(CURRENT_USER_PICTURE_ENDPOINT_PATH)).file(file)))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, notNullValue()))
        .andDo(
            document(
                "users/document-save-own-profile-picture",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParts(partWithName(REQUEST_PARAM_FILE).description("Profile picture file")),
                responseFields(PROFILE_PICTURE_FIELD_DESCRIPTORS),
                links(PROFILE_PICTURE_LINK_DESCRIPTORS)))

    val currentUserAggregateIdentifier = eventStreamGenerator.getByReference("user")
    val profilePicture =
        repositories.profilePictureRepository.findOneByUserIdentifier(
            UserId(currentUserAggregateIdentifier.getIdentifier()))!!

    userEventStoreUtils
        .verifyContainsAndGet(UserPictureEventAvro::class.java, CREATED)
        .aggregate.apply {
          aggregateIdentifier.also {
            assertThat(it.type).isEqualTo(USERPICTURE.name)
            assertThat(it.identifier).isEqualTo(profilePicture.identifier.toString())
            assertThat(it.version).isEqualTo(profilePicture.version)
          }
          auditingInformation.also {
            assertThat(it.createdBy).isEqualTo(currentUserAggregateIdentifier)
            assertThat(it.createdDate).isEqualTo(profilePicture.createdDate.get().toEpochMilli())
            assertThat(it.lastModifiedBy).isEqualTo(currentUserAggregateIdentifier)
            assertThat(it.lastModifiedDate)
                .isEqualTo(profilePicture.lastModifiedDate.get().toEpochMilli())
          }
          assertThat(fullAvailable).isFalse
          assertThat(smallAvailable).isFalse
          assertThat(user).isEqualTo(currentUserAggregateIdentifier)
          assertThat(height).isEqualTo(profilePicture.height)
          assertThat(width).isEqualTo(profilePicture.width)
          assertThat(fileSize).isEqualTo(profilePicture.fileSize)
        }
  }

  @Test
  fun `verify and document save current users profile picture (with identifier)`() {
    eventStreamGenerator.submitUser("user")

    val file = multiPartFile()
    val randomUuid = randomUUID()

    setAuthentication("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                multipart(latestVersionOf(CURRENT_USER_PICTURE_BY_ID_ENDPOINT_PATH), randomUuid)
                    .file(file)))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, notNullValue()))
        .andDo(
            document(
                "users/document-save-own-profile-picture-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROFILE_PICTURE_ID_PATH_PARAMETER_DESCRIPTOR),
                requestParts(partWithName(REQUEST_PARAM_FILE).description("Profile picture file")),
                responseFields(PROFILE_PICTURE_FIELD_DESCRIPTORS),
                links(PROFILE_PICTURE_LINK_DESCRIPTORS)))

    userEventStoreUtils
        .verifyContainsAndGet(UserPictureEventAvro::class.java, CREATED)
        .getAggregate()
        .apply {
          getAggregateIdentifier().also {
            assertThat(it.getType()).isEqualTo(USERPICTURE.name)
            assertThat(it.getIdentifier()).isEqualTo(randomUuid.toString())
            assertThat(it.getVersion()).isEqualTo(0)
          }
        }
  }

  @Test
  fun `verify and document delete current users profile picture`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")

    val userIdentifier = eventStreamGenerator.getIdentifier("user")

    setAuthentication("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH), userIdentifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "users/document-delete-profile-picture",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_USER_ID)
                        .description(
                            "ID of the user. The special value 'current' refers to the authenticated user."))))

    userEventStoreUtils.verifyContainsTombstoneMessageAndGet(USERPICTURE.name)
  }

  companion object {

    private val USER_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_USER_ID).description("ID of the User")

    private val PROFILE_PICTURE_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_PICTURE_ID).description("ID of the profile picture")

    private val PROFILE_PICTURE_LINK_DESCRIPTORS =
        listOf(
            linkWithRel("small").description("Link to small resolution"),
            linkWithRel("full").description("Link to full resolution"),
            linkWithRel("delete").description("Link to delete the profile picture.").optional(),
            linkWithRel("update").description("Link to update the profile picture.").optional())

    private val PROFILE_PICTURE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("width").description("Width in pixels"),
            fieldWithPath("height").description("Height in pixels"),
            fieldWithPath("fileSize").description("File size in bytes"),
            fieldWithPath("userReference.displayName")
                .description("Name of the user whose profile picture was created."),
            fieldWithPath("userReference.id")
                .description("ID of the user whose profile picture was created."),
            fieldWithPath("id").description("ID of the Profile Picture"),
            fieldWithPath("createdDate").description("Date of creation"),
            fieldWithPath("lastModifiedDate").description("Date of last modification"),
            fieldWithPath("version").description("version of the profile picture"),
            fieldWithPath("createdBy.displayName").description("Name of the uploader"),
            fieldWithPath("createdBy.id").description("ID of the uploader"),
            fieldWithPath("lastModifiedBy.displayName").description("Name of the last uploader"),
            fieldWithPath("lastModifiedBy.id").description("ID of the last uploader"),
            subsectionWithPath("_links").ignored())
  }

  /** create a small 2x2 pixel PNG (yellow with 0.5 opacity) */
  private fun multiPartFile() =
      MockMultipartFile("file", "image2x2.png", "image/png", multiPartFileBytes())

  private fun multiPartFileBytes(): ByteArray =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0k" +
                  "AAAAEklEQVR42mP8/5+hngEIGGEMADlqBP1mY/qhAAAAAElFTkSuQmCC")
}
