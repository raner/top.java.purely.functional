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
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
* {@link PureCacheAndCalculationTest} is a JUnit 5 test case for verifying the correctness
* (and observing the performance) of the {@link PureCacheAndCalculation} example.
*
* @author Mirko Raner
**/
public class PureCacheAndCalculationTest extends PureCacheAndCalculation
{
    /**
    * Verifies the result for the following tree (numbers in parentheses indicate operations):
    * <pre>
    *                (1) +
    *                   / \
    *              (2) *   \
    *                 / \   \
    *            (3) +   2   * (4)
    *               / \     / \
    *          (5) ^   2   2   ^ (6)
    *             / \         / \ 
    *            2   10     10   2
    * </pre>
    **/
    @Test
    public void simpleExample()
    {
        Node example = add // 1
        (
            mul // 2
            (
                add // 3
                (
                    pow // 5
                    (
                        constant(TWO),
                        constant(TEN)
                    ),
                    constant(TWO)
                ),
                constant(TWO)
            ),
            mul // 4
            (
                constant(TWO),
                pow // 6
                (
                    constant(TEN),
                    constant(TWO)
                )
            )
        );
        BigInteger result = example.calculate().getValue();
        assertEquals(2252, result.intValue());
    }

    /**
    * Verifies the result for the following tree (numbers in parentheses indicate operations):
    * <pre>
    *                (1) +
    *                   / \
    *              (2) *   0
    *                 / \
    *            (3) +   2
    *               / \
    *          (4) *   2
    *             / \
    *        (5) *   10
    *           / \
    *      (6) +   \
    *         / \   \
    *        2   2   ^ (7)
    *               / \
    *              2   ^ (8)
    *                 / \
    *                2   * (9)
    *                   / \
    *                  2   10
    * </pre>
    **/
    @Test
    public void mediumExample()
    {
        Node example = add // 1
        (
            mul //2
            (
                add // 3
                (
                    mul // 4
                    (
                        mul // 5
                        (
                            add // 6
                            (
                                constant(TWO),
                                constant(TWO)
                            ),
                            pow // 7
                            (
                                constant(TWO),
                                pow // 8
                                (
                                    constant(TWO),
                                    mul // 9
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
            constant(ZERO)
        );
        BigInteger result = example.calculate().getValue();
        assertEquals(1284, result.intValue());
    }

    /**
    * Verifies the result for the following tree (numbers in parentheses indicate operations):
    * <pre>
    *                    (0) %
    *                       / \
    *                      /   \
    *                     /     \
    *                (1) +       +
    *                   / \     / \
    *              (2) *   \   1   ^
    *                 / \   \     / \
    *            (3) +   2   +   2   *
    *               / \     / \     / \
    *          (5) *   2   2   *   2   10
    *             / \         / \ 
    *        (7) *   10     10   * (8)
    *           / \             / \
    *      (9) +   \      (10) *   \
    *         / \   \         / \   \
    *        2   2   ^ (11)  2   2   ^ (12)
    *               / \             / \
    *              2   ^ (13)      2   ^ (14)
    *                 / \             / \
    *                2   * (15)      2   * (16)
    *                   / \             / \
    *                  2   + (17)      2   + (18)
    *                     / \             / \
    *                    2   10         10   2
    * </pre>
    **/
    @Test
    public void complexExample()
    {
        BigInteger FIVE = BigInteger.valueOf(5);
        Node example = mod
        (
          add // 1
          (
            mul //2
            (
                add // 3
                (
                    mul // 5
                    (
                        mul // 7
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
                                        add(constant(FIVE), constant(TEN)) // 17
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
                add // 6
                (
                    constant(TEN),
                    mul // 8
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
                                    add(constant(TEN), constant(FIVE)) // 18
                                )
                            )
                        )
                    )
                )
            )
          ),
          add
          (
           constant(ONE),
           pow // -1
           (
            constant(TWO),
            mul
            (
                constant(TWO),
                constant(TEN)
            )
           )
          )
        );
        Entry<Cache, BigInteger> result = example.calculate().apply(new Cache());
        assertEquals(184, result.getValue().intValue());
        assertEquals(5, result.getKey().hits);
    }
}
