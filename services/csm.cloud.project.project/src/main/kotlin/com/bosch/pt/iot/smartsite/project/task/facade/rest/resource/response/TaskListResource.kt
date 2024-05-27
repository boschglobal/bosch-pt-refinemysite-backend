/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractPageResource

class TaskListResource(
    val tasks: Collection<TaskResource>,
    pageNumber: Int,
    pageSize: Int,
    totalPages: Int,
    totalElements: Long
) : AbstractPageResource(pageNumber, pageSize, totalPages, totalElements) {

  companion object {
    const val LINK_ASSIGN = "assign"
    const val LINK_SEND = "send"
  }
}
