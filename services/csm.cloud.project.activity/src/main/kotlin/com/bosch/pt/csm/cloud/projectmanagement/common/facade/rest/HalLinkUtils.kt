/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest

import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder

fun Array<String>.linkTemplateWithPathSegments(): UriComponentsBuilder =
    ServletUriComponentsBuilder.fromCurrentServletMapping().pathSegment(*this)

fun String.linkTemplateWithPathSegments(): UriComponentsBuilder =
    ServletUriComponentsBuilder.fromCurrentServletMapping().pathSegment(this)
