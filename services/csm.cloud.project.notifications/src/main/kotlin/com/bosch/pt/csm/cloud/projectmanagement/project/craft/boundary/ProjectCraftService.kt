/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ProjectCraftService(private val projectCraftRepository: ProjectCraftRepository) {

  @Trace
  fun save(projectCraft: ProjectCraft): ProjectCraft = projectCraftRepository.save(projectCraft)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID): ProjectCraft =
      projectCraftRepository.findLatest(identifier, projectIdentifier)

  @Trace
  fun deleteProjectCraft(projectCraftIdentifier: UUID, projectIdentifier: UUID) =
      projectCraftRepository.deleteProjectCraft(projectCraftIdentifier, projectIdentifier)
}
