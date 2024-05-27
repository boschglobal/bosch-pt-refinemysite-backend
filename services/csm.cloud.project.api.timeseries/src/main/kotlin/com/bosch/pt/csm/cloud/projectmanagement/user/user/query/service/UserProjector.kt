/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.valueOf
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.craft.craft.domain.asCraft
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.PhoneNumber
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.PhoneNumberTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjectionMapper
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserVersion
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository.UserProjectionRepository
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import java.time.LocalDateTime
import org.apache.commons.lang3.LocaleUtils.toLocale
import org.springframework.stereotype.Component

@Component
class UserProjector(private val repository: UserProjectionRepository) {

  fun onUserEvent(aggregate: UserAggregateAvro) {
    if (aggregate.registered) {
      val existingUser = repository.findOneByIdentifier(aggregate.getIdentifier().asUserId())

      if (existingUser == null || aggregate.getVersion() > existingUser.version) {
        (existingUser?.updateFromUserAggregate(aggregate) ?: aggregate.toNewProjection()).apply {
          repository.save(this)
        }
      }
    }
  }

  fun onUserDeletedEvent(key: AggregateEventMessageKey) {
    if (repository.existsById(key.aggregateIdentifier.identifier.asUserId())) {
      repository.deleteById(key.aggregateIdentifier.identifier.asUserId())
    }
  }

  private fun UserAggregateAvro.toNewProjection(): UserProjection {
    val userVersion = this.newUserVersion()

    return UserProjectionMapper.INSTANCE.fromUserVersion(
        userVersion, this.getIdentifier().asUserId(), listOf(userVersion))
  }

  private fun UserProjection.updateFromUserAggregate(aggregate: UserAggregateAvro): UserProjection {
    val userVersion = aggregate.newUserVersion()

    return UserProjectionMapper.INSTANCE.fromUserVersion(
        userVersion, this.identifier, this.history.toMutableList().also { it.add(userVersion) })
  }

  private fun UserAggregateAvro.newUserVersion(): UserVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return UserVersion(
        version = this.getVersion(),
        idpIdentifier = this.userId,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        position = this.position,
        eulaAcceptedDate = this.eulaAcceptedDate?.toLocalDateByMillis(),
        admin = this.admin,
        locked = this.locked,
        locale = toLocale(this.locale),
        country = this.country?.let { valueOf(this.country.name) },
        crafts = this.crafts.map { it.identifier.toUUID().asCraft() },
        phoneNumbers =
            this.phoneNumbers.map {
              PhoneNumber(
                  it.countryCode,
                  PhoneNumberTypeEnum.valueOf(it.phoneNumberType.name),
                  it.callNumber)
            },
        auditUser,
        auditDate)
  }
}
