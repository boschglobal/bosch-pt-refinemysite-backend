/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TopicQueryServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TopicQueryService

  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }
  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        // Only the csm users can add project crafts to a project
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitTopicG2()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find topic by identifier authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findTopicByIdentifier(topic.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find topic by identifier not authorized for non existing topic`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.findTopicByIdentifier(TopicId()) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find paged topics is authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findPagedTopicByTaskIdAndTopicDate(task.identifier, null, 5) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find paged topics using task not authorized for non existing task`(
      userType: UserTypeAccess
  ) = checkAccessWith(userType) { cut.findPagedTopicByTaskIdAndTopicDate(TaskId(), null, 5) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find slice topics using task identifiers is authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.findByTaskIdentifiers(mutableListOf(task.identifier), DEFAULT_PAGE_REQUEST)
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find slice topics using task identifiers not authorized for tasks with no view permission`(
      userType: UserTypeAccess
  ) =
      checkAccessWith(userType) {
        cut.findByTaskIdentifiers(mutableListOf(task.identifier, TaskId()), DEFAULT_PAGE_REQUEST)
      }
}
