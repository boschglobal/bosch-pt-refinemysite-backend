/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.authorization.WorkAreaAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.WorkAreaController
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_PROJECT
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import datadog.trace.api.Trace
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class WorkAreaResourceFactoryHelper(
    messageSource: MessageSource,
    private val workAreaAuthorizationComponent: WorkAreaAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService
) : AbstractResourceFactoryHelper(messageSource) {

  @Trace
  open fun build(workAreas: List<WorkArea>, project: Project): List<WorkAreaResource> {
    val auditUsers = userService.findAuditUsers(workAreas)

    val updatePermissions =
        workAreaAuthorizationComponent.getWorkAreasWithUpdatePermission(workAreas)
    val deletePermissions =
        workAreaAuthorizationComponent.getWorkAreasWithDeletePermission(workAreas)

    return workAreas.map {
      return@map build(
          it,
          project,
          updatePermissions.contains(it.identifier),
          deletePermissions.contains(it.identifier),
          auditUsers)
    }
  }

  protected open fun build(
      workArea: WorkArea,
      project: Project,
      hasUpdatePermissionOnWorkArea: Boolean,
      hasDeletePermissionOnWorkArea: Boolean,
      auditUsers: Map<UserId, User>
  ): WorkAreaResource {
    return WorkAreaResource(
            id = workArea.identifier.toUuid(),
            version = workArea.version,
            createdDate = workArea.createdDate.get().toDate(),
            lastModifiedDate = workArea.lastModifiedDate.get().toDate(),
            lastModifiedBy =
                referTo(auditUsers[workArea.lastModifiedBy.get()]!!, deletedUserReference)!!,
            createdBy = referTo(auditUsers[workArea.createdBy.get()]!!, deletedUserReference)!!,
            name = workArea.name,
            project = ResourceReference.from(workArea.project),
            parent = workArea.parent)
        .apply {
          // add reference to the project
          add(
              linkFactory
                  .linkTo(ProjectController.PROJECT_BY_PROJECT_ID_ENDPOINT)
                  .withParameters(
                      mapOf(
                          ProjectController.PATH_VARIABLE_PROJECT_ID to
                              workArea.project.identifier))
                  .withRel(LINK_PROJECT))
          // add edit reference for project workArea
          addIf(hasUpdatePermissionOnWorkArea) {
            linkFactory
                .linkTo(WorkAreaController.WORKAREA_BY_WORKAREA_ID_ENDPOINT)
                .withParameters(
                    mapOf(WorkAreaController.PATH_VARIABLE_WORKAREA_ID to workArea.identifier))
                .withRel(LINK_UPDATE)
          }
          // add delete reference for project workArea
          addIf(hasDeletePermissionOnWorkArea) {
            linkFactory
                .linkTo(WorkAreaController.WORKAREA_BY_WORKAREA_ID_ENDPOINT)
                .withParameters(
                    mapOf(WorkAreaController.PATH_VARIABLE_WORKAREA_ID to workArea.identifier))
                .withRel(LINK_DELETE)
          }
        }
  }
}
