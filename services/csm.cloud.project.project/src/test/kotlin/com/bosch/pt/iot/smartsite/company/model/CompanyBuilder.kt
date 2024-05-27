/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

class CompanyBuilder private constructor() {

  private var name: String? = null
  private var createdDate = LocalDateTime.now()
  private var lastModifiedDate = LocalDateTime.now()
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var identifier: UUID? = null
  private var version: Long? = null
  private var streetAddress: StreetAddress? = null
  private var postBoxAddress: PostBoxAddress? = null

  fun withName(name: String?): CompanyBuilder = apply { this.name = name }

  fun withCreatedDate(createdDate: LocalDateTime?): CompanyBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): CompanyBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun withCreatedBy(createdBy: User?): CompanyBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: User?): CompanyBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withIdentifier(identifier: UUID?): CompanyBuilder = apply { this.identifier = identifier }

  fun withVersion(version: Long?): CompanyBuilder = apply { this.version = version }

  fun withStreetAddress(streetAddress: StreetAddress?): CompanyBuilder = apply {
    this.streetAddress = streetAddress
  }

  fun withPostBoxAddress(postBoxAddress: PostBoxAddress?): CompanyBuilder = apply {
    this.postBoxAddress = postBoxAddress
  }

  fun build(): Company =
      Company(name = name).apply {
        if (this@CompanyBuilder.createdDate != null) {
          setCreatedDate(this@CompanyBuilder.createdDate)
        }
        if (this@CompanyBuilder.lastModifiedDate != null) {
          setLastModifiedDate(this@CompanyBuilder.lastModifiedDate)
        }
        setCreatedBy(this@CompanyBuilder.createdBy)
        setLastModifiedBy(this@CompanyBuilder.lastModifiedBy)
        identifier = this@CompanyBuilder.identifier
        version = this@CompanyBuilder.version
        streetAddress = this@CompanyBuilder.streetAddress
        postBoxAddress = this@CompanyBuilder.postBoxAddress
      }

  companion object {

    @JvmStatic
    fun company(): CompanyBuilder =
        CompanyBuilder().withIdentifier(randomUUID()).withVersion(0L).withName("ACME")
  }
}
