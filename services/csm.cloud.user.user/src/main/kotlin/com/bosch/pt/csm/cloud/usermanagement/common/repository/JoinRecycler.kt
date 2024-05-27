/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.repository

import java.util.Optional
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
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

  fun <S, T> join(from: From<*, S>, target: SingularAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.INNER))

  fun <S, T> join(from: From<*, S>, target: SetAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.INNER))

  fun <S, T> join(from: From<*, S>, target: ListAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.INNER))

  fun <S, T> joinLeft(from: From<*, S>, target: SingularAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.LEFT))

  fun <S, T> joinLeft(from: From<*, S>, target: SetAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.LEFT))

  fun <S, T> joinLeft(from: From<*, S>, target: ListAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.LEFT))

  fun <S, T> joinRight(from: From<*, S>, target: SingularAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.RIGHT))

  fun <S, T> joinRight(from: From<*, S>, target: SetAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.RIGHT))

  fun <S, T> joinRight(from: From<*, S>, target: ListAttribute<S, T>) =
      RecyclableJoin(joinOrRecycle(from, target, JoinType.RIGHT))

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SingularAttribute<S, T>,
      joinType: JoinType
  ): Join<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SingularAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): Join<S, T> =
      getJoinWithAlias(from, alias).let {
        @Suppress("UNCHECKED_CAST")
        when (it.isPresent) {
          true -> it.get() as Join<S, T>
          else -> from.join(target, joinType).apply { alias(alias) }
        }
      }

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SetAttribute<S, T>,
      joinType: JoinType
  ): SetJoin<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: SetAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): SetJoin<S, T> =
      getJoinWithAlias(from, alias).let {
        @Suppress("UNCHECKED_CAST")
        when (it.isPresent) {
          true -> it.get() as SetJoin<S, T>
          else -> from.join(target, joinType).apply { alias(alias) }
        }
      }

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: ListAttribute<S, T>,
      joinType: JoinType
  ): ListJoin<S, T> = joinOrRecycle(from, target, createAlias(from, target, joinType), joinType)

  private fun <S, T> joinOrRecycle(
      from: From<*, S>,
      target: ListAttribute<S, T>,
      alias: String,
      joinType: JoinType
  ): ListJoin<S, T> =
      getJoinWithAlias(from, alias).let {
        @Suppress("UNCHECKED_CAST")
        when (it.isPresent) {
          true -> it.get() as ListJoin<S, T>
          else -> from.join(target, joinType).apply { alias(alias) }
        }
      }

  private fun <S> getJoinWithAlias(from: From<*, S>, alias: String): Optional<Join<S, *>> =
      from.joins.filter { it.alias != null }.firstOrNull { it.alias == alias }?.let {
        Optional.of(it)
      }
          ?: Optional.empty()

  private fun <S, T> createAlias(
      from: From<*, S>,
      target: Attribute<S, T>,
      joinType: JoinType
  ): String {
    val fromName = if (from.alias != null) from.alias else from.javaType.simpleName
    val targetName = target.javaMember.name
    return "${fromName}_${joinType.name}_$targetName"
  }

  class RecyclableJoin<S, T> constructor(private val join: Join<S, T>) {
    fun <U> joinLeft(target: SetAttribute<T, U>) =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.LEFT))

    fun <U> joinLeft(target: SingularAttribute<T, U>) =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.LEFT))

    fun <U> joinRight(target: SetAttribute<T, U>) =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.RIGHT))

    fun <U> joinRight(target: SingularAttribute<T, U>) =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.RIGHT))

    fun <U> join(target: SingularAttribute<T, U>): RecyclableJoin<T, U> =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.INNER))

    fun <U> join(target: SetAttribute<T, U>) =
        RecyclableJoin(joinOrRecycle(join, target, JoinType.INNER))

    operator fun <Y> get(attribute: SingularAttribute<in T, Y>): Path<Y> = join[attribute]

    operator fun <E, C : Collection<E>> get(collection: PluralAttribute<T, C, E>): Expression<C> =
        join[collection]

    operator fun <K, V, M : Map<K, V>> get(map: MapAttribute<T, K, V>): Expression<M> = join[map]

    /** @return the wrapped join object */
    fun get(): Join<S, T> = join
  }
}
