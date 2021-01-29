//                                                                          //
// Copyright 2019 - 2021 Mirko Raner                                        //
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

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.IntStream;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Timed;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.text.MessageFormat.format;
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
* {@link Flowable#timestamp()} and the {@link Timed} class are used for dual purposes:
* 1) to inject a single snapshot of the current system time into the code as a seed value
* for a random number generator, and 2) to obtain the system time before and after execution
* of a piece of code (to determine how long the execution took).
* The RxJava 2 API is also used for creating a simple pseudo-random number generator based on
* an infinite observable of numbers.
* <br>
* Running this class will produce output about how long it took to execute a method that
* just consists of a sleep statement. Typically, the measured execution time will be a few
* milliseconds longer than the sleep time because sleeping threads may not always resume
* instantaneously.
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
  * Provides the bridge between the functional-reactive example code and Java's side-effect-based
  * console I/O. This method bootstraps the example and injects the current system time
  * as a seed for the random number generator (it does the latter in a purely functional manner,
  * though). Random sleep times are configured to be chosen between 0 and 2000 milliseconds.
  * <br>
  * As a bridge to surrounding non-pure, non-functional code, this method is the only method in the
  * example that cannot be considered pure, because (among other things) it uses
  * {@link java.io.PrintStream#println()} to cause the side effect of something showing up on
  * the console. However, this impurity is completely isolated and localized to this method,
  * and it does not bleed into the rest of the code.
  **/
  public static void main(String... arguments)
  {
    PureTimeAndRandomness instance = new PureTimeAndRandomness();
    Function<Integer, Duration> timedMethod =
      sleep -> instance.sleepForGivenAmountOfMilliseconds(Duration.ofMillis(sleep));
    Flowable.
      just(instance).timestamp().
      map(timed -> instance.timeMethodWithRandomInputs(instance.random(timed.time(), 2000), timedMethod)).
      subscribe(execution -> execution.subscribe(System.err::println));
  }

  /**
  * Provides an example method whose execution time is to be measured. The method will sleep for a
  * given number of milliseconds and return its input, which makes it easy to validate if the measured
  * time is accurate: the measured time should only be a few milliseconds more than the requested sleep
  * time.
  * <br>
  * Even this method is technically pure: it returns the same output for the same input (in fact, its
  * output always the same as its input), and it does not have any state-changing side effects. The
  * only "side effect" it has is that it takes a variable amount of time to complete, depending on its
  * input. However, the same is true for most methods, including pure ones, so this can hardly be
  * counted as an impure side effect.
  *
  * @param sleepTime the amount of milliseconds the method should sleep
  * @return always the original input argument (this facilitates running the method in a
  * functional-reactive processing pipeline)
  **/
  Duration sleepForGivenAmountOfMilliseconds(Duration sleepTime)
  {
    sleepUninterruptibly(sleepTime.toMillis(), MILLISECONDS);
    return sleepTime;
  }

  /**
  * Creates a functional-reactive pseudo-random number generator (PRNG). This method uses the
  * same linear-congruential PRNG that is used by {@link java.util.Random#next(int)}.
  *
  * @param seed the seed value (often the current system time)
  * @param max the maximum value to be generated (exclusive)
  * @return an {@link Flowable} of an endless stream of random numbers
  **/
  Flowable<Integer> random(long seed, int max)
  {
    return Flowable.fromIterable(IntStream.generate(() -> 0)::iterator).
      scan(seed, (number, ignore) -> (number * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)).
      skip(1).map(number -> (int)(abs((int)(number >>> 16))/(MAX_VALUE/(double)max)));
  }

  /**
  * Performs the actual measurement of individual execution times (i.e., it individually times a
  * series of method invocations). This method performs the heavy lifting by transforming the input
  * into the output via this series of steps:
  * <ul>
  *  <li> <b>input</b> of random integers indicating sleep time
  *   &rarr; {@link Flowable}&lt;{@link Integer}&gt;
  *  <li> time-stamped durations, before timed method is executed
  *   &rarr; {@link Flowable}&lt;{@link Timed}&lt;{@link Integer}&gt;&gt;
  *  <li> time-stamped durations, after timed method was executed
  *   &rarr; {@link Flowable}&lt;{@link Timed}&lt;{@link Integer}&gt;&gt;
  *  <li> durations with both start and finish time stamp
  *   &rarr; {@link Flowable}&lt;{@link Timed}&lt;{@link Timed}&lt;{@link Integer}&gt;&gt;&gt;
  *  <li> <b>output</b> to console, intended for {@link System#out}/{@link System#err}
  *   &rarr; {@link Flowable}&lt;{@link String}&gt;
  * </ul>
  * As this method is purely functional it cannot just make calls to
  * {@link System#out}{@link java.io.PrintStream#println(String) .println(...)} to inform the user about
  * its progress. Instead, this method returns an {@link Flowable} which then can be subscribed to by
  * a consumer (see {@link #main(String...) main} method). In a functional-reactive manner, this method
  * transforms one random number into one output message that includes information about the timed
  * method execution.
  * The key to purely functional time measurement, very similar to the imperative pattern,
  * is to have two time stamps, one before and one after the execution. This is reflected by the nested
  * {@link Timed}&lt;{@link Timed}&lt;...&gt;&gt; types, which is a bit unwieldy in Java. However, Java
  * was not designed for this style of programming, so one cannot expect the code to look particularly
  * elegant.
  *
  * @param random an infinite series of random numbers
  * @param method the method to be timed
  * @return an {@link Flowable} of messages intended for the console
  **/
  Flowable<String> timeMethodWithRandomInputs(Flowable<Integer> random, Function<Integer, ?> method)
  {
    return random.
      timestamp().
      map(input -> returnFirst(input, method.apply(input.value()))).
      timestamp().
      map((Timed<Timed<Integer>> timed) ->
      {
        final Integer input = timed.value().value();
        final long started = timed.value().time();
        final long finished = timed.time();
        final long measured = finished - started;
        return format("Calling timed method with argument {0} took {1}ms", input, measured);
      });
  }

  /**
  * Always returns the first argument (but forces evaluation of all other arguments).
  * (Have a look at
  * <a href="https://stackoverflow.com/questions/50895231">https://stackoverflow.com/questions/50895231</a>
  * if you have any doubt that this is a highly useful function)
  *
  * @param <ANY> the type of the first argument (and the method's return type)
  * @param first the first argument
  * @param arguments the rest of the arguments
  * @return always the first argument
  **/
  <ANY> ANY returnFirst(ANY first, Object... arguments)
  {
      return first;
  }
}
