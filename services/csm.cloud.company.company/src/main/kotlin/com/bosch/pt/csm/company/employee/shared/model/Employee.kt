/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.EmployeeId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

/** Employee entity. */
@Entity
@Table(
    indexes =
        [
            Index(name = "UK_Employee_Identifier", columnList = "identifier", unique = true),
            Index(name = "IX_Employee_UserRef", columnList = "user_ref"),
        ])
class Employee : AbstractSnapshotEntity<Long, EmployeeId>() {

  // user ref
  @Embedded
  @AttributeOverride(name = "identifier", column = Column(name = "user_ref", nullable = false))
  lateinit var userRef: UserId

  // company
  @JoinColumn(foreignKey = ForeignKey(name = "FK_Employee_Company"))
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var company: Company

  // roles
  @field:Size(min = 1)
  @ElementCollection
  @CollectionTable(
      name = "EMPLOYEE_ROLE",
      foreignKey = ForeignKey(name = "FK_Employee_Role_EmployeeId"),
      joinColumns = [JoinColumn(name = "EMPLOYEE_ID")])
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "varchar(255)")
  var roles: MutableList<EmployeeRoleEnum>? = ArrayList()

  @ExcludeFromCodeCoverage
  override fun toString(): String = ToStringBuilder(this).appendSuper(super.toString()).toString()

  @ExcludeFromCodeCoverage override fun getDisplayName(): String = checkNotNull(id).toString()
}
