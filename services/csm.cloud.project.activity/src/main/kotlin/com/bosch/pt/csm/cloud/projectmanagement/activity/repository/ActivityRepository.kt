/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.mongodb.repository.MongoRepository

interface ActivityRepository : MongoRepository<Activity, AggregateIdentifier> {

  fun findOneByIdentifier(identifier: UUID): Activity?

  fun findAllByContextTask(taskIdentifier: UUID, pageable: Pageable): Slice<Activity>

  fun findAllByContextTaskAndEventDateLessThan(
      taskIdentifier: UUID,
      date: Instant,
      pageable: Pageable
  ): Slice<Activity>

  fun findActivityByAttachmentIdentifier(attachmentIdentifier: UUID): Activity?

  fun findActivityByAggregateIdentifier(aggregateIdentifier: AggregateIdentifier): Activity?

  fun deleteAllByContextTask(taskIdentifier: UUID)

  fun deleteAllByContextProject(projectIdentifier: UUID)
}
