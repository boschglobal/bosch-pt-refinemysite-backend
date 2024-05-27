/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.model

import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID

class ProfilePictureBuilder private constructor() {

  private var identifier: UUID? = null
  private var version: Long? = null
  private var user: User? = null
  private var width: Long = 0
  private var height: Long = 0
  private var fileSize: Long = 0
  private var createdBy: User? = null
  private var createdDate: LocalDateTime? = null
  private var lastModifiedBy: User? = null
  private var lastModifiedDate: LocalDateTime? = null

  fun withIdentifier(identifier: UUID?): ProfilePictureBuilder = apply {
    this.identifier = identifier
  }

  fun withVersion(version: Long?): ProfilePictureBuilder = apply { this.version = version }

  fun withUser(user: User?): ProfilePictureBuilder = apply { this.user = user }

  fun withWidth(width: Long): ProfilePictureBuilder = apply { this.width = width }

  fun withHeight(height: Long): ProfilePictureBuilder = apply { this.height = height }

  fun withFileSize(fileSize: Int): ProfilePictureBuilder = apply {
    this.fileSize = fileSize.toLong()
  }

  fun withCreatedBy(user: User?): ProfilePictureBuilder = apply { createdBy = user }

  private fun withCreatedDate(createdDate: LocalDateTime): ProfilePictureBuilder = apply {
    this.createdDate = createdDate
  }

  private fun withLastModifiedDate(lastModifiedDate: LocalDateTime): ProfilePictureBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun withLastModifiedBy(lastModifiedBy: User?): ProfilePictureBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun build(): ProfilePicture =
      ProfilePicture(identifier, version, user, width, height, fileSize, true, true).apply {
        setCreatedBy(this@ProfilePictureBuilder.createdBy)
        setCreatedDate(this@ProfilePictureBuilder.createdDate)
        setLastModifiedBy(this@ProfilePictureBuilder.lastModifiedBy)
        setLastModifiedDate(this@ProfilePictureBuilder.lastModifiedDate)
      }

  companion object {

    @JvmStatic
    fun profilePicture(): ProfilePictureBuilder =
        ProfilePictureBuilder()
            .withIdentifier(randomUUID())
            .withVersion(0L)
            .withUser(user().build())
            .withWidth(100L)
            .withHeight(100L)
            .withFileSize(4096)
            .withCreatedDate(now())
            .withLastModifiedDate(now())
            .withCreatedBy(user().build())
            .withLastModifiedBy(user().build())
  }
}
