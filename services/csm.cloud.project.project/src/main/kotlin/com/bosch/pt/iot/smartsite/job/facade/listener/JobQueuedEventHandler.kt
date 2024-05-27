/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.facade.listener

import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro

interface JobQueuedEventHandler {

  fun handles(job: JobQueuedEventAvro): Boolean

  fun handle(job: JobQueuedEventAvro): Any
}
