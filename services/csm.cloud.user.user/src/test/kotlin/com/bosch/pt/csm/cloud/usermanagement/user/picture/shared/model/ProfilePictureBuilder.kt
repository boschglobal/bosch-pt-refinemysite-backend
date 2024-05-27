/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import java.util.UUID
import java.util.UUID.randomUUID
import org.springframework.test.util.ReflectionTestUtils

/** Builds [ProfilePicture]s. */
class ProfilePictureBuilder private constructor() {

  private var identifier: UUID? = null
  private var internalVersion: Long? = null
  private var user: User? = null
  private var resolutionsAvailable: Set<ImageResolution> = emptySet()
  private var width: Long = 0
  private var height: Long = 0
  private var fileSize: Long = 0
  private var createdBy: User? = null

  /**
   * Identifier.
   *
   * @param identifier identifier
   * @return this
   */
  fun withIdentifier(identifier: UUID): ProfilePictureBuilder {
    this.identifier = identifier
    return this
  }

  /**
   * Sets the internal version number of the user for optimistic locking and calculating the ETag.
   *
   * @param internalVersion the internal version
   * @return the builder
   */
  fun withInternalVersion(internalVersion: Long): ProfilePictureBuilder {
    this.internalVersion = internalVersion
    return this
  }

  /**
   * User.
   *
   * @param user user
   * @return this
   */
  fun withUser(user: User): ProfilePictureBuilder {
    this.user = user
    return this
  }

  /**
   * With resolutions available.
   *
   * @param resolutions the image resolution
   * @return the builder itself
   */
  fun withResolutionsAvailable(vararg resolutions: ImageResolution): ProfilePictureBuilder {
    resolutionsAvailable = HashSet(listOf(*resolutions))
    return this
  }

  /**
   * Width.
   *
   * @param width width
   * @return this
   */
  fun withWidth(width: Long): ProfilePictureBuilder {
    this.width = width
    return this
  }

  /**
   * Height.
   *
   * @param height height
   * @return this
   */
  fun withHeight(height: Long): ProfilePictureBuilder {
    this.height = height
    return this
  }

  /**
   * File size.
   *
   * @param fileSize file size
   * @return this
   */
  fun withFileSize(fileSize: Int): ProfilePictureBuilder {
    this.fileSize = fileSize.toLong()
    return this
  }

  /**
   * Created By.
   *
   * @param user user
   * @return this
   */
  fun withCreatedBy(user: User): ProfilePictureBuilder {
    createdBy = user
    return this
  }

  /**
   * Constructs a [ProfilePicture].
   *
   * @return profile picture
   */
  fun build(): ProfilePicture {
    val profilePicture =
        ProfilePicture(ProfilePictureId(identifier!!), user!!, width, height, fileSize)
    resolutionsAvailable.forEach { imageResolution: ImageResolution ->
      profilePicture.setResolutionAvailable(imageResolution)
    }
    profilePicture.setCreatedBy(createdBy!!.identifier)

    if (internalVersion != null) {
      ReflectionTestUtils.setField(profilePicture, "version", internalVersion)
    }

    return profilePicture
  }

  companion object {

    /**
     * Constructor.
     *
     * @return new [ProfilePictureBuilder] with default values.
     */
    fun profilePicture() =
        ProfilePictureBuilder()
            .withIdentifier(randomUUID())
            .withUser(defaultUser())
            .withWidth(100L)
            .withHeight(100L)
            .withFileSize(4096)
            .withCreatedBy(defaultUser())
  }
}
