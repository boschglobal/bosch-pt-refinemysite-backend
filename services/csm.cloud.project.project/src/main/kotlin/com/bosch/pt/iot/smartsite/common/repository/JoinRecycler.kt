/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.repository

import java.util.Optional
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.JoinType.INNER
import jakarta.persistence.criteria.JoinType.LEFT
import jakarta.persistence.criteria.JoinType.RIGHT
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.SetJoin
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.ListAttribute
import jakarta.persistence.metamodel.MapAttribute
import jakarta.persistence.metamodel.PluralAttribute
import jakarta.persistence.metamodel.SetAttribute
import jakarta.persistence.metamodel.SingularAttribute

/**
 * Provides recyclable joins for solving the problem of duplicate joins when using JPA Criteria API.
 *
 * Consider the case when doing the same join twice using Criteria API. JPA will not recognize that
 * the existing join could be reused/recycled and will do a redundant join instead. This helper
 * takes care of this problem by annotating each join with an alias that uniquely identifies the
 * join path (e.g. task.project.participants). If a join exists already for the given alias (join
 * path), the existing join is returned. Otherwise, the desired join is performed and annotated with
 * the alias.
 *
 * The alias also encodes the join type so that an [JoinType.INNER] join won't be recycled in a case
 * that requested a [JoinType.LEFT] join instead.
 */
object JoinRecycler {

  fun <S, T> join(from: From<*, S>, target: SingularAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, INNER))

  fun <S, T> join(from: From<*, S>, target: SetAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, INNER))

  fun <S, T> join(from: From<*, S>, target: ListAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, INNER))

  fun <S, T> joinLeft(from: From<*, S>, target: SingularAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, LEFT))

  fun <S, T> joinLeft(from: From<*, S>, target: SetAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, LEFT))

  fun <S, T> joinLeft(from: From<*, S>, target: ListAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, LEFT))

  fun <S, T> joinRight(from: From<*, S>, target: SingularAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, RIGHT))

  fun <S, T> joinRight(from: From<*, S>, target: SetAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, RIGHT))

  fun <S, T> joinRight(from: From<*, S>, target: ListAttribute<S, T>): RecyclableJoin<S, T> =
      RecyclableJoin(joinOrRecycle(from, target, RIGHT))

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SingularAttribute<S, T>,
      joinType: JoinType
  ): Join<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  @Suppress("UNCHECKED_CAST")
  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SingularAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): Join<S, T> {
    val joinWithAlias = getJoinWithAlias(from, alias)

    return if (joinWithAlias.isPresent) {
      joinWithAlias.get() as Join<S, T>
    } else {
      from.join(target, joinType).apply { alias(alias) }
    }
  }

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SetAttribute<S, T>,
      joinType: JoinType
  ): SetJoin<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  @Suppress("UNCHECKED_CAST")
  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SetAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): SetJoin<S, T> {
    val joinWithAlias = getJoinWithAlias(from, alias)

    return if (joinWithAlias.isPresent) {
      joinWithAlias.get() as SetJoin<S, T>
    } else {
      from.join(target, joinType).apply { alias(alias) }
    }
  }

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: ListAttribute<S, T>,
      joinType: JoinType
  ): ListJoin<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  @Suppress("UNCHECKED_CAST")
  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: ListAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): ListJoin<S, T> {
    val joinWithAlias = getJoinWithAlias(from, alias)

    return if (joinWithAlias.isPresent) {
      joinWithAlias.get() as ListJoin<S, T>
    } else {
      from.join(target, joinType).apply { alias(alias) }
    }
  }

  private fun <S> getJoinWithAlias(
      from: From<*, S>,
      alias: String
  ): Optional<Join<S, *>> = // alias can be null if JoinRecycler wasn't used for creating a join
  Optional.ofNullable(
          from.joins
              // alias can be null if JoinRecycler wasn't used for creating a join
              .filter { it.alias != null }
              .firstOrNull { it.alias == alias })

  private fun <S, T> createAlias(
      from: From<*, S>,
      target: Attribute<S, T>,
      joinType: JoinType
  ): String {
    val fromName = if (from.alias != null) from.alias else from.javaType.simpleName
    val targetName = target.javaMember.name
    return fromName + "_" + joinType.name + "_" + targetName
  }

  class RecyclableJoin<S, T>(private val join: Join<S, T>) {

    fun <U> joinLeft(target: SetAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, LEFT))

    fun <U> joinLeft(target: SingularAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, LEFT))

    fun <U> joinRight(target: SetAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, RIGHT))

    fun <U> joinRight(target: SingularAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, RIGHT))

    fun <U> join(target: SingularAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, INNER))

    fun <U> join(target: SetAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, INNER))

    operator fun <Y> get(attribute: SingularAttribute<in T, Y>): Path<Y> = join.get(attribute)

    operator fun <E, C : Collection<E>?> get(collection: PluralAttribute<T, C, E>): Expression<C> =
        join.get(collection)

    operator fun <K, V, M : Map<K, V>> get(map: MapAttribute<T, K, V>): Expression<M> =
        join.get(map)

    /** @return the wrapped join object */
    fun get(): Join<S, T> = join
  }
}
