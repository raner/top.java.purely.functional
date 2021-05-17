//                                                                          //
// Copyright 2021 Mirko Raner                                               //
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import top.java.purely.functional.example.PurePlayAndInteraction.Input.Response;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static pro.projo.Projo.creates;
import static top.java.purely.functional.example.PurePlayAndInteraction.Input.Response.CORRECT;
import static top.java.purely.functional.example.PurePlayAndInteraction.Input.Response.TOO_BIG;
import static top.java.purely.functional.example.PurePlayAndInteraction.Input.Response.TOO_SMALL;

/**
* The {@link PurePlayAndInteraction} class provides an example that demonstrates how
* interactivity can be achieved in a functional-reactive program. The example uses a
* number guessing game where the program will try to guess a number chosen by the
* user.
*
* The functional state of the game consists of the highest possible number, the lowest
* possible number, and the latest guess. The game logic takes a state, a user input,
* and a normally distributed random number and calculates the next game state based
* on that information.
*
* @author Mirko Raner
**/
public class PurePlayAndInteraction
{
  Random random = new Random(); // TODO: impure

  interface State
  {
    pro.projo.triples.Factory<State, Integer, Integer, Optional<Float>> FACTORY =
      creates(State.class).with(State::lowestPossible, State::highestPossible, State::random);

    Integer lowestPossible();
    Integer highestPossible();
    Optional<Float> random();

    default State next(final Input input)
    {
      if (guess().isPresent()) // A previous guess was made and the user provided feedback:
      {
        switch (input.response())
        {
          case TOO_BIG: return FACTORY.create(lowestPossible(), guess().get()-1, input.random());
          case TOO_SMALL: return FACTORY.create(guess().get()+1, highestPossible(), input.random());
          default: return FACTORY.create(guess().get(), guess().get(), Optional.empty());
        }
      }
      else // This will be the first guess; user input will be ignored:
      {
        return FACTORY.create(1, 100, input.random());
      }
    }

    default Optional<Integer> guess()
    {
      int spread = highestPossible() - lowestPossible();
      return random().map(it -> lowestPossible() + Math.round(it*spread));
    }

    default boolean stillGuessing()
    {
      return highestPossible() != lowestPossible() || guess().isPresent();
    }
  }

  interface Input
  {
    enum Response {TOO_BIG, TOO_SMALL, CORRECT}

    pro.projo.doubles.Factory<Input, Optional<Float>, Response> FACTORY =
      creates(Input.class).with(Input::random, Input::response);

    Optional<Float> random();
    Response response();
  }

  // TODO: this method is clearly impure
  Double random()
  {
    return 0.5 + Math.min(Math.max(random.nextGaussian(), -5), 5) / 10;
  }

  Response response(String input)
  {
    Object[][] mappings =
    {
      {'b', TOO_BIG}, {'i', TOO_BIG}, {'g', TOO_BIG},
      {'s', TOO_SMALL}, {'m', TOO_SMALL}, {'a', TOO_SMALL}, {'l', TOO_SMALL},
      {'c', CORRECT}, {'r', CORRECT}, {'e', CORRECT}
    };
    Map<Integer, Response> map = Stream.of(mappings).collect(Collectors.toMap(it -> (int)(((Character)it[0]).charValue()), it -> (Response)it[1]));
    return input.chars().mapToObj(map::get).filter(Objects::nonNull).findAny().orElse(CORRECT);
  }

  String state(State state)
  {
    if (state.guess().isPresent())
    {
      return "Are you thinking of the number " + state.guess().get() + "?";
    }
    return "Hit return to start the game.";
  }

  /**
  * Provides the bridge between the functional-reactive example code and Java's side-effect-based
  * console I/O. This method bootstraps the example object instance and subscribes to the standard
  * output.
  **/
  public static void main(String... arguments)
  {
    PurePlayAndInteraction instance = new PurePlayAndInteraction();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    Flowable<Optional<Float>> random =
      Flowable.fromIterable(Stream.generate(instance::random).map(Double::floatValue)::iterator).map(Optional::of);
    Flowable<String> prologue = Flowable.just
    (
      "Welcome to the Number Guessing Game!",
      "Please think of a number between 1 and 100 (inclusive) and hit the return key when you're ready.",
      "I will try to guess your number, and after each guess you can respond with one of these options:",
      "- too big",
      "- too small",
      "- correct",
      "Alternatively, you can also just enter a characteristic letter (e.g., b/i/g, s/m/a/l, c/r/e)"
    );
    Flowable<String> inputs = Flowable.fromIterable(reader.lines()::iterator);
    Flowable<Response> responses = inputs.map(instance::response);
    Flowable<Input> combination = Flowable.zip(random, responses, Input.FACTORY::create);
    Flowable<State> states = combination.scan(State.FACTORY.create(1, 100, Optional.empty()), State::next);
    Flowable<String> game = Flowable.concat(prologue, states.takeWhile(State::stillGuessing).map(instance::state));
    Flowable.concat(game, Flowable.just("Good bye!")).subscribe(System.out::println);
  }

  /**
  * Creates a functional-reactive pseudo-random number generator (PRNG). This method uses the
  * same linear-congruential PRNG that is used by {@link java.util.Random#next(int)}.
  *
  * @param seed the seed value (often the current system time)
  * @param max the maximum value to be generated (exclusive)
  * @return an {@link Observable} of an endless stream of random numbers
  **/
  Flowable<Integer> random(long seed, int max)
  {
    BiFunction<Long, Emitter<Long>, Long> emit = (next, emitter) -> {emitter.onNext(next); return next;};
    UnaryOperator<Long> next = number -> (number * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    return Flowable.generate(() -> seed, emit.andThen(next)::apply).skip(1).
      map(number -> (int)(abs((int)(number >>> 16))/(MAX_VALUE/(double)max)));
  }
}
