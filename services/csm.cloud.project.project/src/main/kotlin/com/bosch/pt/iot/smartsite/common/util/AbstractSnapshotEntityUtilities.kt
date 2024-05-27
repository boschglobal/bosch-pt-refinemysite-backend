/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.util

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable

object AbstractSnapshotEntityUtilities {

  fun <T : UuidIdentifiable> Collection<T>.sortByIdentifier() = this.sortedBy { it.identifier }
}
