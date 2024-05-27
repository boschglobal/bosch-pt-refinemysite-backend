/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.model

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.Random
import java.util.UUID
import java.util.UUID.randomUUID

class ProjectPictureBuilder private constructor() {

  private var createdBy: User? = null
  private var fileSize: Long? = null
  private var resolutionsAvailable = emptySet<ImageResolution>()
  private var height: Long? = null
  private var identifier: UUID? = null
  private var lastModifiedBy: User? = null
  private var project: Project? = null
  private var width: Long? = null

  fun withCreatedBy(createdBy: User?): ProjectPictureBuilder = apply { this.createdBy = createdBy }

  fun withFileSize(fileSize: Long): ProjectPictureBuilder = apply { this.fileSize = fileSize }

  fun withResolutionsAvailable(vararg resolutions: ImageResolution?): ProjectPictureBuilder =
      apply {
        resolutionsAvailable = resolutions.map { it!! }.toSet()
      }

  fun withHeight(height: Long?): ProjectPictureBuilder = apply { this.height = height }

  fun withIdentifier(identifier: UUID?): ProjectPictureBuilder = apply {
    this.identifier = identifier
  }

  fun withLastModifiedBy(lastModifiedBy: User?): ProjectPictureBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withProject(project: Project?): ProjectPictureBuilder = apply { this.project = project }

  fun withWidth(width: Long?): ProjectPictureBuilder = apply { this.width = width }

  fun build(): ProjectPicture {
    val projectPicture = ProjectPicture(project!!, fileSize!!)

    resolutionsAvailable.forEach { imageResolution: ImageResolution ->
      projectPicture.setResolutionAvailable(imageResolution)
    }

    if (identifier != null) {
      projectPicture.identifier = identifier
    }
    if (createdBy != null) {
      projectPicture.setCreatedBy(createdBy)
    }
    if (lastModifiedBy != null) {
      projectPicture.setLastModifiedBy(lastModifiedBy)
    }
    if (height != null) {
      projectPicture.height = height
    }
    if (width != null) {
      projectPicture.width = width
    }

    return projectPicture
  }

  companion object {

    @JvmStatic
    fun projectPicture(): ProjectPictureBuilder =
        ProjectPictureBuilder()
            .withFileSize(Random(1000000L).nextLong())
            .withHeight(800L)
            .withIdentifier(randomUUID())
            .withProject(project().build())
            .withWidth(800L)
  }
}
