/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategy

@Deprecated("to be removed once all aggregated are migrated to arch 2.0")
interface ProjectContextRestoreDbStrategy : RestoreDbStrategy
