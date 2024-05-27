/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.common.FM_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.common.submitInitProjectData
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import io.mockk.clearMocks
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class LiveUpdateEventProcessorIntegrationTest : AbstractEventStreamIntegrationTest() {

  // it's a mock
  @Autowired private lateinit var liveUpdateEventProcessor: LiveUpdateEventProcessor

  @Test
  fun `live updates are sent to recipients on task schedule change`() {
    eventStreamGenerator.submitInitProjectData().submitTask(
        auditUserReference = CSM_USER,
        time = LocalDateTime.of(2023, 7, 12, 16, 32).toInstant(ZoneOffset.UTC)) {
          it.assignee = getByReference(FM_PARTICIPANT)
          it.status = OPEN
        }
    clearMocks(liveUpdateEventProcessor)
    eventStreamGenerator.submitTaskSchedule {
      it.start = LocalDate.of(2023, 7, 17).toEpochMilli()
      it.end = LocalDate.of(2023, 7, 21).toEpochMilli()
    }

    val key = slot<EventMessageKey>()
    val value = slot<SpecificRecordBase>()
    verify { liveUpdateEventProcessor.process(capture(key), capture(value), any()) }

    // Check key
    assertThat(key.captured)
        .isEqualTo(
            AggregateEventMessageKey(
                AggregateIdentifier(TASKSCHEDULE.value, getIdentifier("taskSchedule"), 0L),
                getIdentifier("project")))

    // Check value
    val schedule = get<TaskScheduleAggregateAvro>("taskSchedule")!!
    assertThat(value.captured)
        .isEqualTo(
            TaskScheduleEventAvro(
                TaskScheduleEventEnumAvro.CREATED,
                TaskScheduleAggregateAvro(
                    AggregateIdentifierAvro(
                        getIdentifier("taskSchedule").toString(), 0L, TASKSCHEDULE.value),
                    schedule.auditingInformation,
                    AggregateIdentifierAvro(getIdentifier("task").toString(), 0L, TASK.value),
                    LocalDate.of(2023, 7, 17).toEpochMilli(),
                    LocalDate.of(2023, 7, 21).toEpochMilli(),
                    emptyList())))
  }

  @Test
  fun `live updates are sent to user on profile picture update`() {
    eventStreamGenerator.submitInitProjectData()
    clearMocks(liveUpdateEventProcessor)
    eventStreamGenerator.submitProfilePicture(rootContextIdentifier = getIdentifier(CSM_USER)) {
      it.user = getByReference(CSM_USER)
    }

    val key = slot<EventMessageKey>()
    val value = slot<SpecificRecordBase>()
    verify { liveUpdateEventProcessor.processUserEvents(capture(key), capture(value), any()) }

    // Check key
    assertThat(key.captured)
        .isEqualTo(
            AggregateEventMessageKey(
                AggregateIdentifier(USERPICTURE.value, getIdentifier("profilePicture"), 0L),
                getIdentifier(CSM_USER)))

    // Check value
    val picture = get<UserPictureAggregateAvro>("profilePicture")!!
    assertThat(value.captured)
        .isEqualTo(
            UserPictureEventAvro(
                UserPictureEventEnumAvro.CREATED,
                UserPictureAggregateAvro(
                    AggregateIdentifierAvro(
                        getIdentifier("profilePicture").toString(), 0L, USERPICTURE.value),
                    picture.auditingInformation,
                    AggregateIdentifierAvro(getIdentifier(CSM_USER).toString(), 0L, USER.value),
                    picture.smallAvailable,
                    picture.fullAvailable,
                    picture.width,
                    picture.height,
                    picture.fileSize)))
  }
}
