/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.repository

import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.company.model.Company
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CompanyRepository : ReplicatedEntityRepository<Company, Long> {

  @EntityGraph(attributePaths = ["streetAddress", "postBoxAddress", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): Company?

  fun findOneByIdentifier(identifier: UUID): Company?

  @Query("select c.id from Company c where c.identifier = :identifier")
  fun findIdByIdentifier(identifier: UUID): Long?

  @EntityGraph(attributePaths = ["createdBy", "lastModifiedBy"])
  override fun findAll(pageable: Pageable): Page<Company>

  @Query("select comp.identifier from Company comp where comp.identifier in :identifiers")
  fun validateExistingIdentifiersFor(
      @Param("identifiers") identifiers: Collection<UUID>
  ): Collection<UUID>
}
