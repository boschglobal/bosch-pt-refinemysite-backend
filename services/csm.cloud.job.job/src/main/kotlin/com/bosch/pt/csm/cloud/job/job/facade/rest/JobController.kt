/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.cloud.job.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.csm.cloud.job.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.MarkJobResultReadCommand
import com.bosch.pt.csm.cloud.job.job.command.handler.JobCommandDispatcher
import com.bosch.pt.csm.cloud.job.job.query.JobProjection
import com.bosch.pt.csm.cloud.job.job.query.JobProjectionRepository
import com.bosch.pt.csm.cloud.job.job.query.JobResource
import com.bosch.pt.csm.cloud.job.job.shared.JobList
import com.bosch.pt.csm.cloud.job.job.shared.JobListRepository
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetails
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
class JobController(
    val jobProjectionRepository: JobProjectionRepository,
    val jobCommandDispatcher: JobCommandDispatcher,
    val jobListRepository: JobListRepository
) {

  @GetMapping("/jobs")
  fun getJobsForUser(
      @AuthenticationPrincipal user: JobServiceUserDetails,
      @PageableDefault(sort = ["lastModifiedDate"], direction = DESC, size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<JobListResource> {
    val sortableFields = mapOf("status" to "status", "lastModifiedDate" to "lastModifiedDate")
    val filteredPageable = filterAndTranslate(pageable, sortableFields)
    return ResponseEntity.ok(
        JobListResource(
            page = jobProjectionRepository.findByUserIdentifier(user.identifier, filteredPageable),
            lastSeen =
                jobListRepository
                    .findById(user.identifier)
                    .map { it.lastSeen.toInstant(ZoneOffset.UTC) }
                    .orElse(null)))
  }

  @PostMapping("/jobs/seen")
  fun updateJobListLastSeen(
      @AuthenticationPrincipal user: JobServiceUserDetails,
      @RequestBody body: LastSeenRequestBody
  ): ResponseEntity<Void> {
    jobListRepository.save(
        JobList(user.identifier, LocalDateTime.ofInstant(body.lastSeen, ZoneId.of("UTC"))))
    return ResponseEntity.accepted().build()
  }

  @PostMapping("/jobs/{jobIdentifier}/read")
  fun markJobAsRead(
      @PathVariable("jobIdentifier") jobIdentifier: JobIdentifier
  ): ResponseEntity<Void> {
    jobCommandDispatcher.dispatch(MarkJobResultReadCommand(jobIdentifier))
    return ResponseEntity.accepted().build()
  }
}

class JobListResource(page: Page<JobProjection>, val lastSeen: Instant?) :
    ListResponseResource<JobResource>(
        page.content.map {

          // TODO: Remove environment specific code after adjustment of the clients
          // Check: SMAR-19217 and SMAR-19218
          val path = ServletUriComponentsBuilder.fromCurrentRequest().build().path

          JobResource(
              it.let {
                if (it.serializedResult?.json?.contains(API_PATH) == true &&
                    path?.contains(INTERNAL_PATH) == true) {

                  var resource: JobProjection = it
                  listOf(
                          Pair("sandbox1.", "sandbox1-api."),
                          Pair("sandbox2.", "sandbox2-api."),
                          Pair("sandbox3.", "sandbox3-api."),
                          Pair("sandbox4.", "sandbox4-api."),
                          Pair("dev.", "dev-api."),
                          Pair("review.", "review-api."),
                          Pair("test1.", "test1-api."),
                          Pair("app.", "api."),
                      )
                      .forEach {

                        // Replace sub-domain
                        if (resource.containsInResult(it.first)) {
                          resource = resource.replaceInResult(it.first, it.second)
                          return@forEach
                        }
                      }

                  // Replace path prefix
                  resource.replaceInResult(API_PATH, INTERNAL_PATH)
                } else if (it.serializedResult?.json?.contains(INTERNAL_PATH) == true &&
                    path?.contains(API_PATH) == true) {

                  var resource: JobProjection = it
                  listOf(
                          Pair("sandbox1-api.", "sandbox1."),
                          Pair("sandbox2-api.", "sandbox2."),
                          Pair("sandbox3-api.", "sandbox3."),
                          Pair("sandbox4-api.", "sandbox4."),
                          Pair("dev-api.", "dev."),
                          Pair("review-api.", "review."),
                          Pair("test1-api.", "test1."),
                          Pair("api.", "app."),
                      )
                      .forEach {

                        // Replace sub-domain
                        if (resource.containsInResult(it.first)) {
                          resource = resource.replaceInResult(it.first, it.second)
                          return@forEach
                        }
                      }

                  // Replace path prefix
                  resource.replaceInResult(INTERNAL_PATH, API_PATH)
                } else {
                  it
                }
              })
        },
        page.number,
        page.size,
        page.totalPages,
        page.totalElements) {

  companion object {
    const val API_PATH = "/api/"
    const val INTERNAL_PATH = "/internal/"
  }
}

private fun JobProjection.containsInResult(value: String): Boolean =
    this.serializedResult?.json?.contains(value) == true

private fun JobProjection.replaceInResult(oldValue: String, newValue: String): JobProjection =
    this.copy(
        serializedResult =
            this.serializedResult?.copy(
                json = checkNotNull(this.serializedResult).json.replace(oldValue, newValue)))

data class LastSeenRequestBody(val lastSeen: Instant)
