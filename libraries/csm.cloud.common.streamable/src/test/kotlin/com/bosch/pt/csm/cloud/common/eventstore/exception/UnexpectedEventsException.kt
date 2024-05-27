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

class UnexpectedEventsException(eventTypes: List<SpecificRecordBase>, occurrences: Int) :
    AssertionFailedError(
        "$occurrences event(s) were created that were not expected. " +
            "Types are: ${eventTypes.joinToString("") { it.javaClass.simpleName }}}",
        0,
        occurrences)
