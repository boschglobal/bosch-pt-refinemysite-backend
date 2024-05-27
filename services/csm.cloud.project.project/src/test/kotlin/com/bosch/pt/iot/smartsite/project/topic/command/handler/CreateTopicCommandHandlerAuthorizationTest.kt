/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.command.api.CreateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateTopicCommandHandlerAuthorizationTest : AbstractTopicAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreateTopicCommandHandler

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify create topic authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            CreateTopicCommand(
                TopicId(),
                TopicCriticalityEnum.CRITICAL,
                "Topic discription",
                taskIdentifier = task.identifier,
                projectIdentifier = project.identifier))
      }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify create topic not authorized for non existing task`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            CreateTopicCommand(
                TopicId(),
                TopicCriticalityEnum.CRITICAL,
                "Topic discription",
                taskIdentifier = TaskId(),
                projectIdentifier = project.identifier))
      }
}
