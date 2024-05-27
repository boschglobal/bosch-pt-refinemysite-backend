/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.snapshot

import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import org.springframework.data.mongodb.repository.MongoRepository

interface JobSnapshotRepository : MongoRepository<JobSnapshot, JobIdentifier> {
  fun countByUserIdentifierAndStateIn(userIdentifier: UserIdentifier, states: Set<JobState>): Long
}
