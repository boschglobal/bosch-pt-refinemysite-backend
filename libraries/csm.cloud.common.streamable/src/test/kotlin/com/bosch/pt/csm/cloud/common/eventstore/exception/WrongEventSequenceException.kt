/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore.exception

import org.apache.avro.specific.SpecificRecordBase
import org.opentest4j.AssertionFailedError

class WrongEventSequenceException(
    expectedSequence: List<Class<out SpecificRecordBase>>,
    actualSequence: List<SpecificRecordBase>
) :
    AssertionFailedError(
        "Wrong sequence of event",
        expectedSequence.joinToString(",") { it.simpleName },
        actualSequence.joinToString(",") { it.javaClass.simpleName })
