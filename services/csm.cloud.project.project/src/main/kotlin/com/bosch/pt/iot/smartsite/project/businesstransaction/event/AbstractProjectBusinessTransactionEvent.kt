/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.businesstransaction.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractProjectBusinessTransactionEvent(val projectIdentifier: ProjectId) :
    KafkaStreamable {

  fun buildEventAuditingInformation(
      createdDate: LocalDateTime,
      createdBy: User
  ): EventAuditingInformationAvro =
      EventAuditingInformationAvro.newBuilder()
          .setDate(createdDate.toEpochMilli())
          .setUser(createdBy.identifier.toString())
          .build()

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun getChannel(): String = PROJECT_BINDING
}
