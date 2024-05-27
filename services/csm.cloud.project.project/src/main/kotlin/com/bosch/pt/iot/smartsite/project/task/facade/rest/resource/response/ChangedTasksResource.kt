/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource

/** This resource is used when we have batch endpoint that modifies multiple tasks. */
@Deprecated("Should be replaced with BatchResponseResource")
class ChangedTasksResource(
    val tasks: Collection<TaskResource>,
) : AbstractResource()
