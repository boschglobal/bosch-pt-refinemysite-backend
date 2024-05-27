/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.i18n.Key.USER_DELETED
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.function.Supplier
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

@LibraryCandidate("Similar (more concise) impl in company service!", "csm.cloud.common.web")
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractResourceFactoryHelper
protected constructor(private val messageSource: MessageSource) {

  private var deletedUserReferenceSupplier: Supplier<ResourceReference>? = null

  /**
   * Supplies a ResourceReference to represent a deleted user. The user's identifier is different on
   * every invocation of [Supplier.get].
   */
  val deletedUserReference: Supplier<ResourceReference>
    get() {
      if (deletedUserReferenceSupplier == null) {
        deletedUserReferenceSupplier = Supplier {
          ResourceReference.from(
              object : Referable {
                override fun getIdentifierUuid(): UUID = randomUUID()

                override fun getDisplayName(): String =
                    messageSource.getMessage(
                        USER_DELETED, arrayOf(), LocaleContextHolder.getLocale())
              })
        }
      }
      return deletedUserReferenceSupplier!!
    }
}
