/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import java.util.UUID

/** this class is meant to be used in a Spring Data dynamic projection (Spring Data DTO) */
data class UuidIdentifierDto(val identifier: UUID)
