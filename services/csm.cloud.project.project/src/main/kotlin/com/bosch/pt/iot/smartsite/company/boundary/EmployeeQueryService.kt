/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class EmployeeQueryService(private val employeeRepository: EmployeeRepository) {

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneByUser(user: User): Employee? = employeeRepository.findOneByUser(user)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneByIdentifier(identifier: UUID): Employee? =
      employeeRepository.findOneByIdentifier(identifier)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneWithRolesByUser(user: User): Employee? =
      employeeRepository.findOneWithRolesByUser(user)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneByUserIdentifier(userIdentifier: UUID): Employee? =
      employeeRepository.findOneByUserIdentifier(userIdentifier)
}
