/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject

class Company(
    override val id: String,
    val version: Long? = null,
    val name: String? = null,
    val streetAddress: StreetAddress? = null,
    val postBoxAddress: PostBoxAddress? = null
) : ImportObject
