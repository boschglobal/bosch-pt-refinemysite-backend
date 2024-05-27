/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.query

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import datadog.trace.api.Trace
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class InvitationQueryService(
    private val invitationRepository: InvitationRepository,
) {

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAllCreatedAtDay(day: LocalDate) =
      invitationRepository.findAllByCreatedDateAfterAndCreatedDateBefore(
          day.atStartOfDay().toDate(), day.atEndOfDay().toDate())

  private fun LocalDate.atEndOfDay() = LocalDateTime.of(this, LocalTime.MAX)
}
