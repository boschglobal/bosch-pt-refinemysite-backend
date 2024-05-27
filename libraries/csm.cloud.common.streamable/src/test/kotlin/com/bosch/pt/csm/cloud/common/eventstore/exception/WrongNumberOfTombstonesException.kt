/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore.exception

import org.opentest4j.AssertionFailedError

class WrongNumberOfTombstonesException(aggregateType: String, expectedTime: Int, actualTime: Int) :
    AssertionFailedError(
        "$expectedTime tombstone messages for aggregate $aggregateType expected but found $actualTime",
        expectedTime,
        actualTime)
