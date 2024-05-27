/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable.unpaged

@DisplayName("Test authorization in Task Attachment Query Service")
open class TaskAttachmentQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskAttachmentQueryService

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }
  private val taskAttachmentIdentifier by lazy { getIdentifier("taskAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitTask().submitTaskAttachment()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find single task attachment is granted for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val attachment = cut.findOneByIdentifier(taskAttachmentIdentifier)
      assertThat(attachment).isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find all task attachments of a task is granted for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val attachments = cut.findAllByTaskIdentifier(taskIdentifier)
      assertThat(attachments).isNotEmpty()
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find all task, topic and message attachments of one task is granted for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      val attachments = cut.findAllByTaskIdentifierIncludingChildren(taskIdentifier)
      assertThat(attachments).isNotEmpty()
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find all task, topic and message attachments of a set of tasks is granted for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      val attachments =
          cut.findAllByTaskIdentifierInIncludingChildren(
              listOf(taskIdentifier), DEFAULT_PAGE_REQUEST)
      assertThat(attachments).isNotEmpty()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find single attachment is denied for non-existing task for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findOneByIdentifier(randomUUID()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find all attachments view permission is denied for non-existing task for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAllByTaskIdentifier(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find all task, topic and message attachments is denied for non-existing task for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAllByTaskIdentifierIncludingChildren(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find all task, topic and message attachments is denied for non-existing tasks for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      cut.findAllByTaskIdentifierInIncludingChildren(setOf(TaskId()), unpaged())
    }
  }
}
