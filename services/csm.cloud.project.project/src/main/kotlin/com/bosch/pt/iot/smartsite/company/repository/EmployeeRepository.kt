/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.repository

import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.dto.EmployeeDto
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query

interface EmployeeRepository : ReplicatedEntityRepository<Employee, Long> {

  fun findOneByUserIdentifierAndCompanyIdentifier(
      userIdentifier: UUID,
      companyIdentifier: UUID
  ): Employee?

  fun findOneByUser(user: User): Employee?

  @EntityGraph(attributePaths = ["roles"]) fun findOneWithRolesByUser(user: User): Employee?

  fun findOneByUserIdentifier(userIdentifier: UUID): Employee?

  @EntityGraph(attributePaths = ["user", "company", "roles", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByUserIdentifier(userIdentifier: UUID): Employee?

  fun findOneByUserEmail(email: String): Employee?

  fun findOneByIdentifier(identifier: UUID): Employee?

  fun findAllByUserIdentifierIn(userIdentifiers: Set<UUID>): List<EmployeeDto>

  @EntityGraph(attributePaths = ["user", "company", "roles", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): Employee?

  @Query(
      "select emp.company.identifier from Employee emp where emp.user.identifier = :userIdentifier")
  fun findCompanyIdentifierByUserIdentifier(userIdentifier: UUID): UUID?
}
