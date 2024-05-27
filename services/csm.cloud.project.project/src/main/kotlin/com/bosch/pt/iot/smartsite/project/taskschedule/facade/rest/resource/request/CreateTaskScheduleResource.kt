/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request

import java.time.LocalDate

open class CreateTaskScheduleResource(open val start: LocalDate?, open val end: LocalDate?)