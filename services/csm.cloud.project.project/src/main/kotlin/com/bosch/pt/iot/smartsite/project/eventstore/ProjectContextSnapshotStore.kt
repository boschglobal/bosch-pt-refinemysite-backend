/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.SnapshotStore

// marker interface for snapshot stores belonging to the project context
interface ProjectContextSnapshotStore : SnapshotStore
