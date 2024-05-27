/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.PREVIEW
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFileBytes
import io.mockk.every
import java.util.TimeZone
import java.util.UUID.randomUUID
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Task Attachment Service")
open class TaskAttachmentServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskAttachmentService

  @Autowired private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }
  private val taskAttachmentIdentifier by lazy { getIdentifier("taskAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitTask().submitTaskAttachment()
  }

  @Suppress("SwallowedException")
  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find a task attachment blob is granted for`(userType: UserTypeAccess) {
    every { azureBlobStorageRepository.generateSignedUrl(any()) } returns null

    checkAccessWith(userType) {
      try {
        cut.generateBlobAccessUrl(taskAttachmentIdentifier, PREVIEW)
        fail("Exception expected")
      } catch (ex: AggregateNotFoundException) {
        // expected
      }
    }
  }

  @ParameterizedTest
  @MethodSource("taskAttachmentUpdatePermissionGroup")
  fun `create task attachment is granted for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { verifySaveTaskAttachmentAuthorized(taskIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("taskAttachmentUpdatePermissionGroup")
  fun `delete a task attachment is granted for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.deleteTaskAttachmentByIdentifier(taskAttachmentIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find a task attachment is denied for non-existing task attachment for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.generateBlobAccessUrl(randomUUID(), PREVIEW) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `create task attachment is denied for non-existing task for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { verifySaveTaskAttachmentAuthorized(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `delete a task attachment is denied for non-existing task attachment for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.deleteTaskAttachmentByIdentifier(randomUUID()) }
  }

  private fun verifySaveTaskAttachmentAuthorized(taskIdentifier: TaskId) {
    val identifier =
        cut.saveTaskAttachment(
            multiPartFileBytes(), taskIdentifier, "sample.png", null, TimeZone.getDefault())
    assertThat(identifier).isNotNull
  }

  companion object {
    @JvmStatic
    fun taskAttachmentUpdatePermissionGroup(): Stream<UserTypeAccess> =
        createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, CR, FM_CREATOR))
  }
}
