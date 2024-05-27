/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties("_links")
class FeatureToggleListResource(
    @JsonProperty("items") val featureToggles: Collection<FeatureToggleResource> = emptyList()
)
