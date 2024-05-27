/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.facade.listener

import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro

interface JobCompletedEventHandler {

  fun handles(job: JobCompletedEventAvro): Boolean

  fun handle(job: JobCompletedEventAvro)
}
