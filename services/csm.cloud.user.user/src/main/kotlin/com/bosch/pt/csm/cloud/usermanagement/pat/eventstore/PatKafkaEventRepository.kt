/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import org.springframework.data.repository.Repository

interface PatKafkaEventRepository : Repository<PatKafkaEvent, Long>
