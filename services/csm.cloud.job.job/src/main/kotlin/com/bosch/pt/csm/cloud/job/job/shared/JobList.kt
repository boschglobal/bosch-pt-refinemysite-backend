/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.shared

import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "JobList")
@TypeAlias("JobList")
data class JobList(
    @Id val userIdentifier: UserIdentifier,
    val lastSeen: LocalDateTime
)
