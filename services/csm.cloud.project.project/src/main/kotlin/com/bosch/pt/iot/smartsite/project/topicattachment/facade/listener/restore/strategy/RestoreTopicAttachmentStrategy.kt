/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPICATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreTopicAttachmentStrategy(
    private val topicRepository: TopicRepository,
    private val topicAttachmentRepository: TopicAttachmentRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, topicAttachmentRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      TOPICATTACHMENT.value == record.key().aggregateIdentifier.type &&
          record.value() is TopicAttachmentEventAvro?

  public override fun doHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ) {
    val event = record.value() as TopicAttachmentEventAvro?
    assertEventNotNull(event, record.key())

    if (event!!.name == CREATED || event.name == UPDATED) {
      val aggregate = event.aggregate
      val topicAttachment = findTopicAttachment(aggregate.aggregateIdentifier)
      if (topicAttachment == null) {
        createTopicAttachment(aggregate)
      } else {
        updateTopicAttachment(topicAttachment, aggregate)
      }
    } else {
      handleInvalidEventType(event.name.name)
    }
  }

  private fun createTopicAttachment(aggregate: TopicAttachmentAggregateAvro) {
    entityManager.persist(
        TopicAttachment().apply {
          setTopicAttachmentAttributes(this, aggregate)
          setAuditAttributes(this, aggregate.auditingInformation)
        })
  }

  private fun updateTopicAttachment(
      topicAttachment: TopicAttachment,
      aggregate: TopicAttachmentAggregateAvro
  ) {
    update(
        topicAttachment,
        object : DetachedEntityUpdateCallback<TopicAttachment> {
          override fun update(entity: TopicAttachment) {
            setTopicAttachmentAttributes(entity, aggregate)
            setAuditAttributes(entity, aggregate.auditingInformation)
          }
        })
  }

  private fun setTopicAttachmentAttributes(
      topicAttachment: TopicAttachment,
      aggregate: TopicAttachmentAggregateAvro
  ) {
    val attachmentAvro = aggregate.attachment
    topicAttachment.identifier = UUID.fromString(aggregate.aggregateIdentifier.identifier)
    topicAttachment.version = aggregate.aggregateIdentifier.version
    val topic = findTopicOrFail(aggregate.topic)
    topicAttachment.topic = topic
    topicAttachment.task = topic.task
    topicAttachment.fileName = attachmentAvro.fileName
    topicAttachment.fileSize = attachmentAvro.fileSize
    topicAttachment.imageHeight = attachmentAvro.height
    topicAttachment.imageWidth = attachmentAvro.width
    topicAttachment.setFullAvailable(attachmentAvro.fullAvailable)
    topicAttachment.setSmallAvailable(attachmentAvro.smallAvailable)
    topicAttachment.captureDate =
        if (attachmentAvro.captureDate == null) null else Date(attachmentAvro.captureDate)
  }

  private fun findTopicAttachment(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ): TopicAttachment? =
      topicAttachmentRepository.findOneWithDetailsByIdentifier(
          UUID.fromString(aggregateIdentifierAvro.identifier))

  private fun findTopicOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Topic =
      requireNotNull(
          topicRepository.findOneWithDetailsByIdentifier(
              aggregateIdentifierAvro.identifier.asTopicId())) {
            "Topic missing: ${aggregateIdentifierAvro.identifier}"
          }
}
