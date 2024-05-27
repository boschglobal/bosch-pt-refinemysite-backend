/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegation.delegateAuthorization
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class WorkAreaAuthorizationComponent(
    val workAreaRepository: WorkAreaRepository,
    val projectAuthorizationComponent: ProjectAuthorizationComponent
) {

  open fun getWorkAreasWithUpdatePermission(workAreas: Collection<WorkArea>): Set<WorkAreaId> =
      delegateAuthorization(
              workAreas,
              { it.project.identifier },
              { projectAuthorizationComponent.getProjectsWithUpdatePermission(it) })
          .map { it.identifier }
          .toSet()

  open fun getWorkAreasWithDeletePermission(workAreas: Collection<WorkArea>): Set<WorkAreaId> =
      delegateAuthorization(
              workAreas,
              { it.project.identifier },
              { projectAuthorizationComponent.getProjectsWithDeletePermission(it) })
          .map { it.identifier }
          .toSet()

  open fun hasReadPermissionOnWorkArea(workAreaIdentifier: WorkAreaId): Boolean =
      workAreaRepository.findProjectIdentifierByWorkAreaIdentifier(workAreaIdentifier).let {
        it != null && projectAuthorizationComponent.hasReadPermissionOnProject(it)
      }

  open fun hasUpdatePermissionOnWorkArea(workAreaIdentifier: WorkAreaId): Boolean =
      workAreaRepository.findProjectIdentifierByWorkAreaIdentifier(workAreaIdentifier).let {
        it != null && projectAuthorizationComponent.hasUpdatePermissionOnProject(it)
      }

  open fun hasDeletePermissionOnWorkArea(workAreaIdentifier: WorkAreaId): Boolean =
      hasUpdatePermissionOnWorkArea(workAreaIdentifier)
}
