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

import org.junit.jupiter.api.Test;
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
        Result result = example.calculate();
        assertEquals(2252, result.value().intValue());
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
    *          (5) *   2
    *             / \
    *        (7) *   10
    *           / \
    *      (9) +   \
    *         / \   \
    *        2   2   ^ (11)
    *               / \
    *              2   ^ (13)
    *                 / \
    *                2   * (15)
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
        Result result = example.calculate();
        assertEquals("5392912...(+315641 digits)...6330884", result.toString());
    }

    /**
    * Verifies the result for the following tree (numbers in parentheses indicate operations):
    * <pre>
    *                (1) +
    *                   / \
    *              (2) *   \
    *                 / \   \
    *            (3) +   2   + (4)
    *               / \     / \
    *          (5) *   2   2   * (6)
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
        Node example = add // 1
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
                                        add(constant(TWO), constant(TEN)) // 17
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
                                    add(constant(TEN), constant(TWO)) // 18
                                )
                            )
                        )
                    )
                )
            )
        );
        Result result = example.calculate();
        assertEquals("1527611...(+5050434 digits)...4193040", result.toString());
    }
}
