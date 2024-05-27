/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.eventprocessor

import com.bosch.pt.csm.application.security.AuthorizationUtils
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.company.employee.command.api.DeleteEmployeeCommand
import com.bosch.pt.csm.company.employee.command.handler.DeleteEmployeeCommandHandler
import com.bosch.pt.csm.company.employee.query.EmployeeQueryService
import com.bosch.pt.csm.user.user.query.UserQueryService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Profile("!restore-db && !remove-employee-on-user-deleted-listener-disabled")
@Component
class RemoveEmployeeOnUserDeletedEventListener(
    private val logger: Logger,
    private val deleteEmployeeCommandHandler: DeleteEmployeeCommandHandler,
    private val userQueryService: UserQueryService,
    private val employeeQueryService: EmployeeQueryService,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) : UserEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('user')}"],
      groupId = "\${custom.kafka.listener.remove-employee-on-user-deleted.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.remove-employee-on-user-deleted.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.remove-employee-on-user-deleted.concurrency}")
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    if (isUserDeletedTombstoneMessage(record)) {
      (record.key() as AggregateEventMessageKey).aggregateIdentifier.identifier.asUserId().let {
        deleteEmployeeForUser(it)
      }
    }

    ack.acknowledge()
  }

  private fun isUserDeletedTombstoneMessage(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>
  ) =
      record.key().let {
        record.value() == null &&
            it is AggregateEventMessageKey &&
            it.aggregateIdentifier.type == USER.name
      }

  private fun deleteEmployeeForUser(userRef: UserId) {
    runAsSystemUser {
      employeeQueryService.findEmployeeByUserId(userRef)?.run {
        deleteEmployeeCommandHandler.handleOnUserDelete(DeleteEmployeeCommand(this.identifier))
        logger.info("Employee for deleted user with identifier ${this.identifier} was removed.")
      }
    }
  }

  private fun runAsSystemUser(block: () -> Unit) {
    AuthorizationUtils.setUserAuthentication(userQueryService.findOne(systemUserIdentifier)!!)
    run(block)
    SecurityContextHolder.clearContext()
  }
}
