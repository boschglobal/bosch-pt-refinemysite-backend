/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request

import java.time.LocalDate

class CreateTaskScheduleResource(val start: LocalDate? = null, val end: LocalDate? = null)
