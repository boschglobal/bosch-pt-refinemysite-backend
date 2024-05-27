/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.iot.smartsite.project.participant.authorization.ParticipantAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANTS_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_ASSIGN
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
open class ParticipantListResourceFactory(
    private val resourceFactoryHelper: ParticipantResourceFactoryHelper,
    private val participantAuthorizationComponent: ParticipantAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  open fun build(
      participantPage: Page<Participant>,
      projectIdentifier: ProjectId
  ): ParticipantListResource {
    val participantResources =
        resourceFactoryHelper.build(projectIdentifier, participantPage.content)
    val hasAssignPermissionOnParticipantsOfProject =
        participantAuthorizationComponent.hasAssignPermissionOnParticipantsOfProject(
            projectIdentifier)

    return ParticipantListResource(
            participantResources,
            participantPage.number,
            participantPage.size,
            participantPage.totalPages,
            participantPage.totalElements)
        .apply {
          // Add reference for assignment
          addIf(hasAssignPermissionOnParticipantsOfProject) {
            linkFactory
                .linkTo(PARTICIPANTS_BY_PROJECT_ID_ENDPOINT)
                .withParameters(
                    mapOf(ParticipantController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
                .withRel(LINK_ASSIGN)
          }
        }
  }
}
