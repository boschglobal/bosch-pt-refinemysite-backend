/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.uuid

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable

fun Set<UuidIdentifiable>.toUuids() = this.map { it.toUuid() }.toSet()
