/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.api

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class ExportCalendarAsCsvCommand(
    val locale: Locale,
    val projectIdentifier: ProjectId,
    val calendarExportParameters: CalendarExportParameters
)

data class ExportCalendarAsJsonCommand(
    val locale: Locale,
    val projectIdentifier: ProjectId,
    val calendarExportParameters: CalendarExportParameters
)

data class ExportCalendarAsPdfCommand(
    val locale: Locale,
    val projectIdentifier: ProjectId,
    val calendarExportParameters: CalendarExportParameters,
    // TODO: Check if we find a better solution here. Currently this is required for the PDF
    //  converter later to authorize against the callback endpoint
    val token: String
)

data class CalendarExportParameters(
    val assignees: AssigneesFilter = AssigneesFilter(emptyList(), emptyList()),
    val from: LocalDate,
    val to: LocalDate,
    val projectCraftIds: List<ProjectCraftId> = emptyList(),
    val workAreaIds: List<WorkAreaIdOrEmpty> = emptyList(),
    val status: List<TaskStatusEnum> = emptyList(),
    val topicCriticality: List<TopicCriticalityEnum> = emptyList(),
    val hasTopics: Boolean? = null,
    val includeDayCards: Boolean = false,
    val includeMilestones: Boolean = false,
    val allDaysInDateRange: Boolean? = null
)

data class AssigneesFilter(
    val participantIds: List<ParticipantId> = emptyList(),
    val companyIds: List<UUID> = emptyList()
)
