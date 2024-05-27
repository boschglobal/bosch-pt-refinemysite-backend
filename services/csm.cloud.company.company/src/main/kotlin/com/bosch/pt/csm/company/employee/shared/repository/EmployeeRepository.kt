/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.shared.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.shared.model.Employee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmployeeRepository : JpaRepository<Employee, Long> {

  @Query(
      "select employee.identifier from Employee employee " +
        "left join UserProjection as userRef on employee.userRef.identifier = userRef.id.identifier " +
        "where employee.company = :company " +
        "and (:#{#restrictedCountries.size()} = 0 or userRef.country in :restrictedCountries)")
  fun findAllEmployeeIdentifiersByCompany(
      @Param("company") company: Company,
      @Param("restrictedCountries") restrictedCountries: Set<IsoCountryCodeEnum>,
      pageable: Pageable
  ): Page<EmployeeId>

  fun countAllByCompanyIdentifier(identifier: CompanyId): Int

  fun findOneByUserRef(userRef: UserId): Employee?

  fun findOneByIdentifier(identifier: EmployeeId): Employee?

  @EntityGraph(attributePaths = ["company", "roles"])
  fun findOneWithDetailsByIdentifier(identifier: EmployeeId): Employee?

  @Query(
      "select employee from Employee employee " +
        "left join UserProjection as userRef on employee.userRef.identifier = userRef.id.identifier " +
        "where employee.identifier in :identifier ")
  @EntityGraph(attributePaths = ["roles"])
  fun findAllWithDetailsByIdentifierIn(identifier: Set<EmployeeId>, sort: Sort): List<Employee>
}
