/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.WorkdayConfigurationController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.WorkdayConfigurationController.Companion.WORKDAY_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.HolidayResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.WorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.WorkdayConfigurationResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class WorkdayConfigurationResourceFactoryHelper(
    messageSource: MessageSource,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(
      workdayConfigurations: Collection<WorkdayConfiguration>
  ): List<WorkdayConfigurationResource> {
    if (workdayConfigurations.isEmpty()) {
      return emptyList()
    }

    val auditUsers = userService.findAuditUsers(workdayConfigurations)

    return workdayConfigurations.map {
      build(it, auditUsers[it.createdBy.get()]!!, auditUsers[it.lastModifiedBy.get()]!!)
    }
  }

  open fun build(
      workdayConfiguration: WorkdayConfiguration,
      createdBy: User,
      lastModifiedBy: User
  ): WorkdayConfigurationResource {
    return WorkdayConfigurationResource(
            id = workdayConfiguration.identifier.toUuid(),
            version = workdayConfiguration.version,
            createdDate = workdayConfiguration.createdDate.get().toDate(),
            createdBy = User.referTo(createdBy, deletedUserReference)!!,
            lastModifiedDate = workdayConfiguration.lastModifiedDate.get().toDate(),
            lastModifiedBy = User.referTo(lastModifiedBy, deletedUserReference)!!,
            project = ResourceReference.from(workdayConfiguration.project),
            startOfWeek = workdayConfiguration.startOfWeek,
            workingDays = workdayConfiguration.workingDays.toSortedSet(),
            holidays = workdayConfiguration.holidays.toSortedHolidayResources(),
            allowWorkOnNonWorkingDays = workdayConfiguration.allowWorkOnNonWorkingDays)
        .apply {
          // add update link
          addIf(
              (projectAuthorizationComponent.hasUpdatePermissionOnProject(
                  workdayConfiguration.project.identifier))) {
                linkFactory
                    .linkTo(WORKDAY_BY_PROJECT_ID_ENDPOINT)
                    .withParameters(
                        mapOf(PATH_VARIABLE_PROJECT_ID to workdayConfiguration.identifier))
                    .withRel(LINK_UPDATE)
              }
        }
  }

  private fun Set<Holiday>.toSortedHolidayResources() =
      map { HolidayResource(it.name, it.date) }.toSortedSet(compareBy({ it.date }, { it.name }))
}
