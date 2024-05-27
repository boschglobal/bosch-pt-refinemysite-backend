/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.company.boundary.CompanyService
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.model.PostBoxAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.model.StreetAddress
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromCompanyEvent(private val companyService: CompanyService) :
    AbstractStateStrategy<CompanyEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) = record.value is CompanyEventAvro

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: CompanyEventAvro): Unit =
      event.aggregate.run {
        companyService.save(
            Company(
                identifier = buildAggregateIdentifier(),
                companyIdentifier = getIdentifier(),
                name = name,
                streetAddress = getStreetAddress(this),
                postBoxAddress = getPostBoxAddress(this),
                deleted = event.name == CompanyEventEnumAvro.DELETED,
            ))
      }

  private fun getStreetAddress(companyAggregate: CompanyAggregateAvro): StreetAddress? =
      companyAggregate.streetAddress?.let {
        StreetAddress(
            area = it.area,
            city = it.city,
            country = it.country,
            houseNumber = it.houseNumber,
            street = it.street,
            zipCode = it.zipCode)
      }

  private fun getPostBoxAddress(companyAggregate: CompanyAggregateAvro): PostBoxAddress? =
      companyAggregate.postBoxAddress?.let {
        PostBoxAddress(
            area = it.area,
            city = it.city,
            country = it.country,
            postBox = it.postBox,
            zipCode = it.zipCode)
      }
}
