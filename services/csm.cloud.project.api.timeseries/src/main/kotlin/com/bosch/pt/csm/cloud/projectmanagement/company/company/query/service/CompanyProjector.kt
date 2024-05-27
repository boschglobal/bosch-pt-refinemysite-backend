/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.asCompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.CompanyMapper
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.CompanyVersion
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.PostBoxAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.StreetAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.repository.CompanyRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class CompanyProjector(private val repository: CompanyRepository) {

  fun onCompanyEvent(aggregate: CompanyAggregateAvro) {
    val existingCompany = repository.findOneByIdentifier(aggregate.getIdentifier().asCompanyId())

    if (existingCompany == null || aggregate.getVersion() > existingCompany.version) {
      (existingCompany?.updateFromCompanyAggregate(aggregate) ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onCompanyDeletedEvent(aggregate: CompanyAggregateAvro) {
    val company = repository.findOneByIdentifier(aggregate.getIdentifier().asCompanyId())
    if (company != null && !company.deleted) {
      val newVersion =
          company.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          CompanyMapper.INSTANCE.fromCompanyVersion(
              newVersion,
              company.identifier,
              company.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun CompanyAggregateAvro.toNewProjection(): Company {
    val companyVersion = this.newCompanyVersion()

    return CompanyMapper.INSTANCE.fromCompanyVersion(
        companyVersion,
        aggregateIdentifier.identifier.toUUID().asCompanyId(),
        listOf(companyVersion))
  }

  private fun Company.updateFromCompanyAggregate(aggregate: CompanyAggregateAvro): Company {
    val companyVersion = aggregate.newCompanyVersion()

    return CompanyMapper.INSTANCE.fromCompanyVersion(
        companyVersion = companyVersion,
        identifier = this.identifier,
        history = this.history.toMutableList().also { it.add(companyVersion) })
  }

  private fun CompanyAggregateAvro.newCompanyVersion(): CompanyVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return CompanyVersion(
        version = this.getVersion(),
        name = this.name,
        streetAddress =
            this.streetAddress?.let {
              StreetAddress(
                  street = it.street,
                  houseNumber = it.houseNumber,
                  city = it.city,
                  zipCode = it.zipCode,
                  area = it.area,
                  country = it.country)
            },
        postBoxAddress =
            this.postBoxAddress?.let {
              PostBoxAddress(
                  postBox = it.postBox,
                  city = it.city,
                  zipCode = it.zipCode,
                  area = it.area,
                  country = it.country)
            },
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
