/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.milestone.authorization.MilestoneAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneController
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchController
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_CRAFT_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_INVESTOR_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_PROJECT_MILESTONE
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectConstructionSiteManagerDto
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.EMBEDDED_PROJECT_PICTURES
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.EMBEDDED_PROJECT_STATISTICS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_COMPANIES
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_DELETE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_MILESTONES
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PARTICIPANTS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PROJECT_CRAFTS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_PROJECT_WORKAREAS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_UPDATE_PROJECT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_WORKDAY_CONFIGURATION
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.NameByIdentifierDto
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController.Companion.CRAFTS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.factory.ProjectPictureResourceFactory
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.projectstatistics.boundary.ProjectStatisticsService
import com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest.resource.response.ProjectStatisticsResource
import com.bosch.pt.iot.smartsite.project.reschedule.authorization.RescheduleAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.RescheduleController
import com.bosch.pt.iot.smartsite.project.rfv.authorization.RfvAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.RfvController
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_UPDATE_RFV
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_SEARCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.authorization.TaskConstraintAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintController
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_UPDATE_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.WorkAreaController
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.WorkdayConfigurationController
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.WorkdayConfigurationController.Companion.WORKDAY_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import org.springframework.context.MessageSource
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Transactional(readOnly = true)
@Component
open class ProjectResourceFactoryHelper(
    messageSource: MessageSource,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val milestoneAuthorizationComponent: MilestoneAuthorizationComponent,
    private val rescheduleAuthorizationComponent: RescheduleAuthorizationComponent,
    private val rfvAuthorizationComponent: RfvAuthorizationComponent,
    private val taskConstraintAuthorizationComponent: TaskConstraintAuthorizationComponent,
    private val projectQueryService: ProjectQueryService,
    private val projectPictureResourceFactory: ProjectPictureResourceFactory,
    private val projectPictureService: ProjectPictureService,
    private val participantQueryService: ParticipantQueryService,
    private val projectStatisticsService: ProjectStatisticsService,
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(projects: Collection<Project>, includeEmbedded: Boolean): List<ProjectResource> {

    if (projects.isEmpty()) {
      return emptyList()
    }

    val auditUser = userService.findAuditUsers(projects)
    val projectIdentifiers = collectProjectIdentifiers(projects)
    val projectPictures = projectPictureService.findProjectPictures(projectIdentifiers)
    val hasReadPermission =
        projectAuthorizationComponent.hasReadPermissionOnProjects(projectIdentifiers)
    val updateProjectPermissions =
        projectAuthorizationComponent.getProjectsWithUpdatePermission(projectIdentifiers)
    val deleteProjectPermissions =
        projectAuthorizationComponent.getProjectsWithDeletePermission(projectIdentifiers)
    val createAnyMilestonePermissions =
        milestoneAuthorizationComponent.getProjectsWithCreateAnyMilestonePermissions(
            projectIdentifiers)
    val createCraftMilestonePermissions =
        milestoneAuthorizationComponent.getProjectsWithCreateCraftMilestonePermissions(
            projectIdentifiers)
    val updateRfvPermissions =
        rfvAuthorizationComponent.getProjectsWithUpdateRfvPermissions(projectIdentifiers)
    val updateConstraintsPermissions =
        taskConstraintAuthorizationComponent.getProjectsWithUpdateConstraintPermissions(
            projectIdentifiers)
    val reschedulePermissions =
        rescheduleAuthorizationComponent.getProjectsWithReschedulePermissions(projectIdentifiers)
    val taskStatusStatistics =
        if (hasReadPermission) projectStatisticsService.getTaskStatistics(projectIdentifiers)
        else emptyMap()
    val topicCriticalityStatistics =
        if (hasReadPermission) projectStatisticsService.getTopicStatistics(projectIdentifiers)
        else emptyMap()
    val constructionSiteManagers =
        participantQueryService.findParticipantsWithRole(
            projectIdentifiers, ParticipantRoleEnum.CSM)
    val csmsCompanyNamesByProjectIdentifiers =
        projectQueryService.findOldestCsmsCompanyNameByProjectIdentifiers(projectIdentifiers)

    // Get project participants of current user
    val userIdentifier = SecurityContextHelper.getInstance().getCurrentUser().identifier!!
    val participants: Map<ProjectId, Participant> =
        participantQueryService
            .findAllParticipants(setOf(userIdentifier.asUserId()), projectIdentifiers)[
                userIdentifier.asUserId()] ?: emptyMap()

    // Get number of active project participants
    val participantCount =
        participantQueryService.countActiveParticipantsPerProject(projectIdentifiers)

    return projects.map {
      buildProjectResource(
          it,
          includeEmbedded,
          projectPictures[it.identifier],
          csmsCompanyNamesByProjectIdentifiers[it.identifier],
          updateProjectPermissions.contains(it.identifier),
          deleteProjectPermissions.contains(it.identifier),
          createCraftMilestonePermissions.contains(it.identifier),
          createAnyMilestonePermissions.contains(it.identifier),
          createAnyMilestonePermissions.contains(it.identifier),
          updateRfvPermissions.contains(it.identifier),
          updateConstraintsPermissions.contains(it.identifier),
          reschedulePermissions.contains(it.identifier),
          taskStatusStatistics.getOrDefault(it.identifier, emptyMap()),
          topicCriticalityStatistics.getOrDefault(it.identifier, emptyMap()),
          participants.containsKey(it.identifier),
          constructionSiteManagers[it.identifier],
          participantCount[it.identifier] ?: 0L,
          auditUser[it.createdBy.get()]!!,
          auditUser[it.lastModifiedBy.get()]!!)
    }
  }

  private fun collectProjectIdentifiers(projects: Collection<Project>): Set<ProjectId> =
      projects.map { it.identifier }.toSet()

  private fun buildProjectResource(
      project: Project,
      addEmbedded: Boolean,
      projectPicture: ProjectPicture?,
      companyReference: NameByIdentifierDto?,
      hasUpdateProjectPermission: Boolean,
      hasDeleteProjectPermission: Boolean,
      hasCreateCraftMilestonePermission: Boolean,
      hasCreateInvestorsMilestonePermission: Boolean,
      hasCreateProjectMilestonePermission: Boolean,
      hasRfvUpdatePermission: Boolean,
      hasConstraintUpdatePermission: Boolean,
      hasReschedulePermission: Boolean,
      taskStatusStatistics: Map<TaskStatusEnum, Long>,
      topicCriticalityStatistics: Map<TopicCriticalityEnum, Long>,
      isParticipant: Boolean,
      csmParticipants: List<Participant>?,
      numberOfActiveParticipants: Long,
      createdBy: User,
      lastModifiedBy: User
  ): ProjectResource {
    Assert.notNull(project, "Project must be set")

    // Workaround to get oldest csm
    var csmResource: ProjectConstructionSiteManagerDto? = null
    if (csmParticipants != null) {
      csmParticipants.sortedWith(
          Comparator.comparing { part: Participant -> part.lastModifiedDate.get() })

      csmResource =
          if (csmParticipants.isEmpty()) null
          else ProjectConstructionSiteManagerDto(csmParticipants[0])
    }

    var companyResource: ResourceReference? = null
    if (companyReference != null) {
      companyResource = ResourceReference(companyReference.identifier, companyReference.name)
    }

    return ProjectResource(
            id = project.identifier.toUuid(),
            version = project.version,
            createdDate = project.createdDate.get().toDate(),
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedDate = project.lastModifiedDate.get().toDate(),
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            client = project.client,
            description = project.description,
            start = project.start,
            end = project.end,
            projectNumber = project.projectNumber,
            title = project.title,
            category = project.category,
            address =
                if (project.projectAddress != null) ProjectAddressDto(project.projectAddress!!)
                else null,
            participants = numberOfActiveParticipants,
            company = companyResource,
            constructionSiteManager = csmResource)
        .apply {

          // Include embedded resources
          if (addEmbedded) {
            addEmbeddedToResource(
                project,
                projectPicture,
                hasUpdateProjectPermission,
                taskStatusStatistics,
                topicCriticalityStatistics,
                isParticipant)
          }
          addLinksToResource(
              project,
              hasUpdateProjectPermission,
              hasDeleteProjectPermission,
              hasCreateCraftMilestonePermission,
              hasCreateInvestorsMilestonePermission,
              hasCreateProjectMilestonePermission,
              hasRfvUpdatePermission,
              hasConstraintUpdatePermission,
              hasReschedulePermission,
              isParticipant)
        }
  }

  private fun ProjectResource.addLinksToResource(
      project: Project,
      hasUpdateProjectPermission: Boolean,
      hasDeleteProjectPermission: Boolean,
      hasCreateCraftMilestonePermission: Boolean,
      hasCreateInvestorsMilestonePermission: Boolean,
      hasCreateProjectMilestonePermission: Boolean,
      hasRfvUpdatePermission: Boolean,
      hasConstraintUpdatePermission: Boolean,
      hasReschedulePermission: Boolean,
      isParticipant: Boolean
  ) {

    if (isParticipant) {
      this.add(tasksLink())
          .add(participantsLink(project.identifier))
          .add(companiesLink(project.identifier))
          .add(projectCraftLink(project.identifier))
          .add(workdayLink(project.identifier))
          .add(workAreasLink(project.identifier))
          .add(milestonesLink())
          .addIf(hasUpdateProjectPermission) { updateProjectLink(project.identifier) }
          .addIf(hasUpdateProjectPermission) { createUpdateWorkdaysLink(project.identifier) }
          .addIf(hasCreateCraftMilestonePermission) { createCraftMilestoneLink() }
          .addIf(hasCreateInvestorsMilestonePermission) { createInvestorMilestoneLink() }
          .addIf(hasCreateProjectMilestonePermission) { createProjectMilestoneLink() }
          .addIf(hasRfvUpdatePermission) { createUpdateRfvLink(project.identifier) }
          .addIf(hasConstraintUpdatePermission) { createUpdateConstraintsLink(project.identifier) }
          .addIf(hasReschedulePermission) { createRescheduleLink(project.identifier) }
    }

    this.addIf(hasDeleteProjectPermission) { deleteProjectLink(project.identifier) }
  }

  private fun ProjectResource.addEmbeddedToResource(
      project: Project,
      projectPicture: ProjectPicture?,
      hasDeletePermissionOnPicture: Boolean,
      taskStatusStatistics: Map<TaskStatusEnum, Long>,
      topicCriticalityStatistics: Map<TopicCriticalityEnum, Long>,
      isParticipant: Boolean
  ) {

    // Provides the "projectPicture" resource
    this.addResourceSupplier(EMBEDDED_PROJECT_PICTURES) {
      if (projectPicture == null) projectPictureResourceFactory.buildDefault(project)
      else projectPictureResourceFactory.build(projectPicture, hasDeletePermissionOnPicture)
    }
    this.embed(EMBEDDED_PROJECT_PICTURES)

    // Provides the "statistics" resource
    if (isParticipant) {
      this.addResourceSupplier(EMBEDDED_PROJECT_STATISTICS) {
        ProjectStatisticsResource(taskStatusStatistics, topicCriticalityStatistics)
      }
      this.embed(EMBEDDED_PROJECT_STATISTICS)
    }
  }

  open fun tasksLink(): Link =
      linkFactory.linkTo(TASKS_SEARCH_ENDPOINT).withRel(ProjectResource.LINK_TASKS)

  open fun participantsLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(ParticipantController.PARTICIPANTS_BY_PROJECT_SEARCH_ENDPOINT)
          .withParameters(
              mapOf(ParticipantController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_PARTICIPANTS)

  open fun companiesLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(ParticipantController.COMPANIES_BY_PROJECT_ID_ENDPOINT)
          .withParameters(
              mapOf(ParticipantController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_COMPANIES)

  open fun projectCraftLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(CRAFTS_ENDPOINT)
          .withParameters(
              mapOf(ProjectCraftController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_PROJECT_CRAFTS)

  open fun workdayLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(WORKDAY_BY_PROJECT_ID_ENDPOINT)
          .withParameters(
              mapOf(WorkdayConfigurationController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_WORKDAY_CONFIGURATION)

  private fun workAreasLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(WorkAreaController.WORKAREAS_BY_PROJECT_ID_ENDPOINT)
          .withParameters(mapOf(WorkAreaController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_PROJECT_WORKAREAS)

  open fun milestonesLink(): Link =
      linkFactory
          .linkTo(MilestoneSearchController.MILESTONES_SEARCH_ENDPOINT)
          .withRel(LINK_MILESTONES)

  open fun updateProjectLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(ProjectController.PROJECT_BY_PROJECT_ID_ENDPOINT)
          .withParameters(mapOf(ProjectController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_UPDATE_PROJECT)

  open fun deleteProjectLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(ProjectController.PROJECT_BY_PROJECT_ID_ENDPOINT)
          .withParameters(mapOf(ProjectController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_DELETE_PROJECT)

  open fun createCraftMilestoneLink(): Link =
      linkFactory
          .linkTo(MilestoneController.MILESTONES_ENDPOINT)
          .withRel(LINK_CREATE_CRAFT_MILESTONE)

  open fun createInvestorMilestoneLink(): Link =
      linkFactory
          .linkTo(MilestoneController.MILESTONES_ENDPOINT)
          .withRel(LINK_CREATE_INVESTOR_MILESTONE)

  open fun createProjectMilestoneLink(): Link =
      linkFactory
          .linkTo(MilestoneController.MILESTONES_ENDPOINT)
          .withRel(LINK_CREATE_PROJECT_MILESTONE)

  open fun createUpdateRfvLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(RfvController.RFVS_BY_PROJECT_ID)
          .withParameters(mapOf(RfvController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_UPDATE_RFV)

  open fun createUpdateConstraintsLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(TaskConstraintController.CONSTRAINTS_BY_PROJECT_ID)
          .withParameters(
              mapOf(TaskConstraintController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_UPDATE_CONSTRAINTS)

  open fun createUpdateWorkdaysLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(RfvController.RFVS_BY_PROJECT_ID)
          .withParameters(mapOf(RfvController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_UPDATE_RFV)

  open fun createRescheduleLink(projectIdentifier: ProjectId): Link =
      linkFactory
          .linkTo(RescheduleController.RESCHEDULE_ENDPOINT)
          .withParameters(mapOf(RescheduleController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
          .withRel(LINK_RESCHEDULE)
}
