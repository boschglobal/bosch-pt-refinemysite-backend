/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.company.company.CompanyId
import org.springframework.data.jpa.repository.JpaRepository

interface EmployableUserCompanyNameRepository :
    JpaRepository<EmployableUserCompanyName, CompanyId> {
  fun findOneById(id: CompanyId): EmployableUserCompanyName?
}
