/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getUserIdentifier
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeQueryService
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeService
import com.bosch.pt.iot.smartsite.craft.boundary.CraftService
import com.bosch.pt.iot.smartsite.project.participant.command.api.AcceptInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.CancelInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.AcceptInvitationCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.handler.CancelInvitationCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.user.boundary.ProfilePictureService
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
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

@Profile("!restore-db & !kafka-user-listener-disabled")
@Component
open class UserEventListenerImpl(
    private val employeeService: EmployeeService,
    private val employeeQueryService: EmployeeQueryService,
    private val craftService: CraftService,
    private val userService: UserService,
    private val profilePictureService: ProfilePictureService,
    private val invitationRepository: InvitationRepository,
    private val acceptInvitationCommandHandler: AcceptInvitationCommandHandler,
    private val cancelInvitationCommandHandler: CancelInvitationCommandHandler,
    private val transactionTemplate: TransactionTemplate,
    private val participantRepository: ParticipantRepository,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) : UserEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('user')}"],
      clientIdPrefix = "csm-cloud-project-user")
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)

    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    val key = record.key()
    val value = record.value()

    doWithAuthentication(key) {
      executeWithAsyncRequestScope {
        transactionTemplate.executeWithoutResult { handleEvent(key, value) }
      }
    }

    ack.acknowledge()
  }

  private fun handleEvent(key: EventMessageKey, message: SpecificRecordBase?) {
    if (message == null && key is AggregateEventMessageKey) {
      processTombstoneEvent(key)
    } else if (message is UserEventAvro) {
      processUserEvent(message)
    } else if (message is UserPictureEventAvro) {
      processProfilePictureEvent(message)
    } else {
      requireNotNull(message) { "Unknown tombstone avro message received: $key" }
      throw IllegalArgumentException("Unknown Avro message received: ${message.schema.name}")
    }
  }

  private fun processTombstoneEvent(key: AggregateEventMessageKey) {
    val identifier = key.aggregateIdentifier.identifier

    if (USER.value == key.aggregateIdentifier.type) {
      deleteUserSafe(identifier, key.aggregateIdentifier.version)
    } else if (USERPICTURE.value == key.aggregateIdentifier.type) {
      profilePictureService.deleteProfilePictureByIdentifier(identifier)
    } else {
      throw IllegalArgumentException("Unknown Avro tombstone message received: $key")
    }
  }

  private fun processUserEvent(userEventAvro: UserEventAvro) =
      when (userEventAvro.name) {
        UserEventEnumAvro.CREATED,
        UserEventEnumAvro.REGISTERED -> {
          createUser(userEventAvro.aggregate)
          acceptInvitations(userEventAvro.aggregate.email)
        }
        UserEventEnumAvro.UPDATED -> {
          updateUser(userEventAvro.aggregate)
        }
        else -> {
          throw IllegalArgumentException("Unknown user event type received: ${userEventAvro.name}")
        }
      }

  private fun acceptInvitations(email: String) {
    invitationRepository.findAllByEmail(email).forEach {
      acceptInvitationCommandHandler.handle(AcceptInvitationCommand(it.participantIdentifier))
    }
  }

  private fun deleteUserSafe(userIdentifier: UUID, version: Long) {
    val employee = employeeQueryService.findOneByUserIdentifier(userIdentifier)
    if (employee != null) {
      employeeService.deleteEmployee(employee.identifier!!)
    }

    cancelAllInStatusValidation(userIdentifier)
    anonymizeParticipants(userIdentifier)
    userService.anonymizeUser(userIdentifier, version)
  }

  private fun cancelAllInStatusValidation(userIdentifier: UUID) {
    participantRepository
        .findAllInValidationByUserIdentifier(userIdentifier)
        .map { it.identifier }
        .forEach { cancelInvitationCommandHandler.handle(CancelInvitationCommand(it)) }
  }

  private fun anonymizeParticipants(userIdentifier: UUID) {
    participantRepository.removeEmailFromParticipantsOfUser(userIdentifier)
  }

  private fun processProfilePictureEvent(userPictureEventAvro: UserPictureEventAvro) =
      when (userPictureEventAvro.name) {
        UserPictureEventEnumAvro.CREATED,
        UserPictureEventEnumAvro.UPDATED -> {
          updateProfilePicture(userPictureEventAvro.aggregate)
        }
        else -> {
          throw IllegalArgumentException(
              "Unknown profile picture event type received: ${userPictureEventAvro.name}")
        }
      }

  private fun updateUser(aggregate: UserAggregateAvro) {
    val craftIds = aggregate.getCraftIdentifiers()
    val crafts = if (craftIds.isEmpty()) emptySet() else craftService.findByIdentifierIn(craftIds)

    val createdBy = userService.findOne(aggregate.getCreatedByUserIdentifier())
    val lastModifiedBy = userService.findOne(aggregate.getLastModifiedByUserIdentifier())
    val user = User.fromAvroMessage(aggregate, crafts, createdBy, lastModifiedBy)
    userService.update(user)
    participantRepository.updateEmailOnParticipantsOfUser(
        user.getIdentifierUuid(), aggregate.email)
  }

  private fun createUser(aggregate: UserAggregateAvro) {
    val craftIds = aggregate.getCraftIdentifiers()
    val crafts = if (craftIds.isEmpty()) emptySet() else craftService.findByIdentifierIn(craftIds)

    val createdBy = userService.findOne(aggregate.getCreatedByUserIdentifier())
    val lastModifiedBy = userService.findOne(aggregate.getLastModifiedByUserIdentifier())
    userService.create(User.fromAvroMessage(aggregate, crafts, createdBy, lastModifiedBy))
  }

  private fun UserAggregateAvro.getCraftIdentifiers() =
      crafts.map { it.identifier.toUUID() }.toSet()

  private fun updateProfilePicture(aggregate: UserPictureAggregateAvro) {
    val user = userService.findOne(aggregate.getUserIdentifier())
    val createdBy = userService.findOne(aggregate.getCreatedByUserIdentifier())
    val lastModifiedBy = userService.findOne(aggregate.getLastModifiedByUserIdentifier())
    profilePictureService.save(
        ProfilePicture.fromAvroMessage(aggregate, user, createdBy, lastModifiedBy))
  }

  private fun doWithAuthentication(key: EventMessageKey, block: () -> Unit) {
    // If the system user events arrives, we don't have in the database, therefore we can not yet
    // authenticate as the system user. This is an edge case that can only happen when the service
    // reprocess the first kafka events.
    if (key is AggregateEventMessageKey &&
        key.aggregateIdentifier.identifier == systemUserIdentifier) {
      return block()
    }

    val systemUser = userService.findOne(systemUserIdentifier)
    requireNotNull(systemUser) { "System user must not be null" }

    doWithAuthenticatedUser(systemUser, block)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserEventListenerImpl::class.java)
  }
}
