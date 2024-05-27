/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response

data class WorkDayConfigurationListResource(
    val workDayConfigurations: List<WorkDayConfigurationResource>
)
