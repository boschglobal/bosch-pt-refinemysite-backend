/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.eventstore.repository

import com.bosch.pt.csm.cloud.common.eventstore.KafkaEventTestRepository
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextKafkaEvent
import org.springframework.context.annotation.Profile

@Profile("test")
interface UserKafkaEventTestRepository : KafkaEventTestRepository<UserContextKafkaEvent>
