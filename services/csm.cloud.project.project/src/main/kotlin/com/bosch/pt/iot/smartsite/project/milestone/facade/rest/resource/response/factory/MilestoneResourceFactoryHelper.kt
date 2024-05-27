/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.milestone.authorization.MilestoneAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneProjectCraftReference
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_MILESTONE_DELETE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_MILESTONE_UPDATE
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.referToWithPicture
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
open class MilestoneResourceFactoryHelper(
    messageSource: MessageSource,
    private val milestoneAuthorizationComponent: MilestoneAuthorizationComponent,
    private val participantQueryService: ParticipantQueryService,
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(milestones: List<Milestone>): List<MilestoneResource> {
    if (milestones.isEmpty()) {
      return emptyList()
    }

    checkAllMilestonesAreOfSameProject(milestones)

    val auditUsers = userService.findAuditUsers(milestones)

    val milestonesWithUpdateAndDeletePermission =
        milestoneAuthorizationComponent.filterMilestonesWithUpdateAndDeletePermission(
            milestones.toSet())

    val creatorParticipants =
        participantQueryService.findActiveAndInactiveParticipants(
            milestones.first().project.identifier, collectUserIdsOfMilestoneCreators(milestones))

    return milestones
        .map {
          return@map buildMilestoneResource(
              it,
              creatorParticipants[it.createdBy.get().identifier.asUserId()]!!,
              milestonesWithUpdateAndDeletePermission.contains(it.identifier),
              milestonesWithUpdateAndDeletePermission.contains(it.identifier),
              auditUsers)
        }
        .toList()
  }

  private fun buildMilestoneResource(
      milestone: Milestone,
      creatorParticipant: Participant,
      hasUpdatePermission: Boolean,
      hasDeletePermission: Boolean,
      auditUsers: Map<UserId, User>
  ): MilestoneResource {

    return MilestoneResource(
            id = milestone.identifier.toUuid(),
            version = milestone.version,
            createdDate = milestone.createdDate.get().toDate(),
            createdBy = referTo(auditUsers[milestone.createdBy.get()]!!, deletedUserReference)!!,
            lastModifiedDate = milestone.lastModifiedDate.get().toDate(),
            lastModifiedBy =
                referTo(auditUsers[milestone.lastModifiedBy.get()]!!, deletedUserReference)!!,
            name = milestone.name,
            type = milestone.type,
            date = milestone.date,
            header = milestone.header,
            project = ResourceReference.from(milestone.project),
            description = milestone.description,
            craft = milestone.craft?.let { MilestoneProjectCraftReference.from(it) },
            workArea = milestone.workArea?.let { ResourceReference.from(it) },
            creator = creatorParticipant.referToWithPicture(deletedUserReference),
            position = milestone.position ?: 0)
        .apply {
          addIf(hasUpdatePermission) {
            linkFactory
                .linkTo(MilestoneController.MILESTONE_BY_MILESTONE_ID_ENDPOINT)
                .withParameters(
                    mapOf(MilestoneController.PATH_VARIABLE_MILESTONE_ID to milestone.identifier))
                .withRel(LINK_MILESTONE_UPDATE)
          }

          addIf(hasDeletePermission) {
            linkFactory
                .linkTo(MilestoneController.MILESTONE_BY_MILESTONE_ID_ENDPOINT)
                .withParameters(
                    mapOf(MilestoneController.PATH_VARIABLE_MILESTONE_ID to milestone.identifier))
                .withRel(LINK_MILESTONE_DELETE)
          }
        }
  }

  private fun checkAllMilestonesAreOfSameProject(milestones: List<Milestone>) =
      check(milestones.map { it.project }.distinct().size == 1) {
        "All milestones must belong to the same project"
      }

  private fun collectUserIdsOfMilestoneCreators(milestones: List<Milestone>) =
      milestones.map { it.createdBy }.map { it.get() }.map { it.identifier.asUserId() }.toSet()
}
