/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshot

object TopicAvroSnapshotMapper : AbstractAvroSnapshotMapper<TopicSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: TopicSnapshot,
      eventType: E
  ): TopicEventG2Avro =
      with(snapshot) {
        TopicEventG2Avro.newBuilder()
            .setName(eventType as TopicEventEnumAvro)
            .setAggregateBuilder(
                TopicAggregateG2Avro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setCriticality(TopicCriticalityEnumAvro.valueOf(criticality.toString()))
                    .setDescription(description)
                    .setTask(snapshot.taskIdentifier.toAggregateReference()))
            .build()
      }

  override fun getAggregateType() = TOPIC.value

  override fun getRootContextIdentifier(snapshot: TopicSnapshot) =
      snapshot.projectIdentifier.identifier
}
