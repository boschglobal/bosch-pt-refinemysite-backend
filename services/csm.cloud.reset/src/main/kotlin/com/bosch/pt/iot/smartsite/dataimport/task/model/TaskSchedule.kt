/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import java.time.LocalDate

class TaskSchedule(
    override var id: String,
    var version: Long? = null,
    var start: LocalDate? = null,
    var end: LocalDate? = null
) : ImportObject
