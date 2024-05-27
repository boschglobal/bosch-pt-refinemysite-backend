/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshot

object CompanyAvroSnapshotMapper : AbstractAvroSnapshotMapper<CompanySnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: CompanySnapshot,
      eventType: E
  ): CompanyEventAvro =
      with(snapshot) {
        CompanyEventAvro.newBuilder()
            .setName(eventType as CompanyEventEnumAvro)
            .setAggregateBuilder(
                CompanyAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setName(name)
                    .setPostBoxAddressBuilder(
                        postBoxAddress?.let {
                          PostBoxAddressAvro.newBuilder()
                              .setArea(it.area)
                              .setCity(it.city)
                              .setCountry(it.country)
                              .setPostBox(it.postBox)
                              .setZipCode(it.zipCode)
                        })
                    .setStreetAddressBuilder(
                        streetAddress?.let {
                          StreetAddressAvro.newBuilder()
                              .setArea(it.area)
                              .setCity(it.city)
                              .setCountry(it.country)
                              .setHouseNumber(it.houseNumber)
                              .setStreet(it.street)
                              .setZipCode(it.zipCode)
                        }))
            .build()
      }

  override fun getAggregateType() = COMPANY.name

  override fun getRootContextIdentifier(snapshot: CompanySnapshot) = snapshot.identifier.toUuid()
}
