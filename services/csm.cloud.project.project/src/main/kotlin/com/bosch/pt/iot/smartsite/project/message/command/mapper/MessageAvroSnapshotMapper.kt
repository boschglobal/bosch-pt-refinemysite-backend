/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.MessageSnapshot
import com.bosch.pt.iot.smartsite.project.topic.domain.toAggregateReference

object MessageAvroSnapshotMapper : AbstractAvroSnapshotMapper<MessageSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: MessageSnapshot,
      eventType: E
  ): MessageEventAvro =
      with(snapshot) {
        MessageEventAvro.newBuilder()
            .setName(eventType as MessageEventEnumAvro)
            .setAggregateBuilder(
                MessageAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setContent(content)
                    .setTopic(topicIdentifier.toAggregateReference()))
            .build()
      }

  override fun getAggregateType() = MESSAGE.value

  override fun getRootContextIdentifier(snapshot: MessageSnapshot) =
      snapshot.projectIdentifier.identifier
}
