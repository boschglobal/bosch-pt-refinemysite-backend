/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.KafkaEventTestRepository
import org.springframework.context.annotation.Profile

@Profile("test")
interface FeaturetoggleKafkaEventTestRepository :
    KafkaEventTestRepository<FeaturetoggleContextKafkaEvent>
