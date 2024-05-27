/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.query

import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository.CraftRepository
import org.apache.commons.collections4.CollectionUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
class CraftQueryService(private val craftRepository: CraftRepository) {

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAllByLanguage(language: String, pageable: Pageable): Page<CraftTranslationProjection> =
      craftRepository.findAllByTranslationsLocale(language, pageable)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findOneByIdentifier(identifier: CraftId): Craft? =
      craftRepository.findOneByIdentifier(identifier)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findOneWithUserAndTranslationsByIdentifier(identifier: CraftId): Craft? =
      craftRepository.findOneWithUserAndTranslationsByIdentifier(identifier)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findByIdentifierAndTranslationsLocale(
      identifier: CraftId,
      locale: String
  ): CraftTranslationProjection? =
      craftRepository.findByIdentifierAndTranslationsLocale(identifier, locale)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findByIdentifiersAndTranslationsLocale(
      identifiers: Collection<CraftId>,
      language: String
  ): List<CraftTranslationProjection> =
      when (CollectionUtils.isEmpty(identifiers)) {
        true -> emptyList()
        else -> craftRepository.findByIdentifiersAndTranslationsLocale(identifiers, language)
      }

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  fun findByIdentifierIn(identifiers: Collection<CraftId>): Set<Craft> =
      craftRepository.findByIdentifierIn(identifiers)
}
