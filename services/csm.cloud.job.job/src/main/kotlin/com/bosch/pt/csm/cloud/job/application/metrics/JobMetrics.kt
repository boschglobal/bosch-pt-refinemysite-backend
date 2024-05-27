/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.application.metrics

import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState.QUEUED
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState.RUNNING
import com.bosch.pt.csm.cloud.job.job.query.JobProjectionRepository
import io.micrometer.core.instrument.Metrics.gauge
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobMetrics(private val jobProjectionRepository: JobProjectionRepository) {

  private val jobsQueued: AtomicInteger? = gauge("custom.jobs.queued", listOf(), AtomicInteger(0))

  private val jobsQueuedMax: AtomicInteger? =
      gauge("custom.jobs.queued.max", listOf(), AtomicInteger(0))

  private val jobsRunning: AtomicInteger? = gauge("custom.jobs.running", listOf(), AtomicInteger(0))

  private val jobsRunningMax: AtomicInteger? =
      gauge("custom.jobs.running.max", listOf(), AtomicInteger(0))

  @Scheduled(fixedDelay = 5000L)
  fun updateMetrics() {
    LocalDateTime.now().apply {
      jobsQueued!!.set(jobProjectionRepository.countByState(QUEUED))
      jobsQueuedMax!!.set(
          Duration.between(
                  jobProjectionRepository.findFirstByStateOrderByCreatedDateAsc(QUEUED)?.createdDate
                      ?: this,
                  this)
              .toSeconds()
              .toInt())
      jobsRunning!!.set(jobProjectionRepository.countByState(RUNNING))
      jobsRunningMax!!.set(
          Duration.between(
                  jobProjectionRepository.findFirstByStateOrderByCreatedDateAsc(RUNNING)
                      ?.createdDate
                      ?: this,
                  this)
              .toSeconds()
              .toInt())
    }
  }
}
