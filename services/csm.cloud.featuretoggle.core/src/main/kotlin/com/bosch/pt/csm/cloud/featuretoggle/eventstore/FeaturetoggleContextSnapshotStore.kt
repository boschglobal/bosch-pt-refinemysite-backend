/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.SnapshotStore

// marker interface for snapshot stores belonging to the user context
interface FeaturetoggleContextSnapshotStore : SnapshotStore
