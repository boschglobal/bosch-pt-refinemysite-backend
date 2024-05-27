/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.toUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.company.shared.model.PostBoxAddress
import com.bosch.pt.csm.company.company.shared.model.StreetAddress
import com.bosch.pt.csm.company.company.shared.repository.CompanyRepository
import com.bosch.pt.csm.company.eventstore.CompanyContextSnapshotStore
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CompanySnapshotStore(private val repository: CompanyRepository) :
    AbstractSnapshotStoreJpa<CompanyEventAvro, CompanySnapshot, Company, CompanyId>(),
    CompanyContextSnapshotStore {

  override fun findOrFail(identifier: CompanyId): CompanySnapshot =
      repository.findOneWithDetailsByIdentifier(identifier)?.asValueObject()
          ?: throw AggregateNotFoundException(
              COMPANY_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == COMPANY.value &&
          message is CompanyEventAvro &&
          setOf(CREATED, UPDATED, DELETED).contains(message.name)

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as CompanyEventAvro).name === DELETED

  override fun updateInternal(event: CompanyEventAvro, currentSnapshot: Company?): Long =
      if (event.name == DELETED && currentSnapshot != null) {
        deleteCompany(currentSnapshot)
      } else {
        when (currentSnapshot == null) {
          true -> createCompany(event)
          false -> updateCompany(currentSnapshot, event)
        }
      }

  override fun findInternal(identifier: UUID): Company? =
      repository.findOneByIdentifier(CompanyId(identifier))

  private fun createCompany(event: CompanyEventAvro) = updateCompany(Company(), event)

  private fun updateCompany(company: Company, event: CompanyEventAvro): Long {
    val aggregate = event.aggregate
    company.apply {
      setBasicCompanyAttributes(this, aggregate)
      setPostBoxAddress(this, aggregate)
      setStreetAddress(this, aggregate)
      setAuditAttributes(this, aggregate.auditingInformation)
      return repository.saveAndFlush(this).version
    }
  }

  private fun deleteCompany(company: Company) =
      repository.delete(company).let { company.version + 1 }

  private fun setAuditAttributes(company: Company, auditingInformation: AuditingInformationAvro) {
    company.apply {
      setCreatedBy(auditingInformation.createdBy.toUserId())
      setLastModifiedBy(auditingInformation.lastModifiedBy.toUserId())
      setCreatedDate(auditingInformation.createdDate.toLocalDateTimeByMillis())
      setLastModifiedDate(auditingInformation.lastModifiedDate.toLocalDateTimeByMillis())
    }
  }

  private fun setBasicCompanyAttributes(company: Company, aggregate: CompanyAggregateAvro) {
    company.apply {
      identifier = CompanyId(aggregate.aggregateIdentifier.identifier.toUUID())
      name = aggregate.name
    }
  }

  private fun setPostBoxAddress(company: Company, aggregate: CompanyAggregateAvro) {
    val postBoxAddressAvro = aggregate.postBoxAddress
    if (postBoxAddressAvro == null) {
      company.postBoxAddress = null
    } else {
      company.postBoxAddress =
          PostBoxAddress().apply {
            this.city = postBoxAddressAvro.city
            this.zipCode = postBoxAddressAvro.zipCode
            this.area = postBoxAddressAvro.area
            this.country = postBoxAddressAvro.country
            this.postBox = postBoxAddressAvro.postBox
          }
    }
  }

  private fun setStreetAddress(company: Company, aggregate: CompanyAggregateAvro) {
    val streetAddressAvro = aggregate.streetAddress
    if (streetAddressAvro == null) {
      company.streetAddress = null
    } else {
      company.streetAddress =
          StreetAddress().apply {
            this.city = streetAddressAvro.city
            this.zipCode = streetAddressAvro.zipCode
            this.area = streetAddressAvro.area
            this.country = streetAddressAvro.country
            this.street = streetAddressAvro.street
            this.houseNumber = streetAddressAvro.houseNumber
          }
    }
  }
}
