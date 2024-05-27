/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.boundary

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ExternalIdService(private val externalIdRepository: ExternalIdRepository) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun saveAll(ids: List<ExternalId>) {
    ids.map { externalIdRepository.save(it, CREATED).identifier }
  }
}
