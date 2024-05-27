/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization

import java.util.UUID
import java.util.function.Function
import java.util.function.UnaryOperator

object AuthorizationDelegation {

  /**
   * @param elements the identifiers for which to check if the current user is authenticated
   * @param mapSourceToTargetFunction a function that translates identifiers (e.g. ProjectCraft
   *   identifiers) to different identifiers (e.g. Project identifiers) understood by the delegation
   *   target
   * @param authorizationFunction a method on another authorization component (the delegation
   *   target)
   * @return set of identifiers of authorized elements
   */
  fun delegateAuthorizationForIdentifiers(
      elements: Collection<UUID>,
      mapSourceToTargetFunction: Function<Collection<UUID>, Set<AuthorizationDelegationDto>>,
      authorizationFunction: UnaryOperator<Set<UUID>>
  ): Set<UUID> {

    // Step 1: map source elements (keys) to their target elements (values)
    val sourceToTargetMap =
        mapSourceToTargetFunction.apply(elements).associate {
          it.sourceIdentifier to it.targetIdentifier
        }

    // Step 2: check authorization for all referenced target objects
    val targetElements = sourceToTargetMap.values.toSet()
    val authorizedTargetElements = authorizationFunction.apply(targetElements)

    // Step 3: based on the source-to-target mapping, return only those source elements for which
    // the mapped target element is authorized
    return sourceToTargetMap.entries
        .filter { authorizedTargetElements.contains(it.value) }
        .map { it.key }
        .toSet()
  }

  /**
   * @param elements the elements for which to check if the current user is authenticated
   * @param mapSourceToTargetFunction a function that translates an element of type S to an element
   *   of type T understood by the delegation target
   * @param authorizationFunction a method on another authorization component (the delegation
   *   target)
   * @return set of authorized elements
   */
  fun <S, T> delegateAuthorization(
      elements: Collection<S>,
      mapSourceToTargetFunction: Function<S, T>,
      authorizationFunction: UnaryOperator<Set<T>>
  ): Set<S> {
    // Step 1: map each source element (keys) to its target element (values)
    val sourceToTargetMap = elements.associateWith { mapSourceToTargetFunction.apply(it) }

    // Step 2: check authorization for all referenced target objects
    val targetElements: Set<T> = sourceToTargetMap.values.toSet()
    val authorizedTargetElements = authorizationFunction.apply(targetElements)

    // Step 3: based on the source-to-target mapping, return only those source elements for which
    // the mapped target element is authorized
    return sourceToTargetMap.entries
        .filter { authorizedTargetElements.contains(it.value) }
        .map { it.key }
        .toSet()
  }
}
