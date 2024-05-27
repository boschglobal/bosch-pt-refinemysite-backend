/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFTLIST
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.toAggregateReference

object ProjectCraftListAvroSnapshotMapper : AbstractAvroSnapshotMapper<ProjectCraftListSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: ProjectCraftListSnapshot,
      eventType: E
  ): ProjectCraftListEventAvro =
      with(snapshot) {
        ProjectCraftListEventAvro.newBuilder()
            .setName(eventType as ProjectCraftListEventEnumAvro)
            .setAggregateBuilder(
                ProjectCraftListAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setProject(projectIdentifier.toAggregateReference())
                    .setProjectCrafts(projectCraftIdentifiers.map { it.toAggregateReference() }))
            .build()
      }

  override fun getAggregateType() = PROJECTCRAFTLIST.value

  override fun getRootContextIdentifier(snapshot: ProjectCraftListSnapshot) =
      snapshot.projectIdentifier.toUuid()
}
