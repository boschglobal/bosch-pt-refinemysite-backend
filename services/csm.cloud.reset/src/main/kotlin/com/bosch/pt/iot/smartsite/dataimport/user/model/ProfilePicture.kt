/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import org.springframework.core.io.Resource

class ProfilePicture(
    override val id: String,
    val version: Long? = null,
    val userId: String,
    val path: String,
    var resource: Resource? = null
) : ImportObject
