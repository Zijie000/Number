/*
 * Copyright (c) 2025. Phasmid Software
 */

package com.phasmidsoftware.number.core.inner

import com.phasmidsoftware.number.core.Fuzz
import scala.Option.when
import scala.util.Try

/**
  * A trait representing a mathematical series, where terms can be computed
  * and evaluated up to a certain level of precision or number of terms.
  *
  * @tparam X the type of the series terms
  */
trait Series[X] {

  /**
    * Evaluates the series to a result of type X, stopping when the increment is below
    * the specified precision epsilon.
    *
    * @param epsilon the desired precision level for the evaluation; the computation halts
    *                when the improvement between terms drops below this value
    * @return a Try containing the result of the evaluation if successful, or a Failure
    *         if the series could not be evaluated
    */
  def evaluate(epsilon: Double): Try[X]

  /**
    * Evaluates the series up to a specified number of terms if provided.
    *
    * @param maybeN an optional parameter specifying the maximum number of terms to evaluate;
    *               if None, the evaluation uses all available terms
    * @return an Option containing the evaluation result of the series, or None if the evaluation cannot be performed
    */
  def evaluate(maybeN: Option[Int]): Option[X]

  /**
    * Computes the nth term of the series.
    *
    * @param n the index of the term to compute, where the index starts from 0
    * @return an `Option` containing the computed term of type `X` if it exists,
    *         or `None` if the term cannot be computed
    */
  def term(n: Int): Option[X]

  /**
    * Retrieves the total number of terms available in the series, if defined.
    *
    * @return an Option containing the number of terms in the series, or None if the number is undefined
    */
  def nTerms: Option[Int]
}

/**
  * Represents an abstract mathematical series, inheriting from the `Series` trait.
  * Provides methods for evaluating the series and accessing individual terms.
  * This class facilitates the representation and evaluation of a series where
  * terms are of type `X` and are subject to numerical operations.
  *
  * @tparam X The type of elements in the series, which must have a `Numeric` type-class instance.
  * @param terms A sequence representing the terms of the mathematical series.
  */
abstract class AbstractSeries[X: Numeric](terms: Seq[X]) extends Series[X] {

  /**
    * Evaluates the series by summing the terms until their absolute value falls below a given threshold.
    * NOTE: the result of this method is always a `Try[Number]`, regardless of `X`.
    * If X and Number are not the same, a `ClassCastException` will be thrown.
    *
    * @param epsilon The threshold value for determining the cutoff in the series evaluation.
    *                Terms with an absolute value less than this will not be included.
    * @return A `Try` containing the result of the series evaluation if successful, or an exception
    *         if the computation fails.
    * @throws ClassCastException if `X` is not `Fuzz[Double]`.
    */
  def evaluate(epsilon: Double): Try[X] = {
    val triedX = Try(terms.takeWhile(x => math.abs(implicitly[Numeric[X]].toDouble(x)) > epsilon).sum)
    val result: Try[Fuzz[Double]] = triedX map {
      case f: Fuzz[Double] => // NOTE that we cannot guarantee Double
        f.fuzz match {
          case Some(z) =>
            f.addFuzz(z.uncertainty(epsilon / convergenceRate))
          case None =>
            f.addFuzz(f.noFuzz.uncertainty(epsilon / convergenceRate))
        }
    }
    result.asInstanceOf[Try[X]]
  }

  /**
    * Returns the default convergence rate used for evaluating the series.
    * The convergence rate determines the threshold at which terms in the series
    * are considered sufficiently small to be ignored in the evaluation.
    * This is used to determine the error bounds when evaluating to a tolerance.
    * The error bounds are the tolerance divided by this number
    *
    * @return The convergence rate as a `Double`.
    */
  def convergenceRate: Double = 0.001

  /**
    * Retrieves the term at the specified index from the series, if the index is valid.
    *
    * A valid index is non-negative and less than the total number of terms in the series, as defined by `nTerms`.
    *
    * @param i The index of the term to retrieve.
    * @return An `Option` containing the term at the given index if the index is valid, or `None` otherwise.
    */
  def term(i: Int): Option[X] =
    when(nTerms.forall(n => i >= 0 && i < n))(terms(i))

  /**
    * Evaluates the series by summing the specified number of terms or a default number of terms if not provided.
    * If any term cannot be evaluated, the method returns `None`.
    *
    * @param maybeN An optional integer specifying the number of terms to evaluate. If not provided, the default number of terms will be used.
    * @return An `Option[X]` containing the result of the summation if all terms are successfully evaluated, or `None` if any term evaluation fails.
    */
  def evaluate(maybeN: Option[Int] = None): Option[X] = {
    val xn = implicitly[Numeric[X]]
    maybeN.orElse(nTerms) flatMap {
      n =>
        Range(0, n).foldLeft[Option[X]](Some(xn.zero)) {
          case (Some(total), i) =>
            term(i) match {
              case Some(x) =>
                Some(xn.plus(total, x))
              case _ =>
                None
            }
        }
    }
  }
}

/**
  * Represents a finite mathematical series with a fixed number of terms.
  *
  * This case class models a series consisting of a finite number of terms, which are stored in a list.
  * Each term in the series is of type `X`, constrained by an implicit `Numeric` type-class for numerical operations.
  *
  * @tparam X The type of the elements (terms) in the series, which must have an associated `Numeric` type-class.
  * @param terms A list of terms that make up the series.
  */
case class FiniteSeries[X: Numeric](terms: List[X]) extends AbstractSeries[X](terms) {
  /**
    * Calculates the number of terms in the series.
    *
    * This method returns the count of terms in the finite series as an optional integer.
    * If the series is empty, the result will still be wrapped in `Some`, indicating a count of `0`.
    *
    * @return An `Option[Int]` representing the number of terms in the series.
    */
  def nTerms: Option[Int] =
    Some(terms.length)
}

/**
  * Represents an infinite mathematical series where the terms are lazily evaluated.
  * This class extends the `AbstractSeries` class and serves as a concrete implementation
  * for series that are infinite in nature.
  *
  * @tparam X The type of elements in the series, which must have a `Numeric` type-class instance.
  * @param terms A lazy sequence representing the terms of the infinite series.
  */
case class InfiniteSeries[X: Numeric](terms: LazyList[X]) extends AbstractSeries[X](terms) {
  /**
    * Retrieves the number of terms of the series, if defined.
    *
    * @return None.
    */
  def nTerms: Option[Int] = None
}
