/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.model

import com.bosch.pt.iot.smartsite.company.model.CompanyBuilder.Companion.company
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum.FM
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

class EmployeeBuilder private constructor() {

  private val roles: MutableList<EmployeeRoleEnum> = ArrayList()
  private var user: User? = null
  private var company: Company? = null
  private var createdDate = LocalDateTime.now()
  private var lastModifiedDate = LocalDateTime.now()
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var identifier: UUID? = null
  private var version: Long? = null

  fun withIdentifier(identifier: UUID?): EmployeeBuilder = apply { this.identifier = identifier }

  fun withVersion(version: Long?): EmployeeBuilder = apply { this.version = version }

  fun withUser(user: User?): EmployeeBuilder = apply { this.user = user }

  fun withRole(role: EmployeeRoleEnum): EmployeeBuilder = apply {
    this.roles.clear()
    this.roles.add(role)
  }

  fun withCompany(company: Company?): EmployeeBuilder = apply { this.company = company }

  fun withCreatedDate(createdDate: LocalDateTime?): EmployeeBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): EmployeeBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun withCreatedBy(createdBy: User?): EmployeeBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: User?): EmployeeBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun build(): Employee =
      Employee(identifier, null, user, company, roles).apply {
        if (this@EmployeeBuilder.createdDate != null) {
          setCreatedDate(this@EmployeeBuilder.createdDate)
        }
        if (this@EmployeeBuilder.lastModifiedDate != null) {
          setLastModifiedDate(this@EmployeeBuilder.lastModifiedDate)
        }
        setCreatedBy(this@EmployeeBuilder.createdBy)
        setLastModifiedBy(this@EmployeeBuilder.lastModifiedBy)
        identifier = this@EmployeeBuilder.identifier
        version = this@EmployeeBuilder.version
      }

  companion object {

    @JvmStatic
    fun employee(): EmployeeBuilder =
        EmployeeBuilder()
            .withIdentifier(randomUUID())
            .withVersion(0L)
            .withRole(FM)
            .withCompany(company().build())
            .withUser(user().build())
  }
}
