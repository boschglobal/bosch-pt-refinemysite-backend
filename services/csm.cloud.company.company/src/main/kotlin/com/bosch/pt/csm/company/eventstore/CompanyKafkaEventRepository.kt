/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.eventstore

import org.springframework.data.repository.Repository

interface CompanyKafkaEventRepository : Repository<CompanyContextKafkaEvent, Long> {
  fun count(): Long
}
