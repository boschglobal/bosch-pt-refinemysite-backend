/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.exception

import org.apache.kafka.clients.consumer.ConsumerRecord

class RestoreServiceAheadOfOnlineServiceException(record: ConsumerRecord<*, *>) :
    RuntimeException(
        "The restore service must not process a record that is not yet processed by the online service. " +
            "Waiting for online service to process record from topic ${record.topic()}, partition " +
            "${record.partition()}, offset ${record.offset()}",
    )
