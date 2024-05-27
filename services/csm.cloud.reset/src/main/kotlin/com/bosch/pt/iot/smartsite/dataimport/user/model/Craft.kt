/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject

class Craft(
    override val id: String,
    val version: Long? = null,
    val translations: Set<Translation> = HashSet()
) : ImportObject
