/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.listener.online

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.toUserId
import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.DeleteProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.handler.DeleteProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.api.DeleteTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.DeleteTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.command.api.DeleteTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.handler.DeleteTopicCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!restore-db & !kafka-delete-listener-disabled")
@Component
class DeleteCommandsListener(
    private val deleteProjectCommandHandler: DeleteProjectCommandHandler,
    private val deleteTaskCommandHandler: DeleteTaskCommandHandler,
    private val deleteTopicCommandHandler: DeleteTopicCommandHandler,
    private val userService: UserService
) {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('project-delete')}"],
      containerFactory = "kafkaListenerForCommandsContainerFactory")
  fun listenToProjectDeleteEvents(
      record: ConsumerRecord<CommandMessageKey, SpecificRecordBase?>,
      acknowledgment: Acknowledgment
  ) {
    LOGGER.logConsumption(record)
    if (record.value() is DeleteCommandAvro) {
      val deleteCommand = record.value() as DeleteCommandAvro
      doWithAuthentication(deleteCommand.userIdentifier.toUserId()) {
        executeWithAsyncRequestScope {
          // Delete data
          val identifier = deleteCommand.aggregateIdentifier
          if (TASK.value == identifier.type) {
            deleteTask(identifier)
          } else if (TOPIC.value == identifier.type) {
            deleteTopic(identifier)
          } else if (PROJECT.value == identifier.type) {
            deleteProject(identifier)
          }
        }
      }
    }

    acknowledgment.acknowledge()
  }

  private fun doWithAuthentication(userIdentifier: UserId, block: () -> Unit) =
      requireNotNull(userService.findOne(userIdentifier.identifier)) {
            "No user found for identifier $userIdentifier"
          }
          .also { doWithAuthenticatedUser(it, block) }

  private fun deleteProject(identifier: AggregateIdentifierAvro) =
      deleteProjectCommandHandler.handle(DeleteProjectCommand(identifier.identifier.asProjectId()))

  private fun deleteTask(identifier: AggregateIdentifierAvro) =
      deleteTaskCommandHandler.handle(DeleteTaskCommand(identifier.identifier.asTaskId()))

  private fun deleteTopic(identifier: AggregateIdentifierAvro) =
      deleteTopicCommandHandler.handle(DeleteTopicCommand(identifier.identifier.asTopicId()))

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DeleteCommandsListener::class.java)
  }
}
