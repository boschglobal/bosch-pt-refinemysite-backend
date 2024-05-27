/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.model

@Suppress("UnnecessaryAbstractClass")
abstract class UserBasedImport(open val createWithUserId: String? = null)
