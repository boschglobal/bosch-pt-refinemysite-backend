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
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import java.time.LocalDateTime.now

class ProjectCraftListBuilder private constructor() {

  private var projectCrafts: MutableList<ProjectCraft> = mutableListOf()
  private lateinit var project: Project
  private var identifier: ProjectCraftListId = ProjectCraftListId()
  private var createdBy: UserId = UserId()
  private var lastModifiedBy: UserId = UserId()
  private val createdDate = now()
  private val lastModifiedDate = now()

  fun withProject(project: Project): ProjectCraftListBuilder = apply { this.project = project }

  fun withProjectCrafts(projectCrafts: MutableList<ProjectCraft>): ProjectCraftListBuilder = apply {
    this.projectCrafts = projectCrafts
  }

  fun withIdentifier(identifier: ProjectCraftListId): ProjectCraftListBuilder = apply {
    this.identifier = identifier
  }

  fun withCreatedBy(createdBy: UserId): ProjectCraftListBuilder = apply {
    this.createdBy = createdBy
  }

  fun withLastModifiedBy(lastModifiedBy: UserId): ProjectCraftListBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun build(): ProjectCraftList =
      ProjectCraftList(project = project, projectCrafts = projectCrafts).apply {
        identifier = this@ProjectCraftListBuilder.identifier
        setCreatedBy(this@ProjectCraftListBuilder.createdBy)
        setLastModifiedBy(this@ProjectCraftListBuilder.lastModifiedBy)
        setCreatedDate(this@ProjectCraftListBuilder.createdDate)
        setLastModifiedDate(this@ProjectCraftListBuilder.lastModifiedDate)
      }

  companion object {

    @JvmStatic
    fun projectCraftList(): ProjectCraftListBuilder =
        ProjectCraftListBuilder()
            .withIdentifier(ProjectCraftListId())
            .withProject(ProjectBuilder.project().build())
  }
}
