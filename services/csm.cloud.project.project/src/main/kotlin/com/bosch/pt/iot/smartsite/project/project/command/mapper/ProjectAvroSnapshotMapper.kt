/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot

object ProjectAvroSnapshotMapper : AbstractAvroSnapshotMapper<ProjectSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(snapshot: ProjectSnapshot, eventType: E) =
      with(snapshot) {
        ProjectEventAvro.newBuilder()
            .setName(eventType as ProjectEventEnumAvro)
            .setAggregateBuilder(
                ProjectAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setCategory(
                        if (category != null) ProjectCategoryEnumAvro.valueOf(category.name)
                        else null)
                    .setClient(client)
                    .setDescription(description)
                    .setEnd(end.toEpochMilli())
                    .setProjectAddressBuilder(
                        ProjectAddressAvro.newBuilder()
                            .setCity(address!!.city)
                            .setHouseNumber(address.houseNumber)
                            .setStreet(address.street)
                            .setZipCode(address.zipCode))
                    .setProjectNumber(projectNumber)
                    .setStart(start.toEpochMilli())
                    .setTitle(title))
            .build()
      }

  override fun getAggregateType() = PROJECT.value

  override fun getRootContextIdentifier(snapshot: ProjectSnapshot) = snapshot.identifier.toUuid()
}
