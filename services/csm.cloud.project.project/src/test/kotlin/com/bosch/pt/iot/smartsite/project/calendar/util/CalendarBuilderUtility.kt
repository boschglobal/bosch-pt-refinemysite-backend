/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.util

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.NB
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.user.model.User
import java.math.BigDecimal
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime

object CalendarBuilderUtility {

  val START_DATE: LocalDate = LocalDate.of(2019, 10, 9)
  val END_DATE: LocalDate = START_DATE.plusDays(6)

  fun buildProject(block: (Project) -> Unit = {}) =
      Project()
          .apply {
            identifier = ProjectId()
            setCreatedDate(LocalDateTime.now())
            setLastModifiedDate(LocalDateTime.now())
            setCreatedBy(UserId())
            setLastModifiedBy(UserId())
            start = START_DATE
            end = END_DATE
            client = "client"
            description = "description"
            projectNumber = "projectNumber"
            title = "title"
            category = NB
            projectAddress = null
            participants = null
          }
          .apply(block)

  fun buildParticipant(block: (Participant) -> Unit = {}) =
      Participant(buildProject(), buildCompany { it.name = "Company" }, buildUser(), CSM).apply {
        identifier = ParticipantId()
        block(this)
      }

  fun buildWorkdayConfiguration(block: (WorkdayConfiguration) -> Unit = {}) =
      with(buildProject()) {
        WorkdayConfiguration(
                startOfWeek = WEDNESDAY,
                workingDays = mutableSetOf(WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY),
                holidays = mutableSetOf(),
                allowWorkOnNonWorkingDays = true,
                project = this)
            .apply {
              identifier = WorkdayConfigurationId()
              block(this)
            }
      }

  fun buildProjectCraft(block: (ProjectCraft) -> Unit = {}) =
      with(buildProject()) {
        ProjectCraft(project = this, name = "name", color = "color").apply {
          identifier = ProjectCraftId()
          block(this)
        }
      }

  fun buildMilestone(block: (Milestone) -> Unit = {}) =
      with(buildProject()) {
        Milestone(
                project = this,
                type = MilestoneTypeEnum.PROJECT,
                date = START_DATE,
                header = true,
                name = "milestone",
                description = "description",
                craft =
                    ProjectCraft(project = this, name = "ProjectCraft", color = "Black").apply {
                      identifier = ProjectCraftId()
                    },
                workArea = null)
            .apply {
              identifier = MilestoneId()
              block(this)
            }
      }

  fun buildTask(block: (Task) -> Unit = {}) =
      with(buildProject()) {
        Task(
                identifier = TaskId(),
                project = this,
                name = "Task",
                description = "description",
                location = "location",
                projectCraft =
                    ProjectCraft(project = this, name = "ProjectCraft", color = "Black").apply {
                      identifier = ProjectCraftId()
                    },
                status = TaskStatusEnum.OPEN)
            .apply {
              taskSchedule =
                  TaskSchedule(
                      task = this, start = START_DATE.plusDays(1), end = END_DATE.minusDays(1))
              block(this)
            }
      }

  fun buildTaskScheduleWithoutDayCardsDto(
      start: LocalDate = START_DATE,
      end: LocalDate = END_DATE,
      taskIdentifier: TaskId = TaskId(),
      block: (TaskScheduleWithoutDayCardsDto) -> Unit = {}
  ) =
      TaskScheduleWithoutDayCardsDto(
              identifier = TaskScheduleId(),
              taskProjectIdentifier = ProjectId(),
              taskIdentifier = taskIdentifier,
              version = 0L,
              taskName = "",
              start = start,
              end = end,
              createdByIdentifier = null,
              createdDate = null,
              lastModifiedByIdentifier = null,
              lastModifiedDate = null)
          .apply(block)

  fun buildTaskScheduleSlotWithDayCardDto(
      date: LocalDate = START_DATE,
      title: String = "DayCard",
      manpower: BigDecimal = BigDecimal("3.50"),
      status: DayCardStatusEnum = OPEN,
      block: (TaskScheduleSlotWithDayCardDto) -> Unit = {}
  ) =
      TaskScheduleSlotWithDayCardDto(
              identifier = TaskScheduleId(),
              slotsDate = date,
              slotsDayCardIdentifier = DayCardId(),
              slotsDayCardVersion = 0L,
              slotsDayCardTitle = title,
              slotsDayCardManpower = manpower,
              slotsDayCardNotes = "",
              slotsDayCardStatus = status,
              slotsDayCardReason = null,
              slotsDayCardCreatedBy = null,
              slotsDayCardCreatedDate = null,
              slotsDayCardLastModifiedBy = null,
              slotsDayCardLastModifiedDate = null)
          .apply(block)

  private fun buildUser(block: (User) -> Unit = {}) = User().apply(block)

  private fun buildCompany(block: (Company) -> Unit = {}) = Company().apply(block)
}
