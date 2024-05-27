/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.projectcraft.authorization.ProjectCraftAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController.Companion.CRAFT_BY_CRAFT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController.Companion.PATH_VARIABLE_PROJECT_CRAFT_ID
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class ProjectCraftResourceFactoryHelper(
    messageSource: MessageSource,
    private val projectCraftAuthorizationComponent: ProjectCraftAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(projectCrafts: Collection<ProjectCraft>): List<ProjectCraftResource> {
    if (projectCrafts.isEmpty()) {
      return emptyList()
    }

    val auditUsers = userService.findAuditUsers(projectCrafts)

    val craftsWithUpdatePermission =
        projectCraftAuthorizationComponent.getProjectCraftsWithUpdatePermission(projectCrafts)
    val craftsWithDeletePermission =
        projectCraftAuthorizationComponent.getProjectCraftsWithDeletePermission(projectCrafts)

    return projectCrafts.map { projectCraft: ProjectCraft ->
      build(
          projectCraft,
          craftsWithUpdatePermission.contains(projectCraft.identifier),
          craftsWithDeletePermission.contains(projectCraft.identifier),
          auditUsers[projectCraft.createdBy.get()]!!,
          auditUsers[projectCraft.lastModifiedBy.get()]!!)
    }
  }

  open fun build(
      projectCraft: ProjectCraft,
      hasUpdatePermissionOnProjectCraft: Boolean,
      hasDeletePermissionOnProjectCraft: Boolean,
      createdBy: User,
      lastModifiedBy: User
  ): ProjectCraftResource {
    return ProjectCraftResource(
            id = projectCraft.identifier.toUuid(),
            version = projectCraft.version,
            createdDate = projectCraft.createdDate.get().toDate(),
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedDate = projectCraft.lastModifiedDate.get().toDate(),
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            project = ResourceReference.from(projectCraft.project),
            name = projectCraft.name,
            color = projectCraft.color)
        .apply {

          // add update link
          addIf(hasUpdatePermissionOnProjectCraft) {
            linkFactory
                .linkTo(CRAFT_BY_CRAFT_ID_ENDPOINT)
                .withParameters(
                    mapOf(
                        PATH_VARIABLE_PROJECT_ID to projectCraft.identifier,
                        PATH_VARIABLE_PROJECT_CRAFT_ID to projectCraft.project.identifier))
                .withRel(LINK_UPDATE)
          }

          // add delete link
          addIf(hasDeletePermissionOnProjectCraft) {
            linkFactory
                .linkTo(CRAFT_BY_CRAFT_ID_ENDPOINT)
                .withParameters(
                    mapOf(
                        PATH_VARIABLE_PROJECT_ID to projectCraft.identifier,
                        PATH_VARIABLE_PROJECT_CRAFT_ID to projectCraft.project.identifier))
                .withRel(LINK_DELETE)
          }
        }
  }
}
