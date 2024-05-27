/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import org.springframework.data.repository.Repository

interface FeaturetoggleContextKafkaEventRepository :
    Repository<FeaturetoggleContextKafkaEvent, Long> {

  fun count(): Long
}
