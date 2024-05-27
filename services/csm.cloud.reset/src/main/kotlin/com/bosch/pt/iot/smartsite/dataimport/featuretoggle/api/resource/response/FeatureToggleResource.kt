/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_links") class FeatureToggleResource(
  val name: String,
  val displayName: String,
  val state: String,
  val id: UUID,
) : AuditableResource()
