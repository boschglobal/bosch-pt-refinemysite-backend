/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.eventstore

import org.springframework.data.repository.Repository

interface ProjectInvitationContextKafkaEventRepository :
    Repository<ProjectInvitationContextKafkaEvent, Long> {
  fun count(): Long
}
