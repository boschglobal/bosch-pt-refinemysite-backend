/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.craft.boundary.CraftService
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.authorization.ParticipantAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PATH_VARIABLE_PARTICIPANT_ID
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_RESEND
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.facade.rest.datastructure.PhoneNumberDto
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import java.util.function.Supplier
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder.getLocale
import org.springframework.stereotype.Component

@Component
open class ParticipantResourceFactoryHelper(
    messageSource: MessageSource,
    private val craftService: CraftService,
    private val participantAuthorizationComponent: ParticipantAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(
      projectIdentifier: ProjectId,
      participants: Collection<Participant>
  ): List<ParticipantResource> {

    val auditUser = userService.findAuditUsers(participants)
    val crafts = loadCraftsForParticipants(participants)
    val hasUpdateAndDeletePermission =
        participantAuthorizationComponent.hasUpdateAndDeletePermissionOnParticipantsOfProject(
            projectIdentifier)

    return participants.map {
      build(
          it,
          crafts[it.identifier],
          hasUpdateAndDeletePermission,
          auditUser[it.createdBy.get()]!!,
          auditUser[it.lastModifiedBy.get()]!!)
    }
  }

  private fun build(
      participant: Participant,
      crafts: List<ResourceReference>?,
      hasUpdateAndDeletePermission: Boolean,
      createdBy: User,
      lastModifiedBy: User
  ): ParticipantResource =
      ParticipantResource(
              identifier = participant.identifier.toUuid(),
              version = participant.version,
              createdDate = participant.createdDate.get().toDate(),
              createdBy = referTo(createdBy, deletedUserReference)!!,
              lastModifiedDate = participant.lastModifiedDate.get().toDate(),
              lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
              project = ResourceReference.from(participant.project!!),
              projectRole = participant.role,
              company =
                  if (participant.company != null) ResourceReference.from(participant.company!!)
                  else null,
              user =
                  if (participant.status == INVITED) null
                  else buildResourceReferenceWithPicture(participant.user!!, deletedUserReference),
              gender = if (participant.status == INVITED) null else participant.user!!.gender,
              phoneNumbers =
                  if (participant.status == INVITED) null
                  else
                      participant.user!!
                          .phonenumbers
                          .map { phoneNumber: PhoneNumber ->
                            PhoneNumberDto(
                                phoneNumber.phoneNumberType!!,
                                phoneNumber.countryCode!!,
                                phoneNumber.callNumber!!)
                          }
                          .sortedBy { it.phoneNumber },
              email = participant.email,
              crafts = if (participant.status == INVITED) null else crafts,
              status = participant.status)
          .apply {

            // Add link to edit and delete participant
            addAllIf(hasUpdateAndDeletePermission && participant.isUpdatePossible()) {
              listOf(
                  linkFactory
                      .linkTo(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT)
                      .withParameters(mapOf(PATH_VARIABLE_PARTICIPANT_ID to participant.identifier))
                      .withRel(LINK_DELETE),
                  linkFactory
                      .linkTo(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT)
                      .withParameters(mapOf(PATH_VARIABLE_PARTICIPANT_ID to participant.identifier))
                      .withRel(LINK_UPDATE))
            }

            // Add link to resent invitations
            addIf(hasUpdateAndDeletePermission && participant.isResendPossible()) {
              linkFactory
                  .linkTo(PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT)
                  .withParameters(mapOf(PATH_VARIABLE_PARTICIPANT_ID to participant.identifier))
                  .withRel(LINK_RESEND)
            }
          }

  private fun loadCraftsForParticipants(
      participants: Collection<Participant>
  ): Map<ParticipantId, List<ResourceReference>> {

    val participantToCraftResourceReferences =
        mutableMapOf<ParticipantId, List<ResourceReference>>()
    val craftsIdentifiersOfAllParticipants =
        participants
            .asSequence()
            .filter { it.status != INVITED }
            .filter { it.user!!.crafts.isNotEmpty() }
            .flatMap { it.user!!.crafts }
            .map { it.identifier!! }
            .toSet()

    val craftTranslations =
        craftService
            .findByIdentifiersAndTranslationsLocale(
                craftsIdentifiersOfAllParticipants, getLocale().language)
            .associateBy { it.craftId }

    participants
        .asSequence()
        .filter { it.status != INVITED }
        .filter { it.user!!.crafts.isNotEmpty() }
        .forEach { participant ->
          participantToCraftResourceReferences[participant.identifier] =
              participant.user!!
                  .crafts
                  .map { craft -> craftTranslations[craft.identifier]!! }
                  .map { translation ->
                    ResourceReference(
                        translation.craftId, translation.value ?: translation.defaultName)
                  }
        }
    return participantToCraftResourceReferences
  }

  private fun buildResourceReferenceWithPicture(
      user: User,
      deletedUserReference: Supplier<ResourceReference>
  ): ResourceReferenceWithPicture =
      if (user.deleted) {
        val deletedUserResourceReference = deletedUserReference.get()
        ResourceReferenceWithPicture(
            deletedUserResourceReference.identifier,
            deletedUserResourceReference.displayName,
            ProfilePictureUriBuilder.buildDefaultProfilePictureUri())
      } else {
        ResourceReferenceWithPicture.from(
            user, ProfilePictureUriBuilder.buildWithFallback(user.profilePicture))
      }
}
