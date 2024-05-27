/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsForDifferentTasks
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInDifferentWeeks
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsInSameWeek
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitDayCardsWithState
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitScheduleWithDayCards
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.submitTaskScheduleWithDayCardsG2
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectRelation
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.NamedObjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class DayCardPpcCalculationStatisticTest : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var controller: StatisticsController
  @Autowired private lateinit var participantMappingRepository: ParticipantMappingRepository
  @Autowired private lateinit var dayCardRepository: DayCardRepository
  @Autowired private lateinit var objectRelationRepository: ObjectRelationRepository
  @Autowired private lateinit var namedObjectRepository: NamedObjectRepository

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @DisplayName("DayCard Ppc Calculation dependent on the date")
  @Nested
  inner class DayCardPpcCalculationDateDependentTest {
    @Test
    fun `in same week`() {
      eventStreamGenerator.submitDayCardsInSameWeek(startDate)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isNotNull.isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
    }

    @Test
    fun `in same week with different tasks`() {
      eventStreamGenerator.submitDayCardsForDifferentTasks(startDate)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
    }

    @Test
    fun `in adjacent weeks`() {
      eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 1)

      val statistics = controller.calculatePpc(project, startDate, 2).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(2)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
      assertThat(statistics.series[1].metrics.ppc!!).isEqualTo(50)
    }

    @Test
    fun `in non-adjacent weeks`() {
      eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 2)

      val statistics = controller.calculatePpc(project, startDate, 3).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(3)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
      assertThat(statistics.series[2].metrics.ppc!!).isEqualTo(50)
    }

    @Test
    fun `start day of the week is friday in non-adjacent weeks`() {
      val startDateWeekOne = LocalDate.ofYearDay(2018, 5) // friday 5. january
      val startDateWeekTwo = LocalDate.ofYearDay(2018, 12) // friday 12. january

      val dayCards =
          mapOf(
              startDateWeekOne to Pair(DONE, null), // friday 5. january
              startDateWeekOne.plusDays(7) to Pair(OPEN, null), // friday 12. january

              startDateWeekTwo.plusDays(1) to Pair(DONE, null), // saturday 13. january
              startDateWeekTwo.plusDays(2) to Pair(DONE, null), // sunday 14. january
              startDateWeekTwo.plusDays(3) to Pair(DONE, null)) // monday 15. january

      eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

      val statistics = controller.calculatePpc(project, startDateWeekOne, 4).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(80) // 4/5 DONE
      assertThat(statistics.series).hasSize(4)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(100)
      assertThat(statistics.series[1].metrics.ppc!!).isEqualTo(75)
      assertThat(statistics.series[2].metrics.ppc).isNull()
      assertThat(statistics.series[3].metrics.ppc).isNull()
    }

    @Test
    fun `in different years`() {
      eventStreamGenerator.submitDayCardsInDifferentWeeks(startDate, 53)

      val statistics = controller.calculatePpc(project, startDate, 54).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(54)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
      assertThat(statistics.series[53].metrics.ppc!!).isEqualTo(50)
    }

    @Test
    fun `truncates decimal places`() {
      val startDate = LocalDate.now()
      val dayCards =
          mapOf(
              startDate to Pair(DONE, null),
              startDate.plusDays(2) to Pair(NOTDONE, null),
              startDate.plusDays(3) to Pair(OPEN, null))
      eventStreamGenerator.submitTaskScheduleWithDayCardsG2("task-1", dayCards)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(33)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(33)
    }
  }

  @DisplayName("DayCard Ppc Calculation dependent on the state")
  @Nested
  inner class DayCardPpcCalculationStateDependentTest {
    @Test
    fun `with state done`() {
      eventStreamGenerator.submitDayCardsWithState(startDate, DONE)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(100)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(100)
    }

    @Test
    fun `with state approved`() {
      eventStreamGenerator.submitDayCardsWithState(startDate, APPROVED)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(100)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(100)
    }

    @Test
    fun `with state open`() {
      eventStreamGenerator.submitDayCardsWithState(startDate, OPEN)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(0)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(0)
    }

    @Test
    fun `with state not done`() {
      eventStreamGenerator.submitDayCardsWithState(startDate, NOTDONE)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(0)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(0)
    }
  }

  @DisplayName("DayCard Ppc Calculation dependent on the daycard")
  @Nested
  inner class DayCardPpcCalculationDayCardDependentTest {
    @Test
    fun `with no dayCards in the duration`() {
      val startDate = LocalDate.now()
      eventStreamGenerator.submitDayCardsInSameWeek(startDate.plusWeeks(4))

      val statistics = controller.calculatePpc(project, LocalDate.now(), 2).items[0]

      assertThat(statistics.series.size).isEqualTo(2)
      assertThat(statistics.series.size).isEqualTo(2)
      assertThat(statistics.series[0].metrics.ppc).isNull()
      assertThat(statistics.series[1].metrics.ppc).isNull()
      assertThat(statistics.totals.ppc).isNull()
    }

    @Test
    fun `with no daycards at all`() {
      val startDate = LocalDate.now()
      eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
          "task-1", emptyMap(), startDate, startDate.plusWeeks(3))

      val statistics = controller.calculatePpc(project, LocalDate.now(), 2).items[0]

      assertThat(statistics.series.size).isEqualTo(2)
      assertThat(statistics.series[0].metrics.ppc).isNull()
      assertThat(statistics.series[1].metrics.ppc).isNull()
      assertThat(statistics.totals.ppc).isNull()
    }
  }

  @DisplayName("DayCard Ppc Calculation dependent on delete")
  @Nested
  inner class DayCardPpcCalculationDeleteDependentTest {
    @Test
    fun `after one daycard has been deleted`() {
      val startDate = LocalDate.now()
      eventStreamGenerator.submitScheduleWithDayCards("task-1", startDate)

      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)

      eventStreamGenerator.submitDayCardG2(
          asReference = "task-1-daycard-0", eventType = DayCardEventEnumAvro.DELETED) {
            it.status = OPEN
            it.task = eventStreamGenerator.get<TaskAggregateAvro>("task-1")!!.aggregateIdentifier
          }

      val statisticsAfterDelete = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statisticsAfterDelete.totals.ppc!!).isEqualTo(100)
      assertThat(statisticsAfterDelete.series).hasSize(1)
      assertThat(statisticsAfterDelete.series[0].metrics.ppc!!).isEqualTo(100)
    }

    @Test
    fun `after a task has been deleted`() {
      eventStreamGenerator
          .submitTask(asReference = "another-task-2") {
            it.craft = craft1
            it.assignee = participantFmA
          }
          .submitTask(asReference = "another-task-3") {
            it.craft = craft1
            it.assignee = participantFmA
          }
          .submitTask(asReference = "another-task-4") {
            it.craft = craft1
            it.assignee = participantFmA
          }

      val startDate = LocalDate.now()

      // Create a tasks schedule with one dayCard with status open and one closed for 3 tasks
      eventStreamGenerator.submitScheduleWithDayCards("task-1", startDate)
      eventStreamGenerator.submitScheduleWithDayCards("another-task-2", startDate)
      eventStreamGenerator.submitScheduleWithDayCards("another-task-3", startDate)

      // Create a schedule with one dayCard with status open
      val dayCards = mapOf(startDate.plusDays(1) to Pair(OPEN, null))
      eventStreamGenerator.submitTaskScheduleWithDayCardsG2(
          "another-task-4", dayCards, startDate, startDate.plusWeeks(1))

      // Calculate and verify statistics before data is deleted
      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(42)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(42)

      // Delete data
      eventStreamGenerator.submitTask(
          asReference = "another-task-4", eventType = TaskEventEnumAvro.DELETED) {
            it.craft = craft1
            it.assignee = participantFmA
          }

      // Verify that project statistics is correct after deletion
      val statisticsAfterDelete = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statisticsAfterDelete.totals.ppc!!).isEqualTo(50)
      assertThat(statisticsAfterDelete.series).hasSize(1)
      assertThat(statisticsAfterDelete.series[0].metrics.ppc!!).isEqualTo(50)

      // Verify that referenced data is deleted
      val taskObjectIdentifier =
          ObjectIdentifier(
              TASK,
              eventStreamGenerator
                  .get<TaskAggregateAvro>("another-task-4")!!
                  .aggregateIdentifier
                  .identifier
                  .toUUID())

      assertThat(dayCardRepository.findAllByTaskIdentifier(taskObjectIdentifier.identifier))
          .isEmpty()
      assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT))
          .isNull()
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(
                  taskObjectIdentifier, PROJECTCRAFT))
          .isNull()
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PARTICIPANT))
          .isNull()
    }

    @Test
    fun `after a project has been deleted`() {
      val startDate = LocalDate.now()

      // Create a tasks schedule with one day card with status open
      eventStreamGenerator.submitScheduleWithDayCards("task-1", startDate)

      // Calculate and verify statistics before data is deleted
      val statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)

      val projectObjectIdentifier = ObjectIdentifier(PROJECT, project.toUUID())

      // Check that expected data can be found in the database
      val projectCrafts =
          getLeftObjectIdentifiers(
              objectRelationRepository.findAllByLeftTypeAndRight(
                  PROJECTCRAFT, projectObjectIdentifier))
      assertThat(projectCrafts).isNotEmpty
      assertThat(namedObjectRepository.findAllByObjectIdentifierIn(projectCrafts)).isNotEmpty
      assertThat(objectRelationRepository.findAllByLeftTypeAndRight(TASK, projectObjectIdentifier))
          .hasSize(3)
      val participants =
          getLeftObjectIdentifiers(
              objectRelationRepository.findAllByLeftTypeAndRight(
                  PARTICIPANT, projectObjectIdentifier))
      assertThat(participants).isNotEmpty
      assertThat(objectRelationRepository.findAllByLeftInAndRightType(participants, EMPLOYEE))
          .isNotEmpty

      val taskObjectIdentifier =
          ObjectIdentifier(
              TASK,
              eventStreamGenerator
                  .get<TaskAggregateAvro>("task-1")!!
                  .aggregateIdentifier
                  .identifier
                  .toUUID())

      assertThat(dayCardRepository.findAllByTaskIdentifier(taskObjectIdentifier.identifier))
          .isNotEmpty
      assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT))
          .isNotNull
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(
                  taskObjectIdentifier, PROJECTCRAFT))
          .isNotNull
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PARTICIPANT))
          .isNotNull
      assertThat(
              participantMappingRepository.findAllByProjectIdentifier(
                  projectObjectIdentifier.identifier))
          .isNotEmpty

      // Delete data
      eventStreamGenerator.submitProject(eventType = ProjectEventEnumAvro.DELETED)

      // Verify that project statistics is correct after deletion
      val statisticsAfterDelete = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statisticsAfterDelete.totals.ppc).isNull()
      assertThat(statisticsAfterDelete.series[0].metrics.ppc).isNull()

      // Verify that referenced data is deleted
      assertThat(
              objectRelationRepository.findAllByLeftTypeAndRight(
                  PROJECTCRAFT, projectObjectIdentifier))
          .isEmpty()
      assertThat(namedObjectRepository.findAllByObjectIdentifierIn(projectCrafts)).isEmpty()
      assertThat(objectRelationRepository.findAllByLeftTypeAndRight(TASK, projectObjectIdentifier))
          .isEmpty()
      assertThat(
              objectRelationRepository.findAllByLeftTypeAndRight(
                  PARTICIPANT, projectObjectIdentifier))
          .isEmpty()
      assertThat(objectRelationRepository.findAllByLeftInAndRightType(participants, EMPLOYEE))
          .isEmpty()

      assertThat(dayCardRepository.findAllByTaskIdentifier(taskObjectIdentifier.identifier))
          .isEmpty()
      assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT))
          .isNull()
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(
                  taskObjectIdentifier, PROJECTCRAFT))
          .isNull()
      assertThat(
              objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PARTICIPANT))
          .isNull()
      assertThat(
              participantMappingRepository.findAllByProjectIdentifier(
                  projectObjectIdentifier.identifier))
          .isEmpty()
    }

    private fun getLeftObjectIdentifiers(relations: List<ObjectRelation>): List<ObjectIdentifier> =
        relations.map { r -> r.left }
  }

  @DisplayName("DayCard Ppc Calculation dependent on participant")
  @Nested
  inner class DayCardPpcCalculationParticipantDependentTest {
    @Test
    fun `after a participant has been deactivated and reactivated`() {
      val startDate = LocalDate.now()

      // Create a tasks schedule with one day card with status open
      eventStreamGenerator.submitScheduleWithDayCards("task-1", startDate)

      // Calculate and verify statistics while participant is active
      var statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)

      eventStreamGenerator.submitParticipantG3(
          asReference = "participant-fm-a",
          eventType = ParticipantEventEnumAvro.DEACTIVATED,
          aggregateModifications = {
            it.company = companyA
            it.user = employeeFmA
          })

      // Calculate and verify statistics while participant is deactivated
      statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)

      eventStreamGenerator.submitParticipantG3(
          asReference = "participant-fm-a",
          eventType = ParticipantEventEnumAvro.REACTIVATED,
          aggregateModifications = {
            it.company = companyA
            it.user = employeeFmA
          })

      // Calculate and verify statistics after participant was reactivated
      statistics = controller.calculatePpc(project, startDate, 1).items[0]

      assertThat(statistics.totals.ppc!!).isEqualTo(50)
      assertThat(statistics.series).hasSize(1)
      assertThat(statistics.series[0].metrics.ppc!!).isEqualTo(50)
    }
  }
}
