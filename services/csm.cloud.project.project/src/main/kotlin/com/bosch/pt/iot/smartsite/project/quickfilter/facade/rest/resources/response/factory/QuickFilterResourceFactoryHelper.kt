/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.PATH_VARIABLE_QUICK_FILTER_ID
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource.CriteriaResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource.Companion.LINK_FILTER_DELETE
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource.Companion.LINK_FILTER_UPDATE
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource.FilterAssigneeResource
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
open class QuickFilterResourceFactoryHelper(
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(
      quickFilters: List<QuickFilter>,
      projectRef: ProjectId
  ): List<QuickFilterResource> {
    val userIdentifiers = quickFilters.map { it.createdBy }.toMutableSet()
    userIdentifiers.addAll(quickFilters.map { it.lastModifiedBy })

    val users = userService.findAllByIdentifiers(userIdentifiers).associateBy { it.identifier!! }
    return quickFilters.map { build(it, projectRef, users) }
  }

  private fun build(quickFilter: QuickFilter, projectRef: ProjectId, users: Map<UUID, User>) =
      QuickFilterResource(
              identifier = quickFilter.identifier,
              version = quickFilter.version!!,
              name = quickFilter.name,
              useTaskCriteria = quickFilter.useTaskCriteria,
              useMilestoneCriteria = quickFilter.useMilestoneCriteria,
              criteria = buildCriteria(quickFilter),
              highlight = quickFilter.highlight,
              createdDate = quickFilter.createdDate.toDate(),
              lastModifiedDate = quickFilter.lastModifiedDate.toDate(),
              createdBy = resourceReferenceOf(users[quickFilter.createdBy]!!),
              lastModifiedBy = resourceReferenceOf(users[quickFilter.lastModifiedBy]!!))
          .apply {
            addUpdateLink(projectRef)
            addDeleteLink(projectRef)
          }

  private fun buildCriteria(
      quickFilter: QuickFilter,
  ) =
      CriteriaResource(
          milestones =
              FilterMilestoneListResource(
                  from = quickFilter.milestoneCriteria.from,
                  to = quickFilter.milestoneCriteria.to,
                  workAreas =
                      FilterMilestoneListResource.WorkAreaFilter(
                          header = quickFilter.milestoneCriteria.workAreas.header,
                          workAreaIds = quickFilter.milestoneCriteria.workAreas.workAreaIds),
                  types =
                      FilterMilestoneListResource.TypesFilter(
                          types = quickFilter.milestoneCriteria.milestoneTypes.types,
                          projectCraftIds =
                              quickFilter.milestoneCriteria.milestoneTypes.projectCraftIds)),
          tasks =
              FilterTaskListResource(
                  from = quickFilter.taskCriteria.from,
                  to = quickFilter.taskCriteria.to,
                  workAreaIds = quickFilter.taskCriteria.workAreaIds.toList(),
                  projectCraftIds = quickFilter.taskCriteria.projectCraftIds.toList(),
                  allDaysInDateRange = quickFilter.taskCriteria.allDaysInDateRange,
                  status = quickFilter.taskCriteria.status.toList(),
                  assignees =
                      FilterAssigneeResource(
                          participantIds =
                              quickFilter.taskCriteria.assignees.participantIds.toList(),
                          companyIds = quickFilter.taskCriteria.assignees.companyIds.toList(),
                      ),
                  hasTopics = quickFilter.taskCriteria.hasTopics,
                  topicCriticality = quickFilter.taskCriteria.topicCriticality.toList()))

  private fun QuickFilterResource.addUpdateLink(projectRef: ProjectId) {
    add(
        linkFactory
            .linkTo(QuickFilterController.QUICK_FILTER_ENDPOINT)
            .withParameters(
                mapOf(
                    PATH_VARIABLE_PROJECT_ID to projectRef,
                    PATH_VARIABLE_QUICK_FILTER_ID to identifier))
            .withRel(LINK_FILTER_UPDATE))
  }

  private fun QuickFilterResource.addDeleteLink(projectRef: ProjectId) {
    add(
        linkFactory
            .linkTo(QuickFilterController.QUICK_FILTER_ENDPOINT)
            .withParameters(
                mapOf(
                    PATH_VARIABLE_PROJECT_ID to projectRef,
                    PATH_VARIABLE_QUICK_FILTER_ID to identifier))
            .withRel(LINK_FILTER_DELETE))
  }

  private fun resourceReferenceOf(user: User) = referTo(user, deletedUserReference, user.deleted)
}
