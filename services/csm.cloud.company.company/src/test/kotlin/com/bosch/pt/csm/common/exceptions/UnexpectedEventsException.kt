/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.exceptions

import org.apache.avro.specific.SpecificRecordBase
import org.opentest4j.AssertionFailedError

class UnexpectedEventsException(eventTypes: List<SpecificRecordBase>, occurrences: Int) :
    AssertionFailedError(
        String.format(
            "$occurrences event(s) were created that were not expected. Types are: " +
                "${eventTypes.joinToString("") { it.javaClass.simpleName }} "),
        0,
        occurrences)
