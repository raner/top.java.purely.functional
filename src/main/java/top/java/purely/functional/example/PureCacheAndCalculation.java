//                                                                          //
// Copyright 2022 Mirko Raner                                               //
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import org.pcollections.HashTreePBag;
import org.pcollections.HashTreePMap;
import org.pcollections.PCollection;
import org.pcollections.PMap;
import org.pcollections.TreePVector;
import static java.util.stream.Collectors.toList;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.NON_COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.ADDITION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.EXPONENTIATION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.MODULATION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.MULTIPLICATION;

/**
* The {@link PureCacheAndCalculation} example demonstrates how intermediate results can be cached
* using a purely functional and therefore immutable (!) cache.
* This example also demonstrates the use of the Paguro purely functional data structure library.
*
* @author Mirko Raner
**/
public class PureCacheAndCalculation
{
    static BigInteger MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    enum Commutativity
    {
        COMMUTATIVE(HashTreePBag.empty()),
        NON_COMMUTATIVE(TreePVector.empty());
        
        final PCollection<BigInteger> operands;

        Commutativity(PCollection<BigInteger> collection)
        {
            operands = collection;
        }
    }

    class Calculation
    {
        private Operation operation;
        private final PCollection<BigInteger> operands;

        public Calculation(Operation operation, Result... operands)
        {
            this(operation, TreePVector.from(Arrays.asList(operands)));
        }

        public Calculation(Operation operation, PCollection<Result> operands)
        {
            this.operation = operation;
            this.operands = operation.commutativity.operands.plusAll(operands.stream().map(Result::value).collect(toList()));
        }

        public Result calculate()
        {
            Iterator<BigInteger> iterator = operands.iterator();
            return new Result(operation.calculate(iterator.next(), iterator.next()));
        }

        @Override
        public boolean equals(Object other)
        {
            if (other instanceof Calculation)
            {
                return ((Calculation)other).operation == operation
                    && ((Calculation)other).operands.equals(operands);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(operation, operands);
        }

        @Override
        public String toString()
        {
            return "Calculation(" + operation + ")";
        }
    }

    enum Operation
    {
        ADDITION(COMMUTATIVE)
        {
            BigInteger calculate(BigInteger a, BigInteger b)
            {
                return a.add(b);
            }
        },
        MULTIPLICATION(COMMUTATIVE)
        {
            BigInteger calculate(BigInteger a, BigInteger b)
            {
                return a.multiply(b);
            }
        },
        MODULATION(NON_COMMUTATIVE)
        {
            BigInteger calculate(BigInteger a, BigInteger b)
            {
                return a.mod(b);
            }
        },
        EXPONENTIATION(NON_COMMUTATIVE)
        {
            BigInteger calculate(BigInteger a, BigInteger b)
            {
                return a.pow(b.intValue()).mod(MAX_VALUE);
            }
        };

        final Commutativity commutativity;

        Operation(Commutativity commutativity)
        {
            this.commutativity = commutativity;
        }

        Commutativity commutativity()
        {
            return commutativity;
        }

        abstract BigInteger calculate(BigInteger a, BigInteger b);
    }

    interface State<STATE, PRIMARY> extends Function<STATE, Entry<STATE, PRIMARY>>
    {
      default <OTHER> State<STATE, OTHER> map(Function<PRIMARY, OTHER> function)
      {
        State<STATE, PRIMARY> that = this;
        return state ->
        {
          Entry<STATE, PRIMARY> value = that.apply(state);
          return Map.entry(value.getKey(), function.apply(value.getValue()));
        };
      }

      default <OTHER> State<STATE, OTHER> flatMap(Function<PRIMARY, State<STATE, OTHER>> function)
      {
        State<STATE, PRIMARY> that = this;
        return state ->
        {
          Entry<STATE, PRIMARY> value = that.apply(state);
          return function.apply(value.getValue()).apply(value.getKey());
        };
      }
    }

    class Cache
    {
        final PMap<Calculation, Result> cache;
        final int hits;

        Cache()
        {
            cache = HashTreePMap.empty();
            hits = 0;
        }

        Cache(PMap<Calculation, Result> cache, int hits)
        {
            this.cache = cache;
            this.hits = hits;
        }

        Entry<Cache, Result> cached(Calculation calculation, Function<Calculation, Result> calculate)
        {
            // To disable the cache:
            // return Map.entry(this, calculate.apply(calculation));

            Result cached = cache.get(calculation);
            if (cached != null)
            {
                // Cache hit: return the current cache and the cached value:
                //
                return Map.entry(new Cache(cache, hits+1), cached);
            }
            else
            {
                // Cache miss: calculate the result and return a new cache that includes the result:
                //
                Result result = calculate.apply(calculation);
                return Map.entry(new Cache(cache.plus(calculation, result), hits), result);
            }
        }
    }

    class Result
    {
        BigInteger value;

        public Result(BigInteger value)
        {
            this.value = value;
        }

        BigInteger value()
        {
            return value;
        }
        
        public String toString()
        {
            String string = value.toString();
            if (string.length() <= 32)
            {
                return string;
            }
            else
            {
                return string.substring(0,7)
                    + "...(+" + (string.length()-14) + " digits)..."
                    + string.substring(string.length()-7);
            }
        }
    }

    class Node
    {
        Operation operation;
        Node[] operands;

        Node(Operation operation, Node... operands)
        {
            this.operation = operation;
            this.operands = operands;
        }

        State<Cache, Result> calculate()
        {
            return cache ->
            {
                State<Cache, Result> left = operands[0].calculate();
                Entry<Cache, Result> leftEntry = left.apply(cache);
                State<Cache, Result> right = operands[1].calculate();
                Entry<Cache, Result> rightEntry = right.apply(leftEntry.getKey());
                Calculation calculation = new Calculation(operation, leftEntry.getValue(), rightEntry.getValue());
                Entry<Cache, Result> entry = rightEntry.getKey().cached(calculation, Calculation::calculate);
                return entry;
            };
        }
    }

    class LeafNode extends Node
    {
        BigInteger value;
        LeafNode(BigInteger value)
        {
            super(null);
            this.value = value;
        }
        
        @Override
        State<Cache, Result> calculate()
        {
            return cache ->
            {
                return Map.entry(cache, new Result(value));
            };
        }
    }

    Node add(Node left, Node right)
    {
        return new Node(ADDITION, left, right);
    }
    
    Node mul(Node left, Node right)
    {
        return new Node(MULTIPLICATION, left, right);
    }
    
    Node mod(Node left, Node right)
    {
        return new Node(MODULATION, left, right);
    }
    
    Node pow(Node left, Node right)
    {
        return new Node(EXPONENTIATION, left, right);
    }

    Node constant(BigInteger value)
    {
        return new LeafNode(value);
    }
}
