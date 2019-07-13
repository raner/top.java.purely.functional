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
import java.util.concurrent.Callable;
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
* It leverages the RxJava2 API for abstracting the state-changing elements of the code in a
* functional-reactive way.
* <br>
* In imperative programming, random numbers and current time are often treated as functions
* (e.g., {@link Math#random()} and {@link System#currentTimeMillis()}). The lack of parameters
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
*
* @author Mirko Raner
**/
public class PureTimeAndRandomness
{
  /**
   * Provides the bridge between the reactive example code and Java's side-effect based
   * console I/O. This method bootstraps the example and injects the current system time
   * as a seed for the random number generator.
   */
  public static void main(String... arguments)
  {
    Observable.just(new PureTimeAndRandomness()).timestamp()
      .map(timed -> timed.value().timeRandomSleeps(timed.time()))
      .subscribe(observable -> observable.subscribe(System.err::println));
  }

  /**
   * Provides the method whose execution time we want to measure. The method will sleep for a
   * given number of millisecods.
   *
   * @param input the arbitrarily typed input of the method
   * @param getSleepTime a {@link Function} to extract the sleep time (in milliseconds) from the input
   * @return the original input
   */
  <_Type_> _Type_ sleepForGivenAmountOfMilliseconds(_Type_ input, Function<_Type_, Integer> getSleepTime)
  {
    sleepUninterruptibly(getSleepTime.apply(input), MILLISECONDS);
    return input;
  }

  Observable<String> timeRandomSleeps(long currentTimeMillis)
  {
    /**
     * Same linear-congruential PRNG as used by {@link java.util.Random#next(int)}.
     */
    final UnaryOperator<Long> prng = seed -> (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    final Stream<Long> seeds = Stream.iterate(currentTimeMillis, prng).skip(1);
    final Stream<Integer> randoms = seeds.map(seed -> (int)(seed >>> 16));

    final Observable<Integer> random =
      Observable.fromIterable(randoms::iterator).map(number -> abs(number)/(MAX_VALUE/3000));
    final Observable<Callable<Timed<Integer>>> randomSleep =
      random.timestamp().map(sleep -> () -> sleepForGivenAmountOfMilliseconds(sleep, Timed::value));

    final Format message = new MessageFormat("Sleeping for {0}ms was measured to take {1}ms (off by {2}ms)");
    final Observable<Timed<Timed<Integer>>> execute = randomSleep.map(Callable::call).timestamp();
    return execute.map((Timed<Timed<Integer>> result) ->
    {
      final int slept = result.value().value();
      final long started = result.value().time();
      final long finished = result.time();
      final long measured = finished - started;
      final Object[] info = {slept, measured, measured - slept};
      return message.format(info);
    });
  }
}

