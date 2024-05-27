/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.shared.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import java.time.LocalDateTime.now

class ProjectCraftBuilder private constructor() {

  private var name: String = ""
  private var color: String = ""
  private var project: Project? = null
  private var projectCraftList: ProjectCraftList? = null
  private var position: Int? = null
  private var identifier: ProjectCraftId? = null
  private var createdBy: UserId = UserId()
  private var lastModifiedBy: UserId = UserId()
  private val createdDate = now()
  private val lastModifiedDate = now()

  fun withName(name: String): ProjectCraftBuilder = apply { this.name = name }

  fun withColor(color: String): ProjectCraftBuilder = apply { this.color = color }

  fun withProject(project: Project): ProjectCraftBuilder = apply { this.project = project }

  fun withProjectCraftList(projectCraftList: ProjectCraftList): ProjectCraftBuilder = apply {
    this.projectCraftList = projectCraftList
  }

  fun withPosition(position: Int): ProjectCraftBuilder = apply { this.position = position }

  fun withIdentifier(identifier: ProjectCraftId?): ProjectCraftBuilder = apply {
    this.identifier = identifier
  }

  fun withCreatedBy(createdBy: UserId): ProjectCraftBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: UserId): ProjectCraftBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun build(): ProjectCraft =
      ProjectCraft(
              project = project!!,
              name = name,
              color = color,
              projectCraftList = projectCraftList,
              position = position)
          .apply {
            identifier = this@ProjectCraftBuilder.identifier!!
            setCreatedBy(this@ProjectCraftBuilder.createdBy)
            setLastModifiedBy(this@ProjectCraftBuilder.lastModifiedBy)
            setCreatedDate(this@ProjectCraftBuilder.createdDate)
            setLastModifiedDate(this@ProjectCraftBuilder.lastModifiedDate)
          }

  companion object {

    @JvmStatic
    fun projectCraft(): ProjectCraftBuilder =
        ProjectCraftBuilder()
            .withIdentifier(ProjectCraftId())
            .withName("Elektrizität")
            .withColor("#FFFFFF")
            .withPosition(0)
            .withProject(ProjectBuilder.project().build())
  }
}
