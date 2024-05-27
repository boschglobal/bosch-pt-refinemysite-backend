/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.project.project.command.handler.DeleteProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.facade.listener.online.DeleteCommandsListener
import com.bosch.pt.iot.smartsite.project.task.command.handler.DeleteTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.command.handler.DeleteTopicCommandHandler
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
abstract class AbstractDeleteIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var deleteTaskCommandHandler: DeleteTaskCommandHandler

  @Autowired private lateinit var deleteProjectCommandHandler: DeleteProjectCommandHandler

  @Autowired private lateinit var deleteTopicCommandHandler: DeleteTopicCommandHandler

  @Autowired private lateinit var userService: UserService

  private lateinit var eventListener: DeleteCommandsListener

  @BeforeEach
  protected fun initAbstractDeleteIntegrationTest() {
    eventListener =
        DeleteCommandsListener(
            deleteProjectCommandHandler,
            deleteTaskCommandHandler,
            deleteTopicCommandHandler,
            userService)
  }

  protected fun sendDeleteCommand(
      identifier: UUID,
      version: Long?,
      type: ProjectmanagementAggregateTypeEnum,
      user: User,
      acknowledgment: TestAcknowledgement = TestAcknowledgement()
  ) {
    simulateKafkaListener {
      eventListener.listenToProjectDeleteEvents(
          buildConsumerRecord(identifier, version ?: 0, type, user), acknowledgment)
    }
  }

  protected fun buildConsumerRecord(
      identifier: UUID,
      version: Long,
      type: ProjectmanagementAggregateTypeEnum,
      user: User
  ): ConsumerRecord<CommandMessageKey, SpecificRecordBase?> =
      ConsumerRecord(
          "",
          0,
          0L,
          CommandMessageKey(identifier),
          DeleteCommandAvro.newBuilder()
              .setAggregateIdentifier(
                  AggregateIdentifierAvro(identifier.toString(), version, type.value))
              .setUserIdentifier(
                  AggregateIdentifierAvro(user.identifier.toString(), user.version, USER.value))
              .build())
}
