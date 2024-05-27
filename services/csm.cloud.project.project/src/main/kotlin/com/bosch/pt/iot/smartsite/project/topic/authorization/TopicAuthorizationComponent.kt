/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.authorization

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.checkAuthorizationForMultipleResults
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicIds
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicSpecifications.equalsId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicSpecifications.hasProjectRole
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicSpecifications.`in`
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicSpecifications.isCreatedBy
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicSpecifications.isCreatedByCompanyOf
import java.util.UUID
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class TopicAuthorizationComponent(
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val topicRepository: TopicRepository,
    private val attachmentRepository: AttachmentRepository
) {

  fun hasCreateAttachmentPermissionOnTopic(topicIdentifier: TopicId): Boolean =
      checkAuthorizationForMultipleResults(
          { topicIdentifiers: Set<UUID>, userIdentifier: UUID ->
            topicRepository
                .findAllIfUserIsCreatorOfTopicsAndActive(
                    topicIdentifiers.asTopicIds(), userIdentifier)
                .map { it.identifier }
          },
          setOf(topicIdentifier.identifier))

  fun hasViewPermissionOnTopic(topicIdentifier: TopicId): Boolean =
      topicRepository.findTaskIdentifierByIdentifier(topicIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }

  fun hasViewPermissionOnTopicAttachment(topicAttachmentIdentifier: UUID): Boolean =
      attachmentRepository.findTaskIdentifierByIdentifier(topicAttachmentIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }

  fun hasDeletePermissionOnTopic(topicIdentifier: TopicId): Boolean =
      hasDeletePermissionOnTopics(setOf(topicIdentifier))

  fun hasDeletePermissionOnTopic(topicId: Long): Boolean =
      getCurrentUser().identifier!!.let {
        topicRepository
            .findOne(
                equalsId(topicId)
                    .and(
                        hasEditPermissionAsCsm(it)
                            .or(hasEditPermissionAsCr(it))
                            .or(hasEditPermissionAsFm(it))))
            .isPresent
      }

  fun hasDeletePermissionOnTopics(topicIdentifiers: Set<TopicId>): Boolean =
      topicIdentifiers.isEmpty() ||
          filterTopicsWithDeletePermission(topicIdentifiers).containsAll(topicIdentifiers)

  fun filterTopicsWithDeletePermission(topicIdentifiers: Set<TopicId>): Set<TopicId> =
      if (topicIdentifiers.isEmpty()) emptySet()
      else
          getCurrentUser().identifier!!.let { userId ->
            topicRepository
                .findAll(
                    `in`(topicIdentifiers)
                        .and(
                            hasEditPermissionAsCsm(userId)
                                .or(hasEditPermissionAsCr(userId))
                                .or(hasEditPermissionAsFm(userId))))
                .map { it.identifier }
                .toSet()
          }

  // "Edit" permission:
  private fun hasEditPermissionAsCsm(userIdentifier: UUID): Specification<Topic> =
      hasProjectRole(userIdentifier, CSM)

  private fun hasEditPermissionAsCr(userIdentifier: UUID): Specification<Topic> =
      hasProjectRole(userIdentifier, CR).and(isCreatedByCompanyOf(userIdentifier))

  private fun hasEditPermissionAsFm(userIdentifier: UUID): Specification<Topic> =
      hasProjectRole(userIdentifier, FM).and(isCreatedBy(userIdentifier))
}
