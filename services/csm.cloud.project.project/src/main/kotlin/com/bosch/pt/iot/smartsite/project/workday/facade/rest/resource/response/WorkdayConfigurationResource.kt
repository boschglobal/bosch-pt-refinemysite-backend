/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.HolidayResource
import java.time.DayOfWeek
import java.util.Date
import java.util.SortedSet
import java.util.UUID

data class WorkdayConfigurationResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val project: ResourceReference,
    val startOfWeek: DayOfWeek,
    val workingDays: SortedSet<DayOfWeek>,
    val holidays: SortedSet<HolidayResource>,
    val allowWorkOnNonWorkingDays: Boolean
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_UPDATE = "updateWorkdays"
  }
}
