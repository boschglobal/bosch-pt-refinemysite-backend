/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.repository

import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.craft.model.CraftTranslationProjection
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CraftRepository : ReplicatedEntityRepository<Craft, Long> {

  fun findByIdentifierIn(identifier: Collection<UUID>): Set<Craft>

  fun findOneByIdentifier(identifier: UUID): Craft?

  @EntityGraph(attributePaths = ["translations", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): Craft?

  /**
   * Retrieves list of crafts with the translation for the given set of identifiers and locale.
   *
   * @param identifiers the set of identifiers for which crafts should be retrieved
   * @param locale the locale for the translation
   * @return the matching crafts
   */
  @Query(
      "select new com.bosch.pt.iot.smartsite.craft.model.CraftTranslationProjection(c.identifier, c.defaultName," +
          " t.locale, t.value) from Craft c left join c.translations t on t.locale = :locale" +
          " where c.identifier in :identifiers")
  fun findByIdentifiersAndTranslationsLocale(
      @Param("identifiers") identifiers: Collection<UUID>,
      @Param("locale") locale: String
  ): List<CraftTranslationProjection>
}
