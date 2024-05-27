/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.AssigneesFilterDto
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleSlot
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class TaskRescheduleServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskRescheduleService

  private lateinit var schedule1Start: LocalDate
  private lateinit var schedule1End: LocalDate

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    with(get<TaskScheduleAggregateAvro>("taskSchedule")!!) {
      schedule1Start = start.toLocalDateByMillis()
      schedule1End = end.toLocalDateByMillis()
    }

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `reschedule whole project if no additional filter criteria are given`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria = SearchTasksDto(projectIdentifier = getIdentifier("project").asProjectId())

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that two tasks were moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful)
        .containsExactlyInAnyOrder(
            getIdentifier("task").asTaskId(), getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 2, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by task status`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2") { it.status = TaskStatusEnumAvro.STARTED }
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            taskStatus = listOf(TaskStatusEnum.STARTED))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by project craft`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitProjectCraftG2("craft2")
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            projectCraftIdentifiers = listOf(getIdentifier("craft2").asProjectCraftId()))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by work area`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitWorkArea("workArea2")
        .submitTask("task2") { it.workarea = getByReference("workArea2") }
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            workAreaIdentifiers =
                listOf(getIdentifier("workArea2").asWorkAreaId().toWorkAreaIdOrEmpty()))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by assignee`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2") { it.assignee = getByReference("participantCsm2") }
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            assignees =
                AssigneesFilterDto(
                    participantIdentifiers =
                        listOf(getIdentifier("participantCsm2").asParticipantId())))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by date range`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            rangeStartDate = schedule2End,
            rangeEndDate = schedule2End.plusDays(1))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by start date range`() {
    val schedule2Start = now().plusDays(1)
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            rangeStartDate = schedule2Start)

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by end date range`() {
    val schedule2Start = now().plusDays(1)
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            rangeEndDate = schedule2Start.minusDays(1))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert schedule2 not shifted
    assertThat(schedule2).hasStart(schedule2Start)
    assertThat(schedule2).hasEnd(schedule2End)
    assertThat(schedule2).hasDayCardAt(schedule2Start, getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(1))
    assertThat(schedule2).hasDayCardAt(schedule2End, getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by date range with all days in date range`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            rangeStartDate = schedule2End,
            rangeEndDate = schedule2End.plusDays(1),
            allDaysInDateRange = true)

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert nothing was affected
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.failed).isEmpty()
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 not shifted
    assertThat(schedule2).hasStart(schedule2Start)
    assertThat(schedule2).hasEnd(schedule2End)

    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `reschedule all by topic criticality`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTopicG2("topic2") { it.criticality = TopicCriticalityEnumAvro.CRITICAL }
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            topicCriticality = listOf(TopicCriticalityEnum.CRITICAL))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task2").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule all by topic existence`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
    projectEventStoreUtils.reset()

    val criteria =
        SearchTasksDto(projectIdentifier = getIdentifier("project").asProjectId(), hasTopics = true)

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert schedule2 not shifted
    assertThat(schedule2).hasStart(schedule2Start)
    assertThat(schedule2).hasEnd(schedule2End)
    assertThat(schedule2).hasDayCardAt(schedule2Start, getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(1))
    assertThat(schedule2).hasDayCardAt(schedule2End, getIdentifier("dayCard2task2"))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule none if filter matches nothing`() {
    val criteria =
        SearchTasksDto(
            projectIdentifier = getIdentifier("project").asProjectId(),
            taskStatus = listOf(TaskStatusEnum.ACCEPTED))

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1) = findSchedulesOfTasksInOrder("task")

    // assert nothing was affected
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).isEmpty()
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `reschedule none of the non-visible calendar tasks`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskSchedule("schedule2") {
          it.start = schedule2Start.toEpochMilli()
          it.end = null
        }
        .submitTask("task3")
        .submitTaskSchedule("schedule") {
          it.start = null
          it.end = schedule2End.toEpochMilli()
        }
        .submitTask("task4")
        .submitTaskSchedule("schedule4") {
          it.start = null
          it.end = null
        }
    projectEventStoreUtils.reset()

    val criteria = SearchTasksDto(projectIdentifier = getIdentifier("project").asProjectId())

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1) = findSchedulesOfTasksInOrder("task")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task").asTaskId())
    assertThat(reschedule.failed).isEmpty()

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule fail for closed or accepted tasks`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.CLOSED
        }
        .submitTaskSchedule("schedule2") {
          it.start = schedule2Start.toEpochMilli()
          it.end = schedule2End.toEpochMilli()
        }
        .submitTask("task3") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.ACCEPTED
        }
        .submitTaskSchedule("schedule3") {
          it.start = schedule2Start.toEpochMilli()
          it.end = schedule2End.toEpochMilli()
        }
    projectEventStoreUtils.reset()

    val criteria = SearchTasksDto(projectIdentifier = getIdentifier("project").asProjectId())

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1) = findSchedulesOfTasksInOrder("task")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task").asTaskId())
    assertThat(reschedule.failed)
        .containsExactlyInAnyOrder(
            getIdentifier("task2").asTaskId(), getIdentifier("task3").asTaskId())

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  @Test
  fun `reschedule fail for tasks with an unmovable daycards`() {
    val schedule2Start = now()
    val schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .submitTask("task2")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = DONE })
        .submitTask("task3")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule3",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task3", schedule2Start) { it.status = NOTDONE })
        .submitTask("task4")
        .submitTaskScheduleWithDayCards(
            asReference = "schedule4",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task4", schedule2Start) { it.status = APPROVED })
    projectEventStoreUtils.reset()

    val criteria = SearchTasksDto(projectIdentifier = getIdentifier("project").asProjectId())

    val validation = cut.validate(criteria = criteria)
    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()

    val reschedule = cut.reschedule(shiftDays = 2, criteria = criteria)
    val (schedule1) = findSchedulesOfTasksInOrder("task")

    // assert that only one task was moved successful.
    assertThat(reschedule).isEqualTo(validation)
    assertThat(reschedule.successful).containsExactlyInAnyOrder(getIdentifier("task").asTaskId())
    assertThat(reschedule.failed)
        .containsExactlyInAnyOrder(
            getIdentifier("task2").asTaskId(),
            getIdentifier("task3").asTaskId(),
            getIdentifier("task4").asTaskId())

    // assert schedule1 shifted +2 days
    assertThat(schedule1).hasStart(schedule1Start.plusDays(2))
    assertThat(schedule1).hasEnd(schedule1End.plusDays(2))

    // assert that events sent
    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateAggregate(it) }
  }

  private fun findSchedulesOfTasksInOrder(vararg taskReferences: String) =
      repositories.taskScheduleRepository
          .findWithDetailsByTaskIdentifierIn(taskReferences.map { getIdentifier(it).asTaskId() })
          .sortedBy {
            // make sure returned schedules are in the same order as the taskReferences
            taskReferences.map { getIdentifier(it).asTaskScheduleId() }.indexOf(it.identifier)
          }

  private fun AbstractObjectAssert<*, TaskSchedule>.hasStart(date: LocalDate) =
      extracting { it.start }.isEqualTo(date)

  private fun ObjectAssert<TaskSchedule>.hasEnd(date: LocalDate) =
      extracting { it.end }.isEqualTo(date)

  private fun ObjectAssert<TaskSchedule>.hasDayCardAt(date: LocalDate, dayCardIdentifier: UUID) =
      extracting { it.getDayCard(date)!!.identifier }.isEqualTo(dayCardIdentifier.asDayCardId())

  private fun ObjectAssert<TaskSchedule>.hasNoDayCardAt(date: LocalDate) =
      extracting { it.getDayCard(date) }.isNull()

  private fun verifyUpdateAggregate(events: Collection<TaskScheduleEventAvro>) {
    for (event in events) {
      val aggregate = event.aggregate
      val schedule =
          repositories.findTaskScheduleWithDetails(aggregate.getIdentifier().asTaskScheduleId())!!
      verifyUpdatedAggregate(aggregate, schedule)
    }
  }

  private fun verifyUpdatedAggregate(aggregate: TaskScheduleAggregateAvro, schedule: TaskSchedule) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, schedule, ProjectmanagementAggregateTypeEnum.TASKSCHEDULE)
        assertThat(aggregate.start).isEqualTo(schedule.start!!.toEpochMilli())
        assertThat(aggregate.end).isEqualTo(schedule.end!!.toEpochMilli())
        assertThat(aggregate.task.identifier).isEqualTo(schedule.task.identifier.toString())
        verifyScheduleSlots(aggregate.slots, schedule.slots!!)
      }

  private fun verifyScheduleSlots(
      scheduleSlotsAvro: Collection<TaskScheduleSlotAvro>,
      scheduleSlots: Collection<TaskScheduleSlot>
  ) {
    val identifierToDateAvro = scheduleSlotsAvro.associate { it.dayCard.identifier!! to it.date }
    val identifierToDate =
        scheduleSlots.associate { it.dayCard.identifier.toString() to it.date.toEpochMilli() }

    assertThat(scheduleSlotsAvro.size).isEqualTo(scheduleSlots.size)
    assertThat(identifierToDateAvro).containsAllEntriesOf(identifierToDate)
  }

  private fun EventStreamGenerator.submitTaskScheduleWithDayCards(
      asReference: String,
      start: LocalDate = now(),
      end: LocalDate = now(),
      vararg dayCards: DayCardToSubmit
  ) =
      submitTaskSchedule(asReference) {
            it.start = start.toEpochMilli()
            it.end = end.toEpochMilli()
          }
          .also {
            dayCards.forEach { (reference, _, aggregateModifications) ->
              submitDayCardG2(reference, aggregateModifications = aggregateModifications)
            }
          }
          .submitTaskSchedule(asReference, eventType = UPDATED) {
            it.slots =
                dayCards.map { (reference, date, _) -> getByReference(reference).asSlot(date) }
          }

  private fun WorkAreaId.toWorkAreaIdOrEmpty() = WorkAreaIdOrEmpty(this)

  private fun AbstractLongAssert<*>.isCloseToNow() =
      this.isCloseTo(LocalDateTime.now().toEpochMilli(), Offset.offset(10_000))

  private fun AbstractStringAssert<*>.isIdentifierOf(reference: String) =
      this.isEqualTo(getIdentifier(reference).toString())

  private data class DayCardToSubmit(
      val asReference: String,
      val date: LocalDate,
      val aggregateModifications: ((DayCardAggregateG2Avro.Builder) -> Unit)
  )
}
