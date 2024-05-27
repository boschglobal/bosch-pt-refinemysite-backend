/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegation.delegateAuthorization
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class ProjectCraftAuthorizationComponent(
    private val projectCraftRepository: ProjectCraftRepository,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent
) {

  open fun getProjectCraftsWithUpdatePermission(
      projectCrafts: Collection<ProjectCraft>
  ): Set<ProjectCraftId> =
      delegateAuthorization(
              projectCrafts,
              { it.project.identifier },
              { projectAuthorizationComponent.getProjectsWithUpdatePermission(it) })
          .map { it.identifier }
          .toSet()

  open fun getProjectCraftsWithDeletePermission(
      projectCrafts: Collection<ProjectCraft>
  ): Set<ProjectCraftId> =
      delegateAuthorization(
              projectCrafts,
              { it.project.identifier },
              { projectAuthorizationComponent.getProjectsWithDeletePermission(it) })
          .map { it.identifier }
          .toSet()

  open fun hasReadPermissionOnProjectCraft(projectCraftIdentifier: ProjectCraftId) =
      projectCraftRepository.findProjectIdentifierByIdentifier(projectCraftIdentifier).let {
        it != null && projectAuthorizationComponent.hasReadPermissionOnProject(it)
      }

  open fun hasUpdatePermissionOnProjectCraft(projectCraftIdentifier: ProjectCraftId) =
      projectCraftRepository.findProjectIdentifierByIdentifier(projectCraftIdentifier).let {
        it != null && projectAuthorizationComponent.hasUpdatePermissionOnProject(it)
      }

  open fun hasDeletePermissionOnProjectCraft(projectCraftIdentifier: ProjectCraftId) =
      hasUpdatePermissionOnProjectCraft(projectCraftIdentifier)
}
