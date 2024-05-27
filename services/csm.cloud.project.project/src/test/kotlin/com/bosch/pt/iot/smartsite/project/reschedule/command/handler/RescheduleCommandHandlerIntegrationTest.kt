/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.reschedule.messages.ProjectRescheduleFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.reschedule.messages.ProjectRescheduleStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.TypesFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleSlot
import java.time.LocalDate
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
class RescheduleCommandHandlerIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: RescheduleCommandHandler

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  // Properties used in the shift operation
  private val shiftDays by lazy { 2L }
  private val taskCriteria by lazy {
    SearchTasksDto(
        projectIdentifier = projectIdentifier, taskStatus = listOf(TaskStatusEnum.STARTED))
  }
  private val milestoneCriteria by lazy {
    SearchMilestonesDto(
        projectIdentifier = projectIdentifier,
        typesFilter = TypesFilterDto(types = setOf(MilestoneTypeEnum.INVESTOR)))
  }

  // Variables used to compare the schedules after the operation
  private lateinit var schedule1Start: LocalDate
  private lateinit var schedule1End: LocalDate
  private lateinit var schedule2Start: LocalDate
  private lateinit var schedule2End: LocalDate

  @BeforeEach
  fun init() {
    schedule2Start = LocalDate.now()
    schedule2End = schedule2Start.plusDays(2)

    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask("task2") {
          it.assignee = getByReference("participant")
          it.status = STARTED
        }
        .submitTaskScheduleWithDayCards(
            asReference = "schedule2",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1task2", schedule2Start) { it.status = OPEN },
            DayCardToSubmit("dayCard2task2", schedule2End) { it.status = OPEN })
        .submitTask(asReference = "closedTask") {
          it.assignee = getByReference("participant")
          it.status = STARTED
        }
        .submitTaskScheduleWithDayCards(
            asReference = "closedTaskSchedule",
            start = schedule2Start,
            end = schedule2End,
            DayCardToSubmit("dayCard1ClosedTask", schedule2Start) { it.status = NOTDONE },
            DayCardToSubmit("dayCard2ClosedTask", schedule2End) { it.status = APPROVED })
        .submitMilestonesWithList(
            listReference = "list1",
            date = schedule2Start.plusDays(1),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR),
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT, craft = getByReference("projectCraft")),
                    SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.PROJECT),
                ))
        .submitMilestonesWithList(
            listReference = "list2",
            date = schedule2Start.plusDays(2),
            milestones = listOf(SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR)))

    with(get<TaskScheduleAggregateAvro>("taskSchedule")!!) {
      schedule1Start = start.toLocalDateByMillis()
      schedule1End = end.toLocalDateByMillis()
    }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify handle reschedule works as expected`() {
    val result =
        cut.handle(
            RescheduleCommand(
                shiftDays = shiftDays,
                useTaskCriteria = true,
                useMilestoneCriteria = true,
                taskCriteria = taskCriteria,
                milestoneCriteria = milestoneCriteria,
                projectIdentifier = projectIdentifier))
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")
    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result from the operation.
    assertThat(result).isNotNull
    assertThat(result.successful).isNotNull
    assertThat(result.successful.milestones)
        .containsExactlyInAnyOrder(list1Milestone0.identifier, list2Milestone0.identifier)
    assertThat(result.successful.tasks).containsExactly(getIdentifier("task2").asTaskId())
    assertThat(result.failed).isNotNull
    assertThat(result.failed.milestones).isEmpty()
    assertThat(result.failed.tasks).containsExactly(getIdentifier("closedTask").asTaskId())

    // assert schedule1 was not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert list1m0 and list2m0 shifted
    assertThat(list1Milestone0.date).isEqualTo(schedule2Start.plusDays(3))
    assertThat(list2Milestone0.date).isEqualTo(schedule2Start.plusDays(4))

    // assert list1m1 and list1m2 not shifted
    assertThat(list1Milestone1.date).isEqualTo(schedule2Start.plusDays(1))
    assertThat(list1Milestone2.date).isEqualTo(schedule2Start.plusDays(1))

    // assert that events
    projectEventStoreUtils.verifyContainsInSequence(
        ProjectRescheduleStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        ProjectRescheduleFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
          assertThat(it.shiftDays).isEqualTo(shiftDays)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateTaskScheduleAggregate(it) }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 2, false)
        .also { verifyUpdateMilestoneAggregate(it) }

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }
  }

  @Test
  fun `verify handle reschedule works as expected for useTaskCriteria false`() {
    val result =
        cut.handle(
            RescheduleCommand(
                shiftDays = shiftDays,
                useTaskCriteria = false,
                useMilestoneCriteria = true,
                taskCriteria = taskCriteria,
                milestoneCriteria = milestoneCriteria,
                projectIdentifier = projectIdentifier))
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")
    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result from the operation.
    assertThat(result).isNotNull
    assertThat(result.successful).isNotNull
    assertThat(result.successful.milestones)
        .containsExactlyInAnyOrder(list1Milestone0.identifier, list2Milestone0.identifier)
    assertThat(result.successful.tasks).isEmpty()
    assertThat(result.failed).isNotNull
    assertThat(result.failed.milestones).isEmpty()
    assertThat(result.failed.tasks).isEmpty()

    // assert schedule1 and schedule2 was not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)
    assertThat(schedule2).hasStart(schedule2Start)
    assertThat(schedule2).hasEnd(schedule2End)

    // assert list1m0 and list2m0 shifted
    assertThat(list1Milestone0.date).isEqualTo(schedule2Start.plusDays(3))
    assertThat(list2Milestone0.date).isEqualTo(schedule2Start.plusDays(4))

    // assert list1m1 and list1m2 not shifted
    assertThat(list1Milestone1.date).isEqualTo(schedule2Start.plusDays(1))
    assertThat(list1Milestone2.date).isEqualTo(schedule2Start.plusDays(1))

    // assert that events
    projectEventStoreUtils.verifyContainsInSequence(
        ProjectRescheduleStartedEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        MilestoneListEventAvro::class.java,
        ProjectRescheduleFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
          assertThat(it.shiftDays).isEqualTo(shiftDays)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.UPDATED, 2, false)
        .also { verifyUpdateMilestoneAggregate(it) }

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }
  }

  @Test
  fun `verify handle reschedule works as expected for useMilestoneCriteria false`() {
    val result =
        cut.handle(
            RescheduleCommand(
                shiftDays = shiftDays,
                useTaskCriteria = true,
                useMilestoneCriteria = false,
                taskCriteria = taskCriteria,
                milestoneCriteria = milestoneCriteria,
                projectIdentifier = projectIdentifier))
    val (schedule1, schedule2) = findSchedulesOfTasksInOrder("task", "task2")
    val (list1Milestone0, list1Milestone1, list1Milestone2, list2Milestone0) =
        findMilestonesInOrder("list1M0", "list1M1", "list1M2", "list2M0")

    // assert result from the operation.
    assertThat(result).isNotNull
    assertThat(result.successful).isNotNull
    assertThat(result.successful.milestones).isEmpty()
    assertThat(result.successful.tasks).containsExactly(getIdentifier("task2").asTaskId())
    assertThat(result.failed).isNotNull
    assertThat(result.failed.milestones).isEmpty()
    assertThat(result.failed.tasks).containsExactly(getIdentifier("closedTask").asTaskId())

    // assert schedule1 was not shifted
    assertThat(schedule1).hasStart(schedule1Start)
    assertThat(schedule1).hasEnd(schedule1End)

    // assert schedule2 shifted +2 days
    assertThat(schedule2).hasStart(schedule2Start.plusDays(2))
    assertThat(schedule2).hasEnd(schedule2End.plusDays(2))
    assertThat(schedule2).hasDayCardAt(schedule2Start.plusDays(2), getIdentifier("dayCard1task2"))
    assertThat(schedule2).hasNoDayCardAt(schedule2Start.plusDays(3))
    assertThat(schedule2).hasDayCardAt(schedule2End.plusDays(2), getIdentifier("dayCard2task2"))

    // assert all milestone were not shifted
    assertThat(list1Milestone0.date).isEqualTo(schedule2Start.plusDays(1))
    assertThat(list1Milestone1.date).isEqualTo(schedule2Start.plusDays(1))
    assertThat(list1Milestone2.date).isEqualTo(schedule2Start.plusDays(1))
    assertThat(list2Milestone0.date).isEqualTo(schedule2Start.plusDays(2))

    // assert that events
    projectEventStoreUtils.verifyContainsInSequence(
        ProjectRescheduleStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        ProjectRescheduleFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
          assertThat(it.shiftDays).isEqualTo(shiftDays)
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
        .also { verifyUpdateTaskScheduleAggregate(it) }

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectRescheduleFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.auditingInformation.date).isCloseToNow()
          assertThat(it.auditingInformation.user).isIdentifierOf("userCsm2")
        }
  }

  private fun findSchedulesOfTasksInOrder(vararg taskReferences: String) =
      repositories.taskScheduleRepository
          .findWithDetailsByTaskIdentifierIn(taskReferences.map { getIdentifier(it).asTaskId() })
          .sortedBy { taskSchedule ->
            // make sure returned schedules are in the same order as the taskReferences
            taskReferences
                .map { getIdentifier(it).asTaskScheduleId() }
                .indexOf(taskSchedule.identifier)
          }

  private fun findMilestonesInOrder(vararg milestoneReferences: String) =
      repositories.milestoneRepository
          .findAllWithDetailsByIdentifierIn(
              milestoneReferences.map { getIdentifier(it).asMilestoneId() })
          .sortedBy {
            // make sure returned milestones are in the same order as the milestoneReferences
            milestoneReferences.map { getIdentifier(it).asMilestoneId() }.indexOf(it.identifier)
          }

  private fun verifyUpdateTaskScheduleAggregate(events: Collection<TaskScheduleEventAvro>) {
    for (event in events) {
      val aggregate = event.aggregate
      val schedule =
          repositories.findTaskScheduleWithDetails(aggregate.getIdentifier().asTaskScheduleId())!!
      verifyUpdatedAggregate(aggregate, schedule)
    }
  }

  private fun verifyUpdateMilestoneAggregate(events: Collection<MilestoneEventAvro>) {
    for (event in events) {
      val aggregate = event.aggregate
      val milestone =
          repositories.findMilestoneWithDetails(aggregate.getIdentifier().asMilestoneId())
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, milestone, ProjectmanagementAggregateTypeEnum.MILESTONE)
        assertThat(aggregate.date).isEqualTo(milestone.date.toEpochMilli())
      }
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

  private data class DayCardToSubmit(
      val asReference: String,
      val date: LocalDate,
      val aggregateModifications: ((DayCardAggregateG2Avro.Builder) -> Unit)
  )

  private fun EventStreamGenerator.submitTaskScheduleWithDayCards(
      asReference: String,
      start: LocalDate = LocalDate.now(),
      end: LocalDate = LocalDate.now(),
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

  private fun AbstractObjectAssert<*, TaskSchedule>.hasStart(date: LocalDate) =
      extracting { it.start }.isEqualTo(date)

  private fun ObjectAssert<TaskSchedule>.hasEnd(date: LocalDate) =
      extracting { it.end }.isEqualTo(date)

  private fun ObjectAssert<TaskSchedule>.hasDayCardAt(date: LocalDate, dayCardIdentifier: UUID) =
      extracting { it.getDayCard(date)!!.identifier }.isEqualTo(dayCardIdentifier.asDayCardId())

  private fun ObjectAssert<TaskSchedule>.hasNoDayCardAt(date: LocalDate) =
      extracting { it.getDayCard(date) }.isNull()

  private fun AbstractLongAssert<*>.isCloseToNow() =
      this.isCloseTo(LocalDateTime.now().toEpochMilli(), Offset.offset(10_000))

  private fun AbstractStringAssert<*>.isIdentifierOf(reference: String) =
      this.isEqualTo(getIdentifier(reference).toString())
}
