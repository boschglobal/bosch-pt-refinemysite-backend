/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler.exception

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.job.common.translation.Key
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier

class JobNotFoundException(jobIdentifier: JobIdentifier) :
    AggregateNotFoundException(Key.JOB_VALIDATION_ERROR_NOT_FOUND, jobIdentifier.value)
