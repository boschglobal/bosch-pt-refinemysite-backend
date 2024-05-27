/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.common.model.AbstractPersistableEntity
import com.bosch.pt.csm.company.company.CompanyId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "PROJECTION_EMPLOYABLE_USER_COMPANY_NAME")
class EmployableUserCompanyName(

    // identifier
    id: CompanyId,

    // companyName
    @Column(nullable = false) var companyName: String
) : AbstractPersistableEntity<CompanyId>(id) {
  override fun getId(): CompanyId = id
}
