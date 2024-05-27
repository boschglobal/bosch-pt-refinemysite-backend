/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.buildDefaultProfilePictureUri
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.buildWithFallback
import java.util.function.Supplier

fun Participant.referToWithPicture(
    deletedUserReference: Supplier<ResourceReference>
): ResourceReferenceWithPicture =
    if (this.user!!.deleted)
        ResourceReferenceWithPicture(
            this.getIdentifierUuid(),
            deletedUserReference.get().displayName,
            buildDefaultProfilePictureUri())
    else ResourceReferenceWithPicture.from(this, buildWithFallback(this.user!!.profilePicture))
