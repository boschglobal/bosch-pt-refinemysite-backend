/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.model

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.EMPLOYEE
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Employee_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Employee_LastModifiedBy")))
@Table(indexes = [Index(name = "UK_Employee_Identifier", columnList = "identifier", unique = true)])
class Employee : AbstractReplicatedEntity<Long> {

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_Employee_User"))
  @ManyToOne(fetch = LAZY, optional = false)
  var user: User? = null

  @field:Size(min = 1)
  @ElementCollection
  @CollectionTable(
      name = "EMPLOYEE_ROLE",
      foreignKey = ForeignKey(name = "FK_Employee_Role_EmployeeId"),
      joinColumns = [JoinColumn(name = "EMPLOYEE_ID")])
  @Enumerated(STRING)
  @Column(columnDefinition = "varchar(255)")
  var roles: MutableList<EmployeeRoleEnum>? = null

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_Employee_Company"))
  @ManyToOne(fetch = LAZY, optional = false)
  var company: Company? = null

  constructor()

  constructor(
      identifier: UUID?,
      version: Long?,
      user: User?,
      company: Company?,
      roles: List<EmployeeRoleEnum>?
  ) {
    this.identifier = identifier
    this.version = version
    this.user = user
    this.company = company
    if (roles == null) {
      this.roles = ArrayList()
    } else {
      this.roles = roles.toMutableList()
    }
  }

  override fun setId(id: Long?) = super.setId(id)

  override fun getDisplayName(): String? = user?.getDisplayName()

  override fun getAggregateType(): String = EMPLOYEE.value

  companion object {
    private const val serialVersionUID: Long = -2494955316637492753

    @JvmStatic
    fun fromAvroMessage(
        aggregate: EmployeeAggregateAvro,
        company: Company?,
        user: User?,
        createdBy: User?,
        lastModifiedBy: User?
    ): Employee =
        Employee(
                aggregate.aggregateIdentifier.identifier.toUUID(),
                aggregate.aggregateIdentifier.version,
                user,
                company,
                aggregate.roles.map { EmployeeRoleEnum.valueOf(it.name) })
            .apply {
              setCreatedBy(createdBy)
              setLastModifiedBy(lastModifiedBy)
              setCreatedDate(aggregate.auditingInformation.createdDate.toLocalDateTimeByMillis())
              setLastModifiedDate(
                  aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis())
            }
  }
}
