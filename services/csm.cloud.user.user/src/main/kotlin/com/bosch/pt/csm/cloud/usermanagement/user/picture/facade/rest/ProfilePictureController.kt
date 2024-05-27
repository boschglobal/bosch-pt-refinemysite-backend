/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
@file:Suppress("SwallowedException")

package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.ImageMetadataExtractor.Companion.IMAGE_BMP_VALUE
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.RegisteredUserPrincipal
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.ATTACHMENT_VALIDATION_ERROR_IOERROR
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.DeleteProfilePictureOfUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.api.SaveProfilePictureCommand
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler.DeleteProfilePictureCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.handler.SaveProfilePictureCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory.ProfilePictureResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource
import com.bosch.pt.csm.cloud.usermanagement.user.picture.query.ProfilePictureQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import jakarta.annotation.Nonnull
import java.io.BufferedInputStream
import java.io.IOException
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.MediaType.IMAGE_GIF_VALUE
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.util.IdGenerator
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentServletMapping

@ApiVersion(from = 3)
@RestController
class ProfilePictureController(
    private val saveProfilePictureCommandHandler: SaveProfilePictureCommandHandler,
    private val deleteProfilePictureCommandHandler: DeleteProfilePictureCommandHandler,
    private val profilePictureQueryService: ProfilePictureQueryService,
    private val profilePictureResourceFactory: ProfilePictureResourceFactory,
    private val idGenerator: IdGenerator
) {

  @GetMapping(CURRENT_USER_PICTURE_ENDPOINT_PATH, USER_PICTURE_BY_USER_ID_ENDPOINT_PATH)
  fun findOwnProfilePictureMetadata(
      @PathVariable(name = PATH_VARIABLE_USER_ID, required = false) userIdentifier: UserId?,
      @RegisteredUserPrincipal user: User
  ): ResponseEntity<ProfilePictureResource> =
      profilePictureQueryService
          .findProfilePictureByUser((userIdentifier ?: user.identifier))
          .let {
            when (it != null) {
              true -> profilePictureResourceFactory.build(it)
              else -> profilePictureResourceFactory.build(user)
            }
          }
          .let { ResponseEntity.ok(it) }

  @GetMapping(USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT_PATH)
  fun findProfilePicture(
      @PathVariable(name = PATH_VARIABLE_PICTURE_ID) pictureIdentifier: ProfilePictureId,
      @PathVariable(name = PATH_VARIABLE_SIZE) size: String
  ): ResponseEntity<*> =
      try {
        ImageResolution.valueOf(StringUtils.upperCase(size))
            .let { profilePictureQueryService.generateBlobAccessUrl(pictureIdentifier, it) }
            .let { ResponseEntity.status(FOUND).header(LOCATION, it.toString()).build<Any>() }
      } catch (e: AggregateNotFoundException) {
        ResponseEntity.notFound().build<Any>()
      }

  /**
   * Save someone else's profile picture (i.e. when an admin stores a profile picture for a regular
   * user).
   *
   * @param file multipart file containing the profile picture
   * @param userIdentifier the target user's identifier
   * @param identifier optional identifier of the new profile picture
   * @param user currently authenticated user
   * @return metadata of the stored profile picture
   */
  @PostMapping(
      USER_PICTURE_BY_USER_ID_ENDPOINT_PATH,
      USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_ENDPOINT_PATH,
      CURRENT_USER_PICTURE_ENDPOINT_PATH,
      CURRENT_USER_PICTURE_BY_ID_ENDPOINT_PATH)
  fun saveProfilePicture(
      @RequestParam(REQUEST_PARAM_FILE) file: MultipartFile,
      @PathVariable(value = PATH_VARIABLE_USER_ID, required = false) userIdentifier: UserId?,
      @PathVariable(name = PATH_VARIABLE_PICTURE_ID, required = false)
      identifier: ProfilePictureId?,
      @RegisteredUserPrincipal user: User
  ): ResponseEntity<ProfilePictureResource> {
    if (!allowedContentTypes.contains(checkNotNull(file.contentType))) {
      throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE)
    }

    val fileName = file.originalFilename!!
    val pictureData = getRawData(file)
    val profilePictureIdentifier =
        saveProfilePictureCommandHandler.handle(
            SaveProfilePictureCommand(
                identifier ?: ProfilePictureId(idGenerator.generateId()),
                (userIdentifier ?: user.identifier),
                pictureData,
                fileName))
    val profilePicture =
        profilePictureQueryService.findOneWithDetailsByIdentifier(profilePictureIdentifier)!!

    val httpHeaders =
        HttpHeaders().apply {
          location =
              fromCurrentServletMapping()
                  .path(
                      getCurrentApiVersionPrefix() +
                          USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_ENDPOINT_PATH +
                          "/full")
                  .buildAndExpand(
                      profilePicture.user.getIdentifierUuid(), profilePicture.getIdentifierUuid())
                  .toUri()
        }

    return ResponseEntity(
        profilePictureResourceFactory.build(profilePicture), httpHeaders, HttpStatus.CREATED)
  }

  @DeleteMapping(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH, CURRENT_USER_PICTURE_ENDPOINT_PATH)
  fun deleteProfilePicture(
      @PathVariable(name = PATH_VARIABLE_USER_ID, required = false) userIdentifier: UserId?,
      @RegisteredUserPrincipal user: User
  ): ResponseEntity<*> {
    deleteProfilePictureCommandHandler.handle(
        DeleteProfilePictureOfUserCommand(userIdentifier ?: user.identifier))
    return ResponseEntity.noContent().build<Any>()
  }

  private fun getRawData(@Nonnull file: MultipartFile): ByteArray {
    try {
      BufferedInputStream(file.inputStream).use {
        return IOUtils.toByteArray(it)
      }
    } catch (ex: IOException) {
      throw PreconditionViolationException(
          messageKey = ATTACHMENT_VALIDATION_ERROR_IOERROR, cause = ex)
    }
  }

  companion object {
    const val CURRENT_USER_PICTURE_ENDPOINT_PATH = "/users/current/picture"
    const val CURRENT_USER_PICTURE_BY_ID_ENDPOINT_PATH = "/users/current/picture/{pictureId}"
    const val USER_PICTURE_BY_USER_ID_ENDPOINT_PATH = "/users/{userId}/picture"
    const val USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_ENDPOINT_PATH =
        "/users/{userId}/picture/{pictureId}"
    const val USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT_PATH =
        "/users/{userId}/picture/{pictureId}/{size}"
    const val PATH_VARIABLE_USER_ID = "userId"
    const val PATH_VARIABLE_PICTURE_ID = "pictureId"
    const val PATH_VARIABLE_SIZE = "size"
    const val REQUEST_PARAM_FILE = "file"

    val allowedContentTypes =
        listOf(IMAGE_BMP_VALUE, IMAGE_GIF_VALUE, IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE)
  }
}
