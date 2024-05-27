/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.employee.shared.model.Employee
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

/** Company entity. */
@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = jakarta.persistence.ForeignKey(name = "FK_Company_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = jakarta.persistence.ForeignKey(name = "FK_Company_LastModifiedBy")))
@Table(
    indexes =
        [
          jakarta.persistence.Index(
                name = "UK_Company_Identifier", columnList = "identifier", unique = true)])
class Company : AbstractSnapshotEntity<Long, CompanyId>() {

  // Employees, only for joining
  @Suppress("UNUSED", "UnusedPrivateMember")
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
  private val employees: MutableSet<Employee> = HashSet()

  // Name
  @field:Size(min = 1, max = MAX_NAME_LENGTH)
  @Column(nullable = false, length = MAX_NAME_LENGTH)
  lateinit var name: String

  // Street address
  @Embedded
  @AttributeOverrides(
      AttributeOverride(name = "street", column = Column(name = "streetAddress_street")),
      AttributeOverride(name = "houseNumber", column = Column(name = "streetAddress_houseNumber")),
      AttributeOverride(name = "city", column = Column(name = "streetAddress_city")),
      AttributeOverride(name = "zipCode", column = Column(name = "streetAddress_zipCode")),
      AttributeOverride(name = "area", column = Column(name = "streetAddress_area")),
      AttributeOverride(name = "country", column = Column(name = "streetAddress_country")))
  var streetAddress: StreetAddress? = null

  // Post box address
  @Embedded
  @AttributeOverrides(
      AttributeOverride(name = "postBox", column = Column(name = "postBoxAddress_postBox")),
      AttributeOverride(name = "city", column = Column(name = "postBoxAddress_city")),
      AttributeOverride(name = "zipCode", column = Column(name = "postBoxAddress_zipCode")),
      AttributeOverride(name = "area", column = Column(name = "postBoxAddress_area")),
      AttributeOverride(name = "country", column = Column(name = "postBoxAddress_country")))
  var postBoxAddress: PostBoxAddress? = null

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this).appendSuper(super.toString()).append("name", name).toString()

  override fun getDisplayName(): String = name

  companion object {
    const val MAX_NAME_LENGTH: Int = 100
  }
}
