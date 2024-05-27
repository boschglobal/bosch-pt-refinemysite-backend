/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.model

import com.bosch.pt.iot.smartsite.dataimport.user.model.PhoneNumber

class ProjectConstructionSiteManager(
    val displayName: String? = null,
    val position: String? = null,
    val phoneNumbers: Set<PhoneNumber>? = null
)
