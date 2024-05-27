/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.query

import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface JobProjectionRepository : MongoRepository<JobProjection, JobIdentifier> {
  fun findByUserIdentifier(userIdentifier: UserIdentifier, pageable: Pageable): Page<JobProjection>
  fun countByState(state: JobState): Int
  fun findFirstByStateOrderByCreatedDateAsc(state: JobState): JobProjection?
}
