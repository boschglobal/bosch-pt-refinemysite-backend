/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory.TaskResourceFactoryHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskResourceFactoryAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskResourceFactoryHelper

  private val taskOne by lazy {
    repositories.findTaskWithDetails(
        EventStreamGeneratorStaticExtensions.getIdentifier("taskOne").asTaskId())!!
  }
  private val taskTwo by lazy {
    repositories.findTaskWithDetails(
        EventStreamGeneratorStaticExtensions.getIdentifier("taskTwo").asTaskId())!!
  }
  private val otherProject by lazy {
    repositories.findProject(
        EventStreamGeneratorStaticExtensions.getIdentifier("otherProject").asProjectId())
  }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask("taskOne")
        .submitTask("taskTwo")
        .submitTopicG2()
        .submitMessage()
  }

  @Test
  fun `verify task batch build not possible for tasks belonging to different projects`() {
    taskTwo.apply { project = otherProject!! }

    val thrown =
        Assertions.assertThrows(PreconditionViolationException::class.java) {
          cut.build(listOf(taskOne, taskTwo), false)
        }
    assertEquals(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT, thrown.messageKey)
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify task resource build access for active participants`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.build(listOf(taskOne, taskTwo), false) }
  }
}
