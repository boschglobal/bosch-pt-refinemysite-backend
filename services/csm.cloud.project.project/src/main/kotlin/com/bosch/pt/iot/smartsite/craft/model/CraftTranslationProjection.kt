/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.model

import java.util.UUID

data class CraftTranslationProjection(
    val craftId: UUID,
    val defaultName: String,
    val locale: String?,
    val value: String?
)
