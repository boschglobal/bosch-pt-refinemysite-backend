/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

open class CustomLinkBuilderFactory {

  /**
   * Since [CustomLinkBuilder] is used many times during a single request and the construction of
   * this prefix is expensive, this factory is instantiated as a request scoped bean at
   * [CustomLinkBuilderFactoryAutoConfiguration]
   */
  private val prefix =
      ServletUriComponentsBuilder.fromCurrentServletMapping().toUriString() +
          getCurrentApiVersionPrefix()

  open fun linkTo(endpoint: String): CustomLinkBuilder = CustomLinkBuilder(prefix).slash(endpoint)
}
