/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.authorization

import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.projectpicture.repository.ProjectPictureRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class ProjectPictureAuthorizationComponent(
    private val projectPictureRepository: ProjectPictureRepository,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent
) {

  open fun hasReadPermissionOnPicture(pictureIdentifier: UUID): Boolean =
      projectPictureRepository.findProjectIdentifierByPictureIdentifier(pictureIdentifier).let {
        it != null && projectAuthorizationComponent.hasReadPermissionOnProject(it)
      }

  open fun hasDeletePermissionOnPicture(pictureIdentifier: UUID): Boolean =
      projectPictureRepository.findProjectIdentifierByPictureIdentifier(pictureIdentifier).let {
        it != null && projectAuthorizationComponent.hasUpdatePermissionOnProject(it)
      }
}
