package top.java.purely.functional.example;

import java.math.BigInteger;
import java.util.Objects;
import org.organicdesign.fp.collections.PersistentHashSet;
import org.organicdesign.fp.collections.PersistentVector;
import org.organicdesign.fp.collections.UnmodCollection;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodIterator;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static org.organicdesign.fp.StaticImports.vec;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Commutativity.NON_COMMUTATIVE;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.ADDITION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.EXPONENTIATION;
import static top.java.purely.functional.example.PureCacheAndCalculation.Operation.MULTIPLICATION;

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

        public Calculation(Operation operation, BigInteger... operands)
        {
            this(operation, vec(operands));
        }
        
        public Calculation(Operation operation, UnmodIterable<BigInteger> operands)
        {
            this.operation = operation;
            this.operands = operation.commutativity.operands.concat(operands);
        }

        public BigInteger calculate()
        {
            UnmodIterator<BigInteger> iterator = operands.iterator();
            return operation.calculate(iterator.next(), iterator.next());
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
    
    class Addition extends Calculation
    {
        Addition(BigInteger left, BigInteger right)
        {
            super(Operation.ADDITION, left, right);
        }

        public boolean equals(Object other)
        {
            return other instanceof Addition;// &&
                
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
                BigInteger c = a.pow(b.intValue());
                System.err.println(a + "^" + b + "=" + c);
                return c;
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
    
    class Node
    {
        Operation operation;
        UnmodIterable<Node> operands;
        Node(Operation operation, Node... operands)
        {
            
        }
        
        BigInteger calculate()
        {
            Calculation calculation = new Calculation(operation, operands.map(Node::calculate));
            return calculation.calculate();
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
        return null;
    }
    
    //                (1) +
    //                   / \
    //              (2) *   \
    //                 / \   \
    //            (3) +   2   + (4)
    //               / \     / \
    //          (5) *   2   2   * (6)
    //             / \         / \ 
    //        (7) ^   10     10   ^ (8)
    //           / \             / \
    //      (9) +   \      (10) *   \
    //         / \   \         / \   \
    //        2   2   ^ (11)  2   2   ^ (12)
    //               / \             / \
    //              2   ^ (13)      2   ^ (14)
    //                 / \             / \
    //                2   * (15)      2   * (16)
    //                   / \             / \
    //                  2   10          2   10
    //
    Node example()
    {
        return add // 1
        (
            mul //2
            (
                add // 3
                (
                    mul // 5
                    (
                        pow // 7
                        (
                            add // 9
                            (
                                constant(TWO),
                                constant(TWO)
                            ),
                            pow // 11
                            (
                                constant(TWO),
                                pow // 13
                                (
                                    constant(TWO),
                                    mul // 15
                                    (
                                        constant(TWO),
                                        constant(TEN)
                                    )
                                )
                            )
                        ),
                        constant(TEN)
                    ),
                    constant(TWO)
                ),
                constant(TWO)
            ),
            add // 4
            (
                constant(TWO),
                mul // 6
                (
                    constant(TEN),
                    pow // 8
                    (
                        mul // 10
                        (
                            constant(TWO),
                            constant(TWO)
                        ),
                        pow // 12
                        (
                            constant(TWO),
                            pow // 14
                            (
                                constant(TWO),
                                mul // 16
                                (
                                    constant(TWO),
                                    constant(TEN)
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    Calculation result()
    {
        //example()
        return null;
    }
    
    public static void main(String[] args)
    {
        BigInteger FOUR = BigInteger.valueOf(4);
        System.err.println("b=" + Operation.EXPONENTIATION.calculate(FOUR, BigInteger.valueOf(10000000)));
    }
}
