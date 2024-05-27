/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.eventstore

import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repositories extending this interface are only used for tests and offers spring data jpa access
 * to events. It is mainly used to verify event generation.
 */
@Profile("test")
interface KafkaEventTestRepository<T : AbstractKafkaEvent> : JpaRepository<T, Long>
