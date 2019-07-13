//                                                                          //
// Copyright 2019 Mirko Raner                                               //
//                                                                          //
// Licensed under the Apache License, Version 2.0 (the "License");          //
// you may not use this file except in compliance with the License.         //
// You may obtain a copy of the License at                                  //
//                                                                          //
//     http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                          //
// Unless required by applicable law or agreed to in writing, software      //
// distributed under the License is distributed on an "AS IS" BASIS,        //
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. //
// See the License for the specific language governing permissions and      //
// limitations under the License.                                           //
//                                                                          //
package top.java.purely.functional.example;

import java.text.Format;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import io.reactivex.Observable;
import io.reactivex.schedulers.Timed;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

/**
* The {@link PureTimeAndRandomness} class provides some examples that demonstrate how the
* concepts of time and randomness can be handled in a purely functional fashion in Java.
* It leverages the RxJava 2 API for abstracting the state-changing elements of the code in a
* functional-reactive way.
* <br>
* In imperative programming, random numbers and current time are often treated as functions,
* e.g., {@link Math#random()} and {@link System#currentTimeMillis()}. The lack of parameters
* on both methods already indicates that they cannot be pure functions. Similar to the
* paradigm shift that is necessary for inversion of control and dependency injection, it is
* necessary to look at these concepts in a different way if the goal is to model them in a
* purely functional way. If a piece of code needs information about time, it needs an additional
* <i>parameter</i> for that. That is, time is "injected" as an argument, rather than obtained
* by calling another function. The same thing is true for randomness. If a pure function needs
* to make a decision based on a random number then that random number has to be provided as
* input into the function.
* <br>
* {@link Observable#timestamp()} and the {@link Timed} class are used for dual purposes:
* 1) to inject a single snapshot of the current system time into the code as a seed value
* for a random number generator, and 2) to obtain the system time before and after execution
* of a piece of code (to determine how long the execution took).
* The Java 8 Stream API is used for creating a simple pseudo-random number generator based on
* an infinite stream of numbers.
* <br>
* Running this class will produce output about how long it took to execute a method that
* just consists of a sleep statement. Typically, the measured execution time will be a few
* milliseconds longer than the sleep time because threads don't resume instantaneously.
* <br>
* This class is not much more than 50 lines of actual code, but since its real purpose is
* explaining purely functional programming (which is not always straightforward) it contains
* <i>a lot</i> of comments.
*
* @author Mirko Raner
**/
public class PureTimeAndRandomness
{
  /**
  * Provides the bridge between the reactive example code and Java's side-effect-based
  * console I/O. This method bootstraps the example and injects the current system time
  * as a seed for the random number generator. Random sleep times are configured to be
  * chosen between 0 and 2500 milliseconds.
  * <br>
  * As a bridge to non-pure, non-functional code, this method is the only method in the
  * example that cannot be considered pure, because (among other things) it uses
  * {@link java.io.PrintStream#println()} to cause the side effect of something showing up on
  * the console. However, this impurity is completely isolated and localized to this method,
  * and it does not bleed into the rest of the code.
  **/
  public static void main(String... arguments)
  {
    Observable.just(new PureTimeAndRandomness()).timestamp().
      map(timed -> timed.value().timeRandomSleeps(timed.value().random(timed.time(), 2500))).
      subscribe(execution -> execution.subscribe(System.err::println));
  }

  /**
  * Provides an example method whose execution time is to be measured. The method will sleep for a
  * given number of milliseconds and return its input. Even this method is technically pure: it returns
  * the same output for the same input (in fact, its output <i>is</i> its input), and it does not
  * have any state-changing side effects. The only "side effect" it has is that it takes a variable
  * amount of time depending on its input. However, the same is true for most methods, including pure
  * ones, so this can hardly be counted as an impure side effect.
  *
  * @param input the arbitrarily typed input of the method
  * @param getSleepTime a {@link Function} to extract the sleep time from the input
  * @return always the original input argument (this facilitates running the method in a
  * functional-reactive processing pipeline)
  **/
  <_Type_> _Type_ sleepForGivenAmountOfMilliseconds(_Type_ input, Function<_Type_, Duration> getSleepTime)
  {
    sleepUninterruptibly(getSleepTime.apply(input).toMillis(), MILLISECONDS);
    return input;
  }

  /**
  * Creates a functional-reactive pseudo-random number generator (PRNG). This method uses the
  * same linear-congruential PRNG that is used by {@link java.util.Random#next(int)}.
  *
  * @param seed the seed value (often the current system time)
  * @param max the maximum value to be generated (exclusive)
  * @return an {@link Observable} of an endless stream of random numbers
  **/
  Observable<Integer> random(long seed, int max)
  {
    final double ratio = MAX_VALUE/(double)max;
    final UnaryOperator<Long> prng = number -> (number * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    final Stream<Long> seeds = Stream.iterate(seed, prng).skip(1);
    final Stream<Integer> random = seeds.map(number -> (int)(number >>> 16));
    return Observable.fromIterable(random::iterator).map(number -> (int)(abs(number)/ratio));
  }

  /**
  * Performs the actual measurement of individual execution times (i.e., it individually times a
  * series of method invocations). This method performs the heavy lifting by transforming the input
  * into the output via this series of steps:
  * <ul>
  *  <li> <b>input</b> of random integers indicating sleep time
  *   &rarr; {@link Observable}&lt;{@link Integer}&gt;
  *  <li> actual sleep durations
  *   &rarr; {@link Observable}&lt;{@link Duration}&gt;
  *  <li> time-stamped durations, before timed method is executed
  *   &rarr; {@link Observable}&lt;{@link Timed}&lt;{@link Duration}&gt;&gt;
  *  <li> time-stamped durations, after timed method was executed
  *   &rarr; {@link Observable}&lt;{@link Timed}&lt;{@link Duration}&gt;&gt;
  *  <li> durations with both start and finish time stamp
  *   &rarr; {@link Observable}&lt;{@link Timed}&lt;{@link Timed}&lt;{@link Duration}&gt;&gt;&gt;
  *  <li> <b>output</b> to console, intended for {@link System#out}/{@link System#err}
  *   &rarr; {@link Observable}&lt;{@link String}&gt;
  * </ul>
  * As this method is purely functional it cannot just make calls to
  * {@link System#out}{@link java.io.PrintStream#println(String) .println(...)} to inform the user about
  * its progress. Instead, this method returns an {@link Observable} which then can be subscribed to by
  * a consumer (see {@link #main(String...) main} method). In a functional-reactive manner, this method
  * transforms one random number into one output message that includes information about the timed
  * method execution.
  * The key to purely functional execution time measurement, very similar to the imperative pattern,
  * is to have two time stamps, one before and one after the execution. This is reflected by the nested
  * {@link Timed}&lt;{@link Timed}&lt;...&gt;&gt; types, which is a bit unwieldy in Java. However, Java
  * was not designed for this style of programming, so one cannot expect the code to look particularly
  * elegant.
  *
  * @param randomNumbers an infinite series of random numbers
  * @return an {@link Observable} of messages intended for the console
  **/
  Observable<String> timeRandomSleeps(Observable<Integer> randomNumbers)
  {
    final Format message = new MessageFormat("Sleeping for {0}ms was measured to take {1}ms (off by {2}ms)");
    final Observable<Timed<Timed<Duration>>> timedStartsAndFinishes = randomNumbers.
      map(Duration::ofMillis).timestamp().
      map(sleep -> sleepForGivenAmountOfMilliseconds(sleep, Timed::value)).timestamp();
    return timedStartsAndFinishes.map(timed ->
    {
      final long slept = timed.value().value().toMillis();
      final long started = timed.value().time();
      final long finished = timed.time();
      final long measured = finished - started;
      final Object[] info = {slept, measured, measured - slept};
      return message.format(info);
    });
  }
}