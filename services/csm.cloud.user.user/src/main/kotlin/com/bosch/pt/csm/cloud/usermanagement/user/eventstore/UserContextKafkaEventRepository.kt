/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import org.springframework.data.repository.Repository

interface UserContextKafkaEventRepository : Repository<UserContextKafkaEvent, Long> {
  fun count(): Long
}
