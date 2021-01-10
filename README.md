[![Codacy Badge](https://api.codacy.com/project/badge/Grade/bb7de1b3ea4e4256997e6b1fac66281b)](https://app.codacy.com/gh/rchillyard/Number?utm_source=github.com&utm_medium=referral&utm_content=rchillyard/Number&utm_campaign=Badge_Grade)
[![CircleCI](https://circleci.com/gh/rchillyard/Number.svg?style=svg)](https://circleci.com/gh/rchillyard/Number)


# Number
This project is about numbers and their mathematics.
The chief features of this library are:
* all numbers are exact wherever it is possible;
* inexact numbers are represented along with their error bounds;
* lazy evaluation to help avoid temporary inexact values which become part of a result;
* there are several domains of Number (expressed with different "factors") to support angles, logarithms. 

There is no such thing as accidental loss of precision (at least, provided that code follows the recommendations).
For example, if you write:

    val x = 1 / 2

your x will be an Int of value 0.

However, if you write:

    val x: Number = 1 / 2

then x will be a Number with value exactly one half.

Introduction
============
The Number project provides mathematical utilities where error bounds are tracked (and not forgotten).
All functions handle the transformation or convolution of error bounds appropriately.
When the error bound is sufficiently large compared to a number, that number is considered to be zero.
This implies that when comparing numbers, any significant overlap of their error bounds will result in them testing
as equal (according to the _compare_ function, but not the _equals_ function).

Numbers are represented internally as either _Int_, _BigInt_, _Rational_, or _Double_.
For more detail, see Representation below.

It is of course perfectly possible to use the _Rational_ classes directly, without using the _Number_ (or _Expression_) classes.

Parsing
=======
A number with two or fewer decimal places is considered exact--a number with more than two decimal places is
considered fuzzy.
You can always override this by adding "*" or "..." to the end of a number with fewer than two DPs,
or by adding two 0s to the end of a number with more than two decimal places.

In general, the form of a number to be parsed is:
  
    number ::= value? factor?
    factor ::= "Pi" | "pi" | "PI" | 𝛑 | 𝜀
    value ::= sign? nominalValue fuzz* exponent*
    nominalValue ::= integerPart ( "." fractionalPart )? | rational
    rational ::= digits "/" digits
    integerPart ::= digits
    fractionalPart ::= digits
    fuzz ::= "..." | "*" | "(" digits ")"
    exponent ::= E sign? digits

Note that the __e__ and __pi__ symbols are, respectively,
(in unicode):   \uD835\uDF00 and \uD835\uDED1 (&#xD835;&#xDF00; and &#xD835;&#xDED1;)  
A number must have at least one of the value or factor components.
If no explicit factor is specified, then the number will be a _Scalar_ (an ordinary number).
If you want to get exact trigonometric values, then it's important to specify the factor as pi (or e).

Number creation
===============
Parsing, described above is really the most precise way of specifying numerical values.
But, of course, it's a lot easier to write code that uses numerical literals.
For _Int_ and _Long_, these give us no problems, of course.
Neither is there any issue with _Rational_, _BigDecimal_, and _BigInt_.
_BigDecimal_ values are represented internally by _Rational_.
There are two ways to specify _Rational_ numbers:

* one is to create a _String_ of the form r"n/d" where n and d represent the numerator and the denominator;
* the other way is simply to write n:/d (again n and d are as above).

Either of these methods will require importing the appropriate implicit classes from _Rational_.
It's probably the simplest just to include:

    import Rational._

_Doubles_ are where the trickiest conversions apply.
Writing something like _Number(3.1415927)_ will result in a _FuzzyNumber_ with error bounds of 5 * 10^-7.
To be consistent with the _String_ representation, _Number(1.25)_ will result in an _ExactNumber_ represented internally
by a _Rational_ of 5/4.
However, if you want to force a number like 3.1415927 to be exact, then you will need to write

    Number("3.141592700")

Rendering
=========
Generally speaking, the output String corresponding to a Number will be the same as the input String,
although at this stage of the software, that is not guaranteed.
Numbers followed by "(xx)" show standard scientific notation where xx represents the standard deviation of the error
with respect to the last two digits (sometimes there is only one x which corresponds to the last digit).
If a Number is followed by "[xx]," this corresponds to a "box" (i.e. truncated uniform) error distribution.

Comparison
==========
Comparison between _Numbers_ is based on, first, equality of value.
If, after any scaling for the factors is taken into account, the two values compare equal, then the Numbers are equal.
For _ExactNumber_, comparison ends there.
However, for _FuzzyNumber_, it is then determined whether there is significant overlap
between the fuzz of the two numbers.
If the overlap is sufficient that there is deemed to be a 50% probability that the numbers are really the same,
then the comparison yields 0 (equal).
Additionally, each of the methods involved has a signature which includes a p value (the confidence probability).
  
Representation
==============
There are two kinds of _Number_: _ExactNumber_ and _FuzzyNumber_.
A _FuzzyNumber_ has a fuzz quantity which is an optional _Fuzz[Double]_.
The "value" of a _Number_ is represented by the following type:

    type Value = Either[Either[Either[Option[Double], Rational], BigInt], Int]

Thus, an integer x is represented by _Right(x)_.
A _BigInt_ x is represented by _Left(Right(x))_.
A _Rational_ x is represented by a _Left(Left(Right(x)))_.
A _Double_ x is represented by a _Left(Left(Left(Some(x))))_.
There is also an invalid _Number_ case which is represented by _Left(Left(Left(None)))_.

This _Value_ is always of the rightmost type possible: given the various possible specializations.
Thus, an _Int_ x which is in range will be represented by _Right(x)_.
Thus, an _BigInt_ x outside the Int range will be represented by _Left(Right(x))_.
Similarly, a _Rational_ with numerator x and unit denominator will be represented by _Left(Right(x))_
(unless it can be further specialized as a _Right(x)_).
It is also possible that a _Double_ x will be represented by a _Left(Left(Right(x)))_.
For this to happen, the value in question must have fewer than three decimal places (similar to the parsing scheme).

NOTE: It is expected that we will remove the possibility of a _BigInt_ representation since this is just a special case
of the _Rational_ representation.

Factors
=======
There are three "factors:" Scalar (for ordinary dimensionless numbers), __Pi__ (used to represent radians or any multiple of pi),
and __E__ (for multiples of the Euler constant).
Trigonometrical functions are designed to work with __Pi__.
For example, if you want to check that the sine of pi/2 is equal to 1 exactly, then you should write the following:

    val target = (Number.pi/2).sin
    target shouldBe Number.one

Similarly, if you use the _atan_ method on a Scalar number, the result will be a number (possibly exact) whose factor is __Pi__.

Lazy Evaluation
===============
Version 1.0.3 supports lazy evaluation via a trait called _Expression_.
The advantage of lazy evaluation is not so much performance.
That's going to be neither here nor there.
But it is in avoiding precision loss in some circumstances.

For example, suppose an expression you are working on involves the square root of, say, 7.
However, you don't particularly pay attention to the fact that later on in the calculation, you square everything.
If you don't use lazy evaluation, your final result will have an error bound, even though the true value should be
proportional to exactly 7.

It's important to realize of course that you have to use the Expression mechanism.

      it should "give precise result for sqrt(7)^2" in {
        val seven: Expression = Number(7)
        val x = seven.sqrt
        val y = x ^ 2
        y shouldBe Number(7)
        y.materialize should matchPattern { case ExactNumber(_, _) => }
      }
      it should "show ^2 and sqrt for illustrative purposes" in {
        val seven = Number(7)
        val x = seven.sqrt
        val y = x power 2
        y shouldEqual Number(7)
        y shouldBe Number(7)
      }

The second test fails with "7.000000000000001 was not equal to 7," although if we do a fuzzy compare,
using a custom equality test, we can at least make y shouldEqual 7 work.

The current set of expression optimizations is somewhat limited, but it catches the most important cases. 

Versions
========
The Current version is 1.0.4

Version 1.0.3 implements lazy evaluation.

Version 1.0.2 Included fixing the bug mentioned in 1.0.1 (actually a Rational bug), as well as adding the :/ operator
and many other fixes/features.

Version 1.0.1 Fixed many issues with minor inconsistencies.
Most important, perhaps, was the implementation of _compare_, along with _signum_ and _isZero_.
Each of these has, significantly, a signature with a confidence value (the default value is 0.5).

**However**, there remains a particularly serious bug which causes a deadlock situation in the runtime (who knew that was even possible?).

Initial version is 1.0.0

Future Upgrades
===============
To remove the BigInt option of Value.