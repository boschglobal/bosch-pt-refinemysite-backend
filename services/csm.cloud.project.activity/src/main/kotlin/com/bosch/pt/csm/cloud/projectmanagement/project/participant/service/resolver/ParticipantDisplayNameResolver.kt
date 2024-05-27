/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.resolver

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.USER_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.DisplayNameResolver
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class ParticipantDisplayNameResolver(
    private val userRepository: UserRepository,
    private val participantService: ParticipantService,
    private val messageSource: MessageSource
) : DisplayNameResolver {

  override val type: String = "PARTICIPANT"

  override fun getDisplayName(objectReference: UnresolvedObjectReference): String {
    val participant =
        participantService.findOneCacheByIdentifierAndProjectIdentifier(
            objectReference.identifier, objectReference.contextRootIdentifier)

    return userRepository.findDisplayNameCached(participant.userIdentifier)
        ?: messageSource.getMessage(USER_DELETED, null, LocaleContextHolder.getLocale())
  }
}
