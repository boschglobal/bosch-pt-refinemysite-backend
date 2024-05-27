/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.boundary.dto

import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.model.AssigneesCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneTypes
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.WorkAreas
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto

data class QuickFilterDto(
    val name: String,
    val highlight: Boolean,
    val useMilestoneCriteria: Boolean,
    val useTaskCriteria: Boolean,
    val taskCriteria: SearchTasksDto,
    val milestoneCriteria: SearchMilestonesDto,
    val projectRef: ProjectId
) {

  fun toDocument(participantIdentifier: ParticipantId) =
      QuickFilter(
          identifier = QuickFilterId(),
          name = name,
          participantIdentifier = participantIdentifier,
          projectIdentifier = projectRef,
          highlight = highlight,
          useTaskCriteria = useTaskCriteria,
          useMilestoneCriteria = useMilestoneCriteria,
          milestoneCriteria =
              MilestoneCriteria(
                  from = milestoneCriteria.from,
                  to = milestoneCriteria.to,
                  workAreas =
                      WorkAreas(
                          header = milestoneCriteria.workAreas.header,
                          workAreaIds = milestoneCriteria.workAreas.workAreaIdentifiers),
                  milestoneTypes =
                      MilestoneTypes(
                          types = milestoneCriteria.typesFilter.types,
                          projectCraftIds = milestoneCriteria.typesFilter.craftIdentifiers)),
          taskCriteria =
              TaskCriteria(
                  from = taskCriteria.rangeStartDate,
                  to = taskCriteria.rangeEndDate,
                  workAreaIds = taskCriteria.workAreaIdentifiers.toSet(),
                  projectCraftIds = taskCriteria.projectCraftIdentifiers.toSet(),
                  allDaysInDateRange = taskCriteria.allDaysInDateRange,
                  status = taskCriteria.taskStatus.toSet(),
                  assignees =
                      AssigneesCriteria(
                          participantIds = taskCriteria.assignees.participantIdentifiers.toSet(),
                          companyIds = taskCriteria.assignees.companyIdentifiers.toSet()),
                  hasTopics = taskCriteria.hasTopics,
                  topicCriticality = taskCriteria.topicCriticality.toSet()))
}
