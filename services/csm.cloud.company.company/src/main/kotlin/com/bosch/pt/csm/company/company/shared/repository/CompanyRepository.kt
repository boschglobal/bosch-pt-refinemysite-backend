/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.shared.repository

import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CompanyRepository : JpaRepository<Company, Long> {

  @EntityGraph(attributePaths = ["createdBy", "lastModifiedBy"])
  @Query(
      "select c from Company c where :#{#restrictedCountries.size()} = 0 " +
          "or c.streetAddress.country in :restrictedCountries " +
          "or c.postBoxAddress.country in :restrictedCountries")
  fun findAll(
      @Param("restrictedCountries") restrictedCountries: Set<String>,
      pageable: Pageable
  ): Page<Company>

  @EntityGraph(attributePaths = ["createdBy", "lastModifiedBy"])
  @Query(
      "select c from Company c where upper(c.name) like upper(concat('%', :name, '%')) " +
          "and (:#{#restrictedCountries.size()} = 0 " +
          "or c.streetAddress.country in :restrictedCountries " +
          "or c.postBoxAddress.country in :restrictedCountries)")
  fun findAllByNameContainingIgnoreCase(
      @Param("name") name: String,
      @Param("restrictedCountries") restrictedCountries: Set<String>,
      pageable: Pageable
  ): Page<Company>

  @EntityGraph(attributePaths = ["streetAddress", "postBoxAddress", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: CompanyId): Company?

  fun findOneByIdentifier(identifier: CompanyId): Company?

  @Query(
      ("select company.identifier from Company company " +
          "inner join Employee AS employee on company.id = employee.company.id " +
          "where company.identifier in :companyIdentifiers " +
          "group by company.identifier"))
  fun findAllCompaniesWithEmployee(
      @Param("companyIdentifiers") companyIdentifiers: List<CompanyId>
  ): List<CompanyId>

  @Query(
      "select c from Company c where upper(c.name) like upper(concat('%', :term, '%')) " +
          "and (:#{#restrictedCountries.size()} = 0 or c.streetAddress.country in :restrictedCountries " +
          "or c.postBoxAddress.country in :restrictedCountries)")
  fun suggestCompaniesByTerm(
      @Param("term") term: String?,
      @Param("restrictedCountries") restrictedCountries: Set<String>,
      pageable: Pageable
  ): Page<Company>
}
