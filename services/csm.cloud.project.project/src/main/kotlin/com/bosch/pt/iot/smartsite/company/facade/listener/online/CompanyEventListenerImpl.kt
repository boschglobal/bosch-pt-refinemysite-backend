/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.facade.listener.online

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.boundary.CompanyService
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeService
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Company.Companion.fromAvroMessage
import com.bosch.pt.iot.smartsite.company.model.Employee.Companion.fromAvroMessage
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.participant.command.api.ActivateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.ActivateParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Profile("!restore-db & !kafka-company-listener-disabled")
@Component
open class CompanyEventListenerImpl(
    private val companyService: CompanyService,
    private val employeeRepository: EmployeeRepository,
    private val employeeService: EmployeeService,
    private val userService: UserService,
    private val activateParticipantForUserCommandHandler: ActivateParticipantCommandHandler,
    private val transactionTemplate: TransactionTemplate,
    private val participantRepository: ParticipantRepository,
    private val participantSnapshotEntityCache: ParticipantSnapshotEntityCache,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) : CompanyEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('company')}"],
      clientIdPrefix = "csm-cloud-project-company")
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)

    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    val key = record.key()
    val value = record.value()

    runAsSystemUser() {
      executeWithAsyncRequestScope {
        transactionTemplate.executeWithoutResult { handleEvent(key, value) }
      }
    }

    ack.acknowledge()
  }

  private fun handleEvent(key: EventMessageKey, message: SpecificRecordBase?) =
      when (message) {
        is CompanyEventAvro -> processCompanyEvent(message)
        is EmployeeEventAvro -> processEmployeeEvent(message)
        null -> throw IllegalArgumentException("Unknown tombstone avro message received: $key")
        else ->
            throw IllegalArgumentException("Unknown Avro message received: ${message.schema.name}")
      }

  private fun processCompanyEvent(companyEventAvro: CompanyEventAvro) {
    val companyIdentifier =
        companyEventAvro.getAggregate().getAggregateIdentifier().getIdentifier().toUUID()

    if (companyEventAvro.getName() == CREATED || companyEventAvro.getName() == UPDATED) {
      updateCompany(companyEventAvro.getAggregate())
    } else if (companyEventAvro.getName() == DELETED) {
      deleteCompany(
          companyIdentifier, companyEventAvro.getAggregate().getAggregateIdentifier().getVersion())
    } else {
      throw IllegalArgumentException(
          "Unknown user event type received: " + companyEventAvro.getName())
    }
  }

  private fun updateCompany(aggregate: CompanyAggregateAvro) {
    val createdBy = findUser(aggregate.getAuditingInformation().getCreatedBy())
    val lastModifiedBy = findUser(aggregate.getAuditingInformation().getLastModifiedBy())
    companyService.save(fromAvroMessage(aggregate, createdBy, lastModifiedBy))
  }

  private fun deleteCompany(companyIdentifier: UUID, version: Long) =
      companyService.deleteCompany(companyIdentifier, version)

  private fun processEmployeeEvent(employeeEventAvro: EmployeeEventAvro) {
    if (employeeEventAvro.getName() == EmployeeEventEnumAvro.CREATED) {
      updateEmployee(employeeEventAvro.getAggregate())

      employeeEventAvro.getAggregate().getUser().getIdentifier().toUUID().let {
        activateParticipantsInValidationForUser(it)
      }
    } else if (employeeEventAvro.getName() == EmployeeEventEnumAvro.UPDATED) {

      updateEmployee(employeeEventAvro.getAggregate())
    } else if (employeeEventAvro.getName() == EmployeeEventEnumAvro.DELETED) {

      employeeService.deleteEmployee(
          employeeEventAvro.getAggregate().getAggregateIdentifier().getIdentifier().toUUID())
    } else {
      throw IllegalArgumentException(
          "Unknown employee event type received: ${employeeEventAvro.getName()}")
    }
  }

  private fun activateParticipantsInValidationForUser(userIdentifier: UUID) {
    participantSnapshotEntityCache
        .populateFromCall {
          participantRepository.findAllInValidationByUserIdentifier(userIdentifier)
        }
        .forEach {
          activateParticipantForUserCommandHandler.handle(
              ActivateParticipantCommand(it.identifier, findCompanyOfUser(it.user!!.identifier!!)))
        }
  }

  private fun findCompanyOfUser(userIdentifier: UUID) =
      employeeRepository.findCompanyIdentifierByUserIdentifier(userIdentifier)?.asCompanyId()!!

  private fun updateEmployee(aggregate: EmployeeAggregateAvro) {
    val user = findUser(aggregate.getUser())
    val company = findCompany(aggregate.getCompany())
    val createdBy = findUser(aggregate.getAuditingInformation().getCreatedBy())
    val lastModifiedBy = findUser(aggregate.getAuditingInformation().getLastModifiedBy())
    employeeService.save(fromAvroMessage(aggregate, company, user, createdBy, lastModifiedBy))
  }

  private fun findCompany(aggregateIdentifierAvro: AggregateIdentifierAvro): Company? =
      companyService.findOneWithDetailsByIdentifier(
          aggregateIdentifierAvro.getIdentifier().toUUID())

  private fun findUser(identifier: AggregateIdentifierAvro): User? =
      userService.findOne(identifier.getIdentifier().toUUID())

  private fun runAsSystemUser(block: () -> Unit) {
    val systemUser = userService.findOne(systemUserIdentifier)
    requireNotNull(systemUser) { "System user must not be null" }

    doWithAuthenticatedUser(systemUser, block)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CompanyEventListenerImpl::class.java)
  }
}
