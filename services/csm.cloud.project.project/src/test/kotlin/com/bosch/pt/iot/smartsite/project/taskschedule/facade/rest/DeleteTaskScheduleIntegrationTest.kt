/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DELETE_NOT_POSSIBLE_DUE_TO_EXISTING_DAY_CARDS
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
class DeleteTaskScheduleIntegrationTest : AbstractTaskScheduleIntegrationTest() {

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

  @Test
  fun `verify delete by task identifier with task schedule not found`() {
    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.deleteByTaskIdentifier(taskWithoutSchedule.identifier, ETag.from("1")) }
        .withMessageKey(TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete by task identifier with task schedule slots not empty`() {
    assertThatThrownBy {
          cut.deleteByTaskIdentifier(
              taskScheduleWithTwoDayCards.task!!.identifier,
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_SCHEDULE_VALIDATION_ERROR_DELETE_NOT_POSSIBLE_DUE_TO_EXISTING_DAY_CARDS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete task schedule with outdated etag`() {
    assertThatThrownBy {
          cut.deleteByTaskIdentifier(
              taskScheduleWithoutDayCard.task!!.identifier,
              ETag.from((taskScheduleWithoutDayCard.version - 1).toString()))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(EntityOutdatedException(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED))

    projectEventStoreUtils.verifyEmpty()
  }
}
