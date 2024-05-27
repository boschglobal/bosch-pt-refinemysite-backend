/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.exceptions

/** Exception to throw if resource cannot be found.  */
class ResourceNotFoundException : RuntimeException {

    val messageKey: String

    /**
     * Constructor to initialize exception with a message key to translate.
     *
     * @param messageKey the message key to translate
     */
    constructor(messageKey: String) {
        this.messageKey = messageKey
    }

    /**
     * Constructor to initialize exception with a message key to translate and error cause.
     *
     * @param messageKey the message key to translate
     * @param cause the cause
     */
    constructor(messageKey: String, cause: Throwable) : super(cause) {
        this.messageKey = messageKey
    }
}
