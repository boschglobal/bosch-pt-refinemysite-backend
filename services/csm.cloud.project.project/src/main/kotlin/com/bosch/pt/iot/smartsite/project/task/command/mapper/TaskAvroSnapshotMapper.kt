/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.toAggregateReference

object TaskAvroSnapshotMapper : AbstractAvroSnapshotMapper<TaskSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: TaskSnapshot,
      eventType: E
  ): TaskEventAvro =
      with(snapshot) {
        TaskEventAvro.newBuilder()
            .setName(eventType as TaskEventEnumAvro)
            .setAggregateBuilder(
                TaskAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setAssignee(
                        assigneeIdentifier?.let {
                          AggregateIdentifierAvro.newBuilder()
                              .setIdentifier(it.identifier.toString())
                              .setVersion(0)
                              .setType(PARTICIPANT.value)
                              .build()
                        })
                    .setCraft(projectCraftIdentifier.toAggregateReference())
                    .setDescription(description)
                    .setEditDate(editDate?.toEpochMilli())
                    .setLocation(location)
                    .setName(name)
                    .setProject(projectIdentifier.toAggregateReference())
                    .setStatus(TaskStatusEnumAvro.valueOf(status.toString()))
                    .setWorkarea(workAreaIdentifier?.toAggregateReference()))
            .build()
      }
  override fun getAggregateType() = TASK.value

  override fun getRootContextIdentifier(snapshot: TaskSnapshot) =
      snapshot.projectIdentifier.toUuid()
}
