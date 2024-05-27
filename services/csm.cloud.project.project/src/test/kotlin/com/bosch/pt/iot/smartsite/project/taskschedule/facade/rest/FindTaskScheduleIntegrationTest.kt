/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
class FindTaskScheduleIntegrationTest : AbstractTaskScheduleIntegrationTest() {

  @Test
  fun `verify find with details by task identifier with task schedule not found`() {
    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findByTaskIdentifier(taskWithoutSchedule.identifier) }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find with details by identifier and project identifier with task schedule not found`() {

    eventStreamGenerator.submitProject(asReference = "otherProject")
    projectEventStoreUtils.reset()

    val projectIdentifier = getIdentifier("otherProject").asProjectId()

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.find(projectIdentifier, taskWithoutSchedule.identifier.identifier.asTaskScheduleId())
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find by task identifiers fails for non supported identifier type`() {
    assertThatThrownBy {
          cut.findByBatchIdentifiers(BatchRequestResource(setOf(randomUUID())), DAYCARD)
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            BatchIdentifierTypeNotSupportedException(
                COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED))

    projectEventStoreUtils.verifyEmpty()
  }
}
