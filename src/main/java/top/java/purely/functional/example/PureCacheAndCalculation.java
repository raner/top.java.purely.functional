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
import java.util.Objects;
import org.organicdesign.fp.collections.PersistentHashSet;
import org.organicdesign.fp.collections.PersistentVector;
import org.organicdesign.fp.collections.UnmodCollection;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodIterator;
import static org.organicdesign.fp.StaticImports.vec;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.NON_COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.ADDITION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.EXPONENTIATION;
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
    enum Commutativity
    {
        COMMUTATIVE(PersistentHashSet.empty()),
        NON_COMMUTATIVE(PersistentVector.empty());
        
        final UnmodCollection<BigInteger> operands;

        Commutativity(UnmodCollection<BigInteger> collection)
        {
            operands = collection;
        }
    }

    class Calculation
    {
        private Operation operation;
        private final UnmodIterable<BigInteger> operands;

        public Calculation(Operation operation, Result... operands)
        {
            this(operation, vec(operands));
        }

        public Calculation(Operation operation, UnmodIterable<Result> operands)
        {
            this.operation = operation;
            this.operands = operation.commutativity.operands.concat(operands.map(Result::value));
        }

        public Result calculate()
        {
            UnmodIterator<BigInteger> iterator = operands.iterator();
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
        EXPONENTIATION(NON_COMMUTATIVE)
        {
            BigInteger calculate(BigInteger a, BigInteger b)
            {
                return a.pow(b.intValue());
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
        UnmodIterable<Node> operands;

        Node(Operation operation, Node... operands)
        {
            this.operation = operation;
            this.operands = vec(operands);
        }

        Result calculate()
        {
            Calculation calculation = new Calculation(operation, operands.map(Node::calculate));
            return calculation.calculate();
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
        Result calculate()
        {
            return new Result(value);
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
    
    Node pow(Node left, Node right)
    {
        return new Node(EXPONENTIATION, left, right);
    }

    Node constant(BigInteger value)
    {
        return new LeafNode(value);
    }
}
