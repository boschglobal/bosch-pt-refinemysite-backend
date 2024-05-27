/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_COPY
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureQueryService
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.command.dto.CopiedProjectResult
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import datadog.trace.api.Trace
import io.opentracing.util.GlobalTracer
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectCopyService(
    private val featureQueryService: FeatureQueryService,
    private val genericExporter: GenericExporter,
    private val genericImporter: GenericImporter,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
) {

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun isCopyPossible(project: Project): Boolean =
      featureQueryService.isFeatureEnabled(PROJECT_COPY, project.identifier)

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasCopyPermissionOnProject(#projectId)")
  open fun copy(
      projectId: ProjectId,
      projectCopyParameters: ProjectCopyParameters
  ): CopiedProjectResult {

    // step 1 - export source project
    val exportSettings = projectCopyParameters.createExportSettings()
    val sourceProject = genericExporter.export(projectId, exportSettings)

    // step 2 - replace project id and title
    val sourceProjectWithNewIdAndTitle =
        sourceProject.copy(identifier = ProjectId(), title = projectCopyParameters.projectName)

    addInfosToTrace(projectCopyParameters, sourceProject, sourceProjectWithNewIdAndTitle)

    // step 3 - remove inactive task assignees, leaving affected tasks unassigned
    val sourceWithoutInactiveAssignees = removeInactiveTaskAssignees(sourceProjectWithNewIdAndTitle)

    // step 4 - import the copy
    businessTransactionManager.doCopyProjectInBusinessTransaction(
        sourceWithoutInactiveAssignees.identifier) {
          genericImporter.import(sourceWithoutInactiveAssignees, ProjectCopyMergeStrategy())
        }

    return CopiedProjectResult(
        sourceWithoutInactiveAssignees.identifier, sourceWithoutInactiveAssignees.title)
  }

  private fun removeInactiveTaskAssignees(
      project: ProjectDto,
  ): ProjectDto {
    val activeParticipants =
        project.participants.filterIsInstance<ActiveParticipantDto>().map { it.identifier }.toSet()

    return project.copy(
        tasks =
            project.tasks.map {
              it.copy(assignee = if (it.assignee in activeParticipants) it.assignee else null)
            })
  }

  private fun addInfosToTrace(
      projectCopyParameters: ProjectCopyParameters,
      sourceProject: ProjectDto,
      sourceProjectWithNewIdAndTitle: ProjectDto
  ) {
    GlobalTracer.get()
        .activeSpan()
        .setTag("custom.originalProject.id", sourceProject.identifier.toString())
        .setTag("custom.originalProject.name", sourceProject.title)
        .setTag(
            "custom.originalProject.numberOfActiveParticipants", sourceProject.participants.size)
        .setTag("custom.originalProject.numberOfWorkAreas", sourceProject.workAreas.size)
        .setTag("custom.originalProject.numberOfProjectCrafts", sourceProject.projectCrafts.size)
        .setTag("custom.originalProject.numberOfMilestones", sourceProject.milestones.size)
        .setTag("custom.originalProject.numberOfTasks", sourceProject.tasks.size)
        .setTag("custom.originalProject.numberOfRelations", sourceProject.relations.size)
        .setTag("custom.copiedProject.id", sourceProjectWithNewIdAndTitle.identifier.toString())
        .setTag("custom.copiedProject.name", sourceProjectWithNewIdAndTitle.title)
        .setTag(
            "custom.copiedProject.firstCsmId",
            if (sourceProjectWithNewIdAndTitle.participants.isNotEmpty())
                sourceProjectWithNewIdAndTitle.participants.first().identifier.toString()
            else "none")
        .setTag("custom.copyParameters.workAreas", projectCopyParameters.workingAreas)
        .setTag("custom.copyParameters.projectCrafts", projectCopyParameters.disciplines.toString())
        .setTag("custom.copyParameters.milestones", projectCopyParameters.milestones.toString())
        .setTag("custom.copyParameters.tasks", projectCopyParameters.tasks.toString())
        .setTag("custom.copyParameters.dayCards", projectCopyParameters.dayCards.toString())
        .setTag(
            "custom.copyParameters.keepTaskStatus", projectCopyParameters.keepTaskStatus.toString())
        .setTag(
            "custom.copyParameters.keepTaskAssignee",
            projectCopyParameters.keepTaskAssignee.toString())
  }
}

data class ProjectCopyParameters(
    val projectName: String,
    val workingAreas: Boolean = false,
    /** Disciplines are Crafts */
    val disciplines: Boolean = false,
    val milestones: Boolean = false,
    val tasks: Boolean = false,
    val dayCards: Boolean = false,
    val keepTaskStatus: Boolean = false,
    val keepTaskAssignee: Boolean = false
) {

  init {
    require(projectName.isNotBlank()) { "Project name must not be blank" }
    if (dayCards) require(tasks) { "Tasks are required to be copied in order to copy DayCards." }
    if (keepTaskAssignee)
        require(tasks) { "Tasks are required to be copied in order to copy its Assignee." }
    if (keepTaskStatus)
        require(keepTaskAssignee) {
          "Assignees are required to be copied in order to copy TaskStatus."
        }
  }

  fun createExportSettings(): ExportSettings =
      ExportSettings(
          exportParticipants = keepTaskAssignee,
          exportCrafts = disciplines,
          exportWorkAreas = workingAreas,
          exportTasks = tasks,
          exportTaskStatus = tasks && keepTaskStatus,
          exportMilestones = milestones,
          exportDayCards = dayCards,
          exportRelations = milestones || tasks,
          /** SMAR-18109: Generally disabled, because author and timestamp can't be copied */
          exportTopics = false)
}
