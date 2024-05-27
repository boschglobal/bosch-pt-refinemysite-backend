/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshot

object ProjectCraftAvroSnapshotMapper : AbstractAvroSnapshotMapper<ProjectCraftSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: ProjectCraftSnapshot,
      eventType: E
  ): ProjectCraftEventG2Avro =
      ProjectCraftEventG2Avro.newBuilder()
          .setName((eventType as ProjectCraftEventEnumAvro))
          .setAggregateBuilder(
              ProjectCraftAggregateG2Avro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setProject(snapshot.projectIdentifier.toAggregateReference())
                  .setName(snapshot.name)
                  .setColor(snapshot.color))
          .build()

  override fun getAggregateType() = PROJECTCRAFT.value

  override fun getRootContextIdentifier(snapshot: ProjectCraftSnapshot) =
      snapshot.projectIdentifier.toUuid()
}
