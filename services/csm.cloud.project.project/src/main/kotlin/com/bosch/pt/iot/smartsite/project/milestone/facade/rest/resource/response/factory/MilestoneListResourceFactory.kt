/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.iot.smartsite.project.milestone.authorization.MilestoneAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
open class MilestoneListResourceFactory(
    private val factoryHelper: MilestoneResourceFactoryHelper,
    private val milestoneAuthorizationComponent: MilestoneAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(
      milestones: Page<Milestone>,
      projectRef: ProjectId
  ): ListResponseResource<MilestoneResource> {

    val hasCreateCraftMilestonePermission =
        milestoneAuthorizationComponent.hasCreateMilestonePermissionOnProject(projectRef, CRAFT)

    val hasCreateInvestorsMilestonePermission =
        milestoneAuthorizationComponent.hasCreateMilestonePermissionOnProject(projectRef, INVESTOR)

    val hasCreateProjectMilestonePermission =
        milestoneAuthorizationComponent.hasCreateMilestonePermissionOnProject(projectRef, PROJECT)

    return ListResponseResource(
            items = factoryHelper.build(milestones.content),
            pageNumber = milestones.number,
            pageSize = milestones.size,
            totalPages = milestones.totalPages,
            totalElements = milestones.totalElements)
        .apply {
          // create craft milestone link
          addIf(hasCreateCraftMilestonePermission) {
            linkFactory
                .linkTo(MilestoneController.MILESTONES_ENDPOINT)
                .withRel(MilestoneResource.LINK_CREATE_CRAFT_MILESTONE)
          }
          // create investor milestone link
          addIf(hasCreateInvestorsMilestonePermission) {
            linkFactory
                .linkTo(MilestoneController.MILESTONES_ENDPOINT)
                .withRel(MilestoneResource.LINK_CREATE_INVESTOR_MILESTONE)
          }
          // create project milestone link
          addIf(hasCreateProjectMilestonePermission) {
            linkFactory
                .linkTo(MilestoneController.MILESTONES_ENDPOINT)
                .withRel(MilestoneResource.LINK_CREATE_PROJECT_MILESTONE)
          }
        }
  }
}
