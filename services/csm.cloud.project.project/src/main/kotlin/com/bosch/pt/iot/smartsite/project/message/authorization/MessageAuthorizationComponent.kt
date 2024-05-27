/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.authorization

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.checkAuthorizationForMultipleResults
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageSpecifications.hasProjectRole
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageSpecifications.`in`
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageSpecifications.isCreatedBy
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageSpecifications.isCreatedByCompanyOf
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class MessageAuthorizationComponent(
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val messageRepository: MessageRepository,
    private val attachmentRepository: AttachmentRepository
) {

  fun hasCreateAttachmentPermissionOnMessage(messageIdentifier: UUID): Boolean =
      checkAuthorizationForMultipleResults(
          messageRepository::findAllIfUserIsCreatorOfMessages, setOf(messageIdentifier))

  fun hasViewPermissionOnMessage(messageIdentifier: MessageId): Boolean =
      messageRepository.findTaskIdentifierByIdentifier(messageIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }

  fun hasViewPermissionOnMessageAttachment(messageAttachmentIdentifier: UUID): Boolean =
      attachmentRepository.findTaskIdentifierByIdentifier(messageAttachmentIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }

  fun hasDeletePermissionOnMessage(messageIdentifier: MessageId): Boolean =
      hasDeletePermissionOnMessages(setOf(messageIdentifier))

  fun hasDeletePermissionOnMessages(messageIdentifiers: Set<MessageId>): Boolean =
      (messageIdentifiers.isEmpty() ||
          filterMessagesWithDeletePermission(messageIdentifiers).containsAll(messageIdentifiers))

  fun filterMessagesWithDeletePermission(messageIdentifiers: Set<MessageId>): Set<MessageId> =
      if (messageIdentifiers.isEmpty()) emptySet()
      else
          getCurrentUser().identifier!!.let {
            messageRepository
                .findAll(
                    `in`(messageIdentifiers)
                        .and(
                            hasEditPermissionAsCsm(it)
                                .or(hasEditPermissionAsCr(it))
                                .or(hasEditPermissionAsFm(it))))
                .map { it.identifier }
                .toSet()
          }

  private fun hasEditPermissionAsCsm(userIdentifier: UUID) = hasProjectRole(userIdentifier, CSM)

  private fun hasEditPermissionAsCr(userIdentifier: UUID) =
      hasProjectRole(userIdentifier, CR).and(isCreatedByCompanyOf(userIdentifier))

  private fun hasEditPermissionAsFm(userIdentifier: UUID) =
      hasProjectRole(userIdentifier, FM).and(isCreatedBy(userIdentifier))
}
