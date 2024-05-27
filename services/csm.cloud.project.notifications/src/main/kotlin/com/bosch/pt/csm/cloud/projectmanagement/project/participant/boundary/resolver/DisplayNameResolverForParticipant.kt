/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.resolver

import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver.DisplayNameResolverStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class DisplayNameResolverForParticipant(
    private val userRepository: UserRepository,
    private val participantService: ParticipantService,
    private val messageSource: MessageSource
) : DisplayNameResolverStrategy {

    override val type: String
        get() = "PARTICIPANT"

    override fun getDisplayName(objectReference: ObjectReferenceWithContextRoot): String {
        val participant = participantService.findOneCachedByIdentifierAndProjectIdentifier(
            objectReference.identifier,
            objectReference.contextRootIdentifier
        )
        return userRepository.findDisplayName(participant.userIdentifier) ?: messageSource.getMessage(
            Key.USER_DELETED,
            null,
            LocaleContextHolder.getLocale()
        )
    }
}
