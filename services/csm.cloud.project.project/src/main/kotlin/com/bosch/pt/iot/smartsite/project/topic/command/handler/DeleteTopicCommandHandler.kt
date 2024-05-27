/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.topic.command.api.DeleteTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshot
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshotStore
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteTopicCommandHandler(
    private val snapshotStore: TopicSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val topicRepository: TopicRepository,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  fun handle(command: DeleteTopicCommand) {
    snapshotStore
        .findOrIgnore(command.identifier)
        ?.apply { ensureMarkedAsDeleted(this) }
        ?.toCommandHandler()
        ?.emitEvent(DELETED)
        ?.to(eventBus) ?: logTopicNotFound(command.identifier)
  }

  /**
   * This is a fallback to mark a topic as deleted, when the commit to the database in the
   * synchronous part of the operation failed, but the message is successfully sent via kafka.
   */
  private fun ensureMarkedAsDeleted(snapshot: TopicSnapshot) {
    if (!snapshot.deleted) {
      topicRepository.findOneByIdentifier(snapshot.identifier)?.apply {
        topicRepository.markAsDeleted(id!!)
      }
    }
  }

  private fun logTopicNotFound(identifier: TopicId) =
      logger.warn(
          "A message was consumed to delete topic {} but it was not found. " +
              "Most likely it was already deleted but the offset could not be committed.",
          identifier)
}
