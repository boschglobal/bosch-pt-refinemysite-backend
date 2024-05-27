/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.service

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ProjectCraftService(private val projectCraftRepository: ProjectCraftRepository) {

  @Trace fun save(projectCraft: ProjectCraft) = projectCraftRepository.save(projectCraft)

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      projectCraftRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      projectCraftRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      projectCraftRepository.deleteByVersion(identifier, version, projectIdentifier)
}
