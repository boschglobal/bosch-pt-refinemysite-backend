/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.PostBoxAddress
import com.bosch.pt.iot.smartsite.company.model.StreetAddress
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreCompanyStrategy(
    private val companyRepository: CompanyRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, companyRepository),
    CompanyContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      COMPANY.value == record.key().aggregateIdentifier.type && record.value() is CompanyEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val key = record.key()
    val event = record.value() as CompanyEventAvro?
    assertEventNotNull(event, key)

    if (event!!.getName() == DELETED) {
      deleteCompany(event.getAggregate())
    } else if (event.getName() == CREATED || event.getName() == UPDATED) {
      createOrUpdateCompany(event)
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun createOrUpdateCompany(event: CompanyEventAvro) {
    val company =
        companyRepository.findOneWithDetailsByIdentifier(
            event.getAggregate().getAggregateIdentifier().getIdentifier().toUUID())

    if (company == null) {
      createCompany(event.getAggregate())
    } else {
      updateCompany(company, event.getAggregate())
    }
  }

  private fun createCompany(aggregate: CompanyAggregateAvro) =
      entityManager.persist(
          Company().apply {
            setBasicCompanyAttributes(this, aggregate)
            setPostBoxAddress(this, aggregate)
            setStreetAddress(this, aggregate)
            setAuditAttributes(this, aggregate.auditingInformation)
          })

  private fun updateCompany(company: Company, aggregate: CompanyAggregateAvro) =
      update(
          company,
          object : DetachedEntityUpdateCallback<Company> {
            override fun update(entity: Company) {
              setBasicCompanyAttributes(entity, aggregate)
              setPostBoxAddress(entity, aggregate)
              setStreetAddress(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun deleteCompany(aggregate: CompanyAggregateAvro) =
      companyRepository
          .findOneWithDetailsByIdentifier(
              aggregate.getAggregateIdentifier().getIdentifier().toUUID())
          ?.let { company ->
            update(
                company,
                object : DetachedEntityUpdateCallback<Company> {
                  override fun update(entity: Company) {
                    setBasicCompanyAttributes(entity, aggregate)
                    setPostBoxAddress(entity, aggregate)
                    setStreetAddress(entity, aggregate)
                    setAuditAttributes(entity, aggregate.getAuditingInformation())
                    entity.deleted = true
                  }
                })
          }

  private fun setBasicCompanyAttributes(company: Company, aggregate: CompanyAggregateAvro) =
      company
          .apply {
            identifier = aggregate.getAggregateIdentifier().getIdentifier().toUUID()
            version = aggregate.getAggregateIdentifier().getVersion()
            name = aggregate.getName()
          }
          .returnUnit()

  private fun setPostBoxAddress(company: Company, aggregate: CompanyAggregateAvro) =
      if (aggregate.getPostBoxAddress() == null) {
        company.postBoxAddress = null
      } else {
        val postBoxAddressAvro = aggregate.getPostBoxAddress()
        company.postBoxAddress =
            PostBoxAddress().apply {
              area = postBoxAddressAvro.getArea()
              city = postBoxAddressAvro.getCity()
              country = postBoxAddressAvro.getCountry()
              postBox = postBoxAddressAvro.getPostBox()
              zipCode = postBoxAddressAvro.getZipCode()
            }
      }

  private fun setStreetAddress(company: Company, aggregate: CompanyAggregateAvro) =
      if (aggregate.getStreetAddress() == null) {
        company.streetAddress = null
      } else {
        val streetAddressAvro = aggregate.getStreetAddress()
        company.streetAddress =
            StreetAddress().apply {
              area = streetAddressAvro.getArea()
              city = streetAddressAvro.getCity()
              country = streetAddressAvro.getCountry()
              houseNumber = streetAddressAvro.getHouseNumber()
              street = streetAddressAvro.getStreet()
              zipCode = streetAddressAvro.getZipCode()
            }
      }
}
