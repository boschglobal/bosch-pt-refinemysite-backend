/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.common.command.api

import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId

interface FeaturetoggleContextEvent {
  val featureId: FeatureId
}