/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import org.springframework.hateoas.RepresentationModel

class TaskScheduleListResource(val taskSchedules: Collection<TaskScheduleResource>) :
    RepresentationModel<AbstractResource>()
