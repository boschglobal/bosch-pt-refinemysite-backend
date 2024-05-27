/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.eventstore

import org.springframework.data.repository.Repository

interface ProjectContextKafkaEventRepository : Repository<ProjectContextKafkaEvent, Long> {
  fun count(): Long
}
