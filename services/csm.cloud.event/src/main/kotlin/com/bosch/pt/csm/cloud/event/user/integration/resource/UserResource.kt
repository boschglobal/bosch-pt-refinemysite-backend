/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2016
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.user.integration.resource

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

class UserResource(@JsonProperty("id") var identifier: UUID)
