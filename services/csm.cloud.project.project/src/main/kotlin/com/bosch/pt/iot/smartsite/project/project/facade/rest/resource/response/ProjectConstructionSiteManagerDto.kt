/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.user.facade.rest.datastructure.PhoneNumberDto

/** Resource for the project's construction site manager. */
class ProjectConstructionSiteManagerDto(
    val displayName: String? = null,
    val position: String? = null,
    val phoneNumbers: List<PhoneNumberDto>? = null
) {

  /**
   * Creates new [ProjectConstructionSiteManagerDto] from the entity [Participant]
   *
   * @param constructionSiteManager the construction site manager user.
   */
  constructor(
      constructionSiteManager: Participant
  ) : this(
      constructionSiteManager.getDisplayName(),
      constructionSiteManager.user!!.position,
      constructionSiteManager.user!!
          .phonenumbers
          .map { PhoneNumberDto(it.phoneNumberType!!, it.countryCode!!, it.callNumber!!) }
          .sortedWith(
              Comparator.comparing(
                      { number: PhoneNumberDto -> number.phoneNumberType },
                      Comparator.naturalOrder())
                  .thenComparing({ number -> number.phoneNumber }, Comparator.naturalOrder())))
}
