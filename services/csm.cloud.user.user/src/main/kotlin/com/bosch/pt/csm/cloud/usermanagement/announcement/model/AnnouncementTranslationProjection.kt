/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import java.util.UUID

data class AnnouncementTranslationProjection(
    val identifier: UUID,
    val type: AnnouncementTypeEnum,
    val value: String
)
