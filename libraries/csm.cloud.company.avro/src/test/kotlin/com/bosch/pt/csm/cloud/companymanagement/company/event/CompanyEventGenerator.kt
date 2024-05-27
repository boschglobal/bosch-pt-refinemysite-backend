/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.event

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import java.time.Instant
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitCompany(
    asReference: String = "company",
    auditUserReference: String = DEFAULT_USER,
    eventType: CompanyEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((CompanyAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingCompany = get<CompanyAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((CompanyAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val companyEvent =
      existingCompany.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications)

  val sentEvent =
      send("company", asReference, null, companyEvent, time.toEpochMilli()) as CompanyEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()
  getContext().lastRootContextIdentifier = sentEvent.getAggregate().getAggregateIdentifier()

  return this
}

@JvmOverloads
fun EventStreamGenerator.submitCompanyWithStreetAddress(
    asReference: String = "company",
    auditUserReference: String = DEFAULT_USER,
    eventType: CompanyEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((CompanyAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  submitCompany(asReference, auditUserReference, eventType, time) {
    it.setDefaultStreetAddress()
    aggregateModifications?.invoke(it)
  }
  return this
}

@JvmOverloads
fun EventStreamGenerator.submitCompanyWithPostboxAddress(
    asReference: String = "company",
    auditUserReference: String = DEFAULT_USER,
    eventType: CompanyEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((CompanyAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  submitCompany(asReference, auditUserReference, eventType, time) {
    it.setDefaultPostBoxAddress()
    aggregateModifications?.invoke(it)
  }
  return this
}

@JvmOverloads
fun EventStreamGenerator.submitCompanyWithBothAddresses(
    asReference: String = "company",
    auditUserReference: String = DEFAULT_USER,
    eventType: CompanyEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((CompanyAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  submitCompany(asReference, auditUserReference, eventType, time) {
    it.setDefaultStreetAddress()
    it.setDefaultPostBoxAddress()
    aggregateModifications?.invoke(it)
  }
  return this
}

private fun CompanyAggregateAvro?.buildEventAvro(
    eventType: CompanyEventEnumAvro,
    vararg blocks: ((CompanyAggregateAvro.Builder) -> Unit)?
): CompanyEventAvro =
    (this?.let { CompanyEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newCompany(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newCompany(event: CompanyEventEnumAvro = CREATED): CompanyEventAvro.Builder {
  val company =
      CompanyAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(CompanymanagementAggregateTypeEnum.COMPANY.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setName(randomString())

  return CompanyEventAvro.newBuilder().setAggregateBuilder(company).setName(event)
}

private fun CompanyAggregateAvro.Builder.setDefaultStreetAddress() =
    setStreetAddress(
        StreetAddressAvro.newBuilder()
            .setStreet("Lincoln Drive")
            .setHouseNumber("1")
            .setArea("Washington State")
            .setZipCode("12345")
            .setCity("Washington D.C.")
            .setCountry("United States of America (the)")
            .build())

private fun CompanyAggregateAvro.Builder.setDefaultPostBoxAddress() =
    setPostBoxAddress(
        PostBoxAddressAvro.newBuilder()
            .setPostBox("PO Box 12345")
            .setArea("Washington State")
            .setZipCode("45678")
            .setCity("Washington D.C.")
            .setCountry("United States of America (the)")
            .build())
