/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler.exception

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.job.common.translation.Key.JOB_VALIDATION_ERROR_INVALID_JOB_STATE_TRANSITION
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState

class InvalidJobStateTransitionException(from: JobState, to: JobState) :
    PreconditionViolationException(
        JOB_VALIDATION_ERROR_INVALID_JOB_STATE_TRANSITION,
        "Cannot transition Job from $from to $to.")
