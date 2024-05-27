/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.Companion.fromAlternativeCountryNameExists
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_MISSING_ADDRESS
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.PostBoxAddressVo
import com.bosch.pt.csm.company.company.StreetAddressVo
import com.bosch.pt.csm.company.company.command.mapper.CompanyAvroSnapshotMapper
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.company.shared.model.PostBoxAddress
import com.bosch.pt.csm.company.company.shared.model.StreetAddress
import java.time.LocalDateTime

data class CompanySnapshot(
    override val identifier: CompanyId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdDate: LocalDateTime? = null,
    override val createdBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    val name: String,
    val streetAddress: StreetAddressVo?,
    val postBoxAddress: PostBoxAddressVo?
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      company: Company
  ) : this(
      company.identifier,
      company.version,
      company.createdDate.get(),
      company.createdBy.get(),
      company.lastModifiedDate.get(),
      company.lastModifiedBy.get(),
      company.name,
      company.streetAddress?.toStreetAddressVo(),
      company.postBoxAddress?.toPostBoxAddressVo())

  init {
    invariably(hasAtLeastOneAddress(), COMPANY_VALIDATION_ERROR_MISSING_ADDRESS)
    invariably(hasValidCountryNames(), COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
  }

  // TODO: move to super class when we see this is the right approach
  private fun invariably(requiredCondition: Boolean, failureMessageKey: String) {
    if (!requiredCondition) throw PreconditionViolationException(failureMessageKey)
  }

  private fun hasAtLeastOneAddress() = streetAddress != null || postBoxAddress != null

  private fun hasValidCountryNames() =
      (streetAddress == null || fromAlternativeCountryNameExists(streetAddress.country)) &&
          (postBoxAddress == null || fromAlternativeCountryNameExists(postBoxAddress.country))

  fun toCommandHandler() = CommandHandler.of(this, CompanyAvroSnapshotMapper)
}

fun Company.asValueObject() = CompanySnapshot(this)

fun StreetAddress.toStreetAddressVo() =
    StreetAddressVo(this.city, this.zipCode, this.area, this.country, this.street, this.houseNumber)

fun PostBoxAddress.toPostBoxAddressVo() =
    PostBoxAddressVo(this.city, this.zipCode, this.area, this.country, this.postBox)
