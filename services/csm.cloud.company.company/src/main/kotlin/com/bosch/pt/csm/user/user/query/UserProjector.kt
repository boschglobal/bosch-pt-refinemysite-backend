/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.user.query

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.valueOf
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedDate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getLastModifiedDate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import org.apache.commons.lang3.LocaleUtils.toLocale
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class UserProjector(private val repository: UserProjectionRepository) {

  fun onUserEvent(aggregate: UserAggregateAvro) {
    if (aggregate.registered)
        (repository
                .findOneById(aggregate.getIdentifier().asUserId())
                ?.updateFromUserAggregate(aggregate)
                ?: aggregate.toNewProjection())
            .apply { repository.save(this) }
  }

  fun onUserDeletedEvent(key: AggregateEventMessageKey) {
    if (repository.existsById(key.aggregateIdentifier.identifier.asUserId())) {
      repository.deleteById(key.aggregateIdentifier.identifier.asUserId())
    }
  }

  private fun UserAggregateAvro.toNewProjection() =
      UserProjection(
          this.getIdentifier().asUserId(),
          this.getCreatedByUserIdentifier().asUserId(),
          this.getCreatedDate().toDate(),
          this.getLastModifiedByUserIdentifier().asUserId(),
          this.getLastModifiedDate().toDate(),
          this.getVersion(),
          this.userId,
          this.firstName,
          this.lastName,
          this.email,
          this.admin,
          this.locked,
          toLocale(this.locale),
          this.country?.let { valueOf(this.country.name) })

  private fun UserProjection.updateFromUserAggregate(aggregate: UserAggregateAvro) =
      this.apply {
        createdBy = aggregate.getCreatedByUserIdentifier().asUserId()
        createdDate = aggregate.getCreatedDate().toDate()
        lastModifiedBy = aggregate.getLastModifiedByUserIdentifier().asUserId()
        lastModifiedDate = aggregate.getLastModifiedDate().toDate()
        version = aggregate.getVersion()
        firstName = aggregate.firstName
        lastName = aggregate.lastName
        email = aggregate.email
        admin = aggregate.admin
        locked = aggregate.locked
        locale = toLocale(aggregate.locale)
        country = aggregate.country?.let { valueOf(aggregate.country.name) }
      }
}
