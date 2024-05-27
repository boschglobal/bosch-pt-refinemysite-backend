/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.user.query

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
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

  fun loadUserProjectionByCiamId(ciamId: String): UserProjection? =
      repository.findByCiamUserIdentifier(ciamId)

  private fun UserAggregateAvro.toNewProjection() =
      UserProjection(
          this.getIdentifier().asUserId(),
          this.getVersion(),
          this.userId,
          this.admin,
          this.locked,
          toLocale(this.locale))

  private fun UserProjection.updateFromUserAggregate(aggregate: UserAggregateAvro) =
      this.apply {
        version = aggregate.getVersion()
        admin = aggregate.admin
        locked = aggregate.locked
        locale = toLocale(aggregate.locale)
      }
}
