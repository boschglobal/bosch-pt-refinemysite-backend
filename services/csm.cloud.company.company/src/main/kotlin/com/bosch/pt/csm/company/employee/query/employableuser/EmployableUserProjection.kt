/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.common.model.AbstractPersistableEntity
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.user.user.model.GenderEnum
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import java.util.Date
import java.util.UUID

@Entity
@Table(
    name = "PROJECTION_EMPLOYABLE_USER",
    indexes =
        [
            Index(name = "UK_Email", columnList = "email", unique = true),
            Index(
                name = "IX_UserName_CompanyName",
                columnList = "userName, companyName",
                unique = false),
            Index(
                name = "IX_CompanyName_UserName",
                columnList = "companyName, userName",
                unique = false)])
class EmployableUserProjection(
    id: UserId,

    // firstName
    var firstName: String? = null,

    // lastName
    var lastName: String? = null,

    // userName
    var userName: String? = null,

    // email
    var email: String? = null,

    // admin
    var admin: Boolean = false,

    // locked
    var locked: Boolean = true,

    // gender
    @Enumerated(STRING) @Column(columnDefinition = "varchar(255)") var gender: GenderEnum? = null,

    // userCreatedDate
    @Temporal(TIMESTAMP) var userCreatedDate: Date? = null,

    // userCountry
    @Enumerated(STRING)
    @Column(columnDefinition = "varchar(255)")
    var userCountry: IsoCountryCodeEnum? = null,

    // companyId
    @Embedded
    @AttributeOverride(name = "identifier", column = Column(name = "companyIdentifier"))
    var companyIdentifier: CompanyId? = null,

    // companyName
    var companyName: String? = null,

    // employeeId
    @Embedded
    @AttributeOverride(name = "identifier", column = Column(name = "employeeIdentifier"))
    var employeeIdentifier: EmployeeId? = null,

    // employeeCreatedDate
    @Temporal(TIMESTAMP) var employeeCreatedDate: Date? = null
) : AbstractPersistableEntity<UserId>(id), Referable {

  override fun getIdentifierUuid(): UUID = id.toUuid()

  override fun getDisplayName(): String = "$firstName $lastName"

  override fun getId(): UserId = id
}
