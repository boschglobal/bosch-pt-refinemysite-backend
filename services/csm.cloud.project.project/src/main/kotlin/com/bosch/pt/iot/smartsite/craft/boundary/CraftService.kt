/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.craft.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME
import com.bosch.pt.iot.smartsite.common.i18n.Key.USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.craft.model.CraftTranslationProjection
import com.bosch.pt.iot.smartsite.craft.repository.CraftRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class CraftService(private val craftRepository: CraftRepository) {

  @Trace
  @NoPreAuthorize
  @Transactional
  open fun create(craft: Craft): UUID {
    if (StringUtils.isBlank(craft.defaultName)) {
      throw PreconditionViolationException(CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME)
    }

    return craftRepository.save(craft).identifier!!
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneByIdentifier(identifier: UUID): Craft? =
      craftRepository.findOneByIdentifier(identifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findByIdentifiersAndTranslationsLocale(
      identifiers: Collection<UUID>,
      language: String
  ): List<CraftTranslationProjection> =
      if (identifiers.isEmpty()) {
        emptyList()
      } else craftRepository.findByIdentifiersAndTranslationsLocale(identifiers, language)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findByIdentifierIn(identifiers: Collection<UUID>): Set<Craft> {
    val crafts = craftRepository.findByIdentifierIn(identifiers)

    if (crafts.size != identifiers.size) {
      throw PreconditionViolationException(USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND)
    }

    return crafts
  }
}
