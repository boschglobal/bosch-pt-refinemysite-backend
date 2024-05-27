/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.dto.CraftTranslation

class CreateCraftResource(val translations: Set<CraftTranslation> = emptySet())
