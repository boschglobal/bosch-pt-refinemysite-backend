/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.boundary.AsynchronousDeleteServiceV2
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import datadog.trace.api.Trace
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TopicRequestDeleteService(
    private val topicRepository: TopicRepository,
    private val commandSendingService: CommandSendingService
) : AsynchronousDeleteServiceV2<TopicId> {

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasDeletePermissionOnTopic(#identifier)")
  @Transactional
  override fun markAsDeletedAndSendEvent(identifier: TopicId) {

    val topic =
        topicRepository.findOneByIdentifier(identifier)
            ?: throw AccessDeniedException("User has no access to this topic")

    // Create message to send
    val key = CommandMessageKey(topic.task.project.identifier.toUuid())
    val deleteCommandAvro =
        DeleteCommandAvro(
            getAggregateIdentifier(topic, TOPIC.value),
            getAggregateIdentifier(
                SecurityContextHelper.getInstance().getCurrentUser(), USER.value))

    // Mark the topic as deleted without sending an event and increasing the hibernate version
    markAsDeleted(topic.identifier)

    // Send message to delete the topic
    commandSendingService.send(key, deleteCommandAvro, "project-delete")
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  override fun markAsDeleted(identifier: TopicId) {
    topicRepository.findOneByIdentifier(identifier)?.apply {
      topicRepository.markAsDeleted(this.id!!)
    }
  }
}
