/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.query

import com.bosch.pt.csm.cloud.job.event.integration.EventService
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.fasterxml.jackson.annotation.JsonRawValue
import java.time.Instant
import java.time.ZoneOffset
import org.slf4j.Logger
import org.springframework.stereotype.Component

/** Creates and updates [JobProjection] based on a stream of [JobEvents]. */
@Component
class JobProjector(
    private val eventService: EventService,
    private val jobProjectionRepository: JobProjectionRepository,
    private val logger: Logger
) {

  fun handle(event: JobEvent) {
    // We want to update the projection with any event content without further validation.  We
    // can't do this for a Job we don't know, because we would miss the userIdentifier and most
    // useful fields.  For all other events, we can simply update the respective fields.
    updatedJobProjectionGiven(event)?.also {
      jobProjectionRepository.save(it)
      notifyUserAbout(it)
    }
        ?: logger.warn("Event for unknown Job ignored: $event")
  }

  private fun updatedJobProjectionGiven(event: JobEvent): JobProjection? =
      when (event) {
        is JobQueuedEvent -> JobProjection(event)
        is JobRejectedEvent -> JobProjection(event)
        else -> loadJobProjection(event.aggregateIdentifier)?.update(event)
      }

  private fun loadJobProjection(jobIdentifier: JobIdentifier): JobProjection? =
      jobProjectionRepository.findById(jobIdentifier).orElse(null)

  private fun notifyUserAbout(updatedJob: JobProjection) {
    eventService.send(updatedJob.userIdentifier, JobResource(updatedJob))
  }
}

// We considered moving [JobResource] and [EventService] to the facade package.  Just as
// [JobController], they are both concerned in communicating projections to the frontend.  This
// would lead to a layer violation, however, and fixing it would require us to e.g. use the Spring
// event bus -- another indirection that makes the component architecture less familiar and harder
// to understand.

/** Representation of a Job for the frontend, delivered via [JobController] and [EventService]. */
data class JobResource(
    val id: String,
    val type: String,
    val status: String,
    val createdDate: Instant,
    val lastModifiedDate: Instant,
    @JsonRawValue val context: String?,
    @JsonRawValue val result: String?,
    val read: Boolean? = false
) {
  constructor(
      jobProjection: JobProjection
  ) : this(
      id = jobProjection.jobIdentifier.value,
      type = jobProjection.jobType,
      status = jobProjection.state.name,
      createdDate = jobProjection.createdDate.toInstant(ZoneOffset.UTC),
      lastModifiedDate = jobProjection.lastModifiedDate.toInstant(ZoneOffset.UTC),
      context = jobProjection.serializedContext?.json,
      result = jobProjection.serializedResult?.json,
      read = jobProjection.read)
}
