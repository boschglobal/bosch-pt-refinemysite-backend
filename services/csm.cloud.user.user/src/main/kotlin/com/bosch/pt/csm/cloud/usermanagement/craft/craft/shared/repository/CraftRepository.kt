/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository

import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CraftRepository : JpaRepository<Craft, Long> {

  /**
   * Retrieves a page of [CraftTranslationProjection]s with given locale.
   *
   * @param locale the requested locale
   * @param pageable page request
   * @return a page of crafts
   */
  @Query(
      "select new com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model." +
          "CraftTranslationProjection(" +
          "c.identifier, c.defaultName, t.locale, t.value) " +
          "from Craft c left join c.translations t on t.locale = :locale")
  fun findAllByTranslationsLocale(
      @Param("locale") locale: String,
      pageable: Pageable
  ): Page<CraftTranslationProjection>

  /**
   * Retrieves a single craft for a specific identifier.
   *
   * @param identifier the identifier of the craft
   * @return the craft if found
   */
  fun findOneByIdentifier(identifier: CraftId): Craft?

  @EntityGraph(attributePaths = ["translations", "createdBy", "lastModifiedBy"])
  fun findOneWithUserAndTranslationsByIdentifier(identifier: CraftId): Craft?

  fun findByIdentifierIn(identifier: Collection<CraftId>): Set<Craft>

  /**
   * Retrieves a single craft with the translation for the given identifier and locale.
   *
   * @param identifier the identifier
   * @param locale the locale
   * @return the craft
   */
  @Query(
      "select new com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model." +
          "CraftTranslationProjection(" +
          "c.identifier, c.defaultName, t.locale, t.value) " +
          "from Craft c left join c.translations t on t.locale = :locale " +
          "where c.identifier = :identifier")
  fun findByIdentifierAndTranslationsLocale(
      @Param("identifier") identifier: CraftId,
      @Param("locale") locale: String
  ): CraftTranslationProjection?

  /**
   * Retrieves list of crafts with the translation for the given set of identifiers and locale.
   *
   * @param identifiers the set of identifiers for which crafts should be retrieved
   * @param locale the locale for the translation
   * @return the matching crafts
   */
  @Query(
      "select new com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model." +
          "CraftTranslationProjection(" +
          "c.identifier, c.defaultName, t.locale, t.value) " +
          "from Craft c left join c.translations t on t.locale = :locale " +
          "where c.identifier in :identifiers")
  fun findByIdentifiersAndTranslationsLocale(
      @Param("identifiers") identifiers: Collection<CraftId>,
      @Param("locale") locale: String
  ): List<CraftTranslationProjection>
}
