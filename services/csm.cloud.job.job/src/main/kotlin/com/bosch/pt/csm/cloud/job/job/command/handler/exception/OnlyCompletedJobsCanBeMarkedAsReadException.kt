/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler.exception

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.job.common.translation.Key.JOB_VALIDATION_ERROR_ONLY_COMPLETED_JOBS_CAN_BE_MARKED_AS_READ
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshot

class OnlyCompletedJobsCanBeMarkedAsReadException(job: JobSnapshot) :
    PreconditionViolationException(
        JOB_VALIDATION_ERROR_ONLY_COMPLETED_JOBS_CAN_BE_MARKED_AS_READ,
        "Job is not COMPLETED and cannot be marked as read: $job")
