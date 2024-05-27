/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.KafkaEventTestRepository

/**
 * This repository is only used for tests and offers spring data jpa access to project events. It is
 * mainly used to verify event generation.
 */
interface CompanyKafkaEventTestRepository : KafkaEventTestRepository<CompanyContextKafkaEvent>
