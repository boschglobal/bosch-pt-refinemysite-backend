/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.api

data class CreateFeatureCommand(val featureName: String)

data class EnableFeatureCommand(val featureName: String)

data class DisableFeatureCommand(val featureName: String)

data class ActivateFeatureWhitelistCommand(val featureName: String)

data class DeleteFeatureCommand(val featureName: String)
