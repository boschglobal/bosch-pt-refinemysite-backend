/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeService
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_CREATE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class ProjectListResourceFactory(
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val projectResourceFactoryHelper: ProjectResourceFactoryHelper,
    private val employeeService: EmployeeService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(projects: Page<Project>): ProjectListResource {
    val projectResources: Collection<ProjectResource> =
        projectResourceFactoryHelper.build(projects.content, true)

    val userRegistered = employeeService.findOneForCurrentUser() != null

    return ProjectListResource(
            projectResources,
            userRegistered,
            projects.number,
            projects.size,
            projects.totalPages,
            projects.totalElements)
        .apply {
          // Add create project reference
          addIf(projectAuthorizationComponent.hasCreatePermissionOnProject()) {
            linkFactory.linkTo(ProjectController.PROJECTS_ENDPOINT).withRel(LINK_CREATE_PROJECT)
          }
        }
  }
}
