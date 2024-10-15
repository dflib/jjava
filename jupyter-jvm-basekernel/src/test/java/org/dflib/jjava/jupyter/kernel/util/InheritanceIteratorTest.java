package org.dflib.jjava.jupyter.kernel.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class InheritanceIteratorTest {

    @ParameterizedTest
    @MethodSource("data")
    public void test(Class<?> root, List<Class<?>> expected) {
        List<Class<?>> actual = collectIteration(root);

        Assertions.assertEquals(expected, actual);
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(
                        A.class,
                        List.of(A.class, Object.class)
                ),
                Arguments.of(
                        B.class,
                        List.of(B.class, A.class, Object.class)
                ),
                Arguments.of(
                        C.class,
                        List.of(C.class, B.class, A.class, Object.class)
                ),
                Arguments.of(
                        int.class,
                        List.of(int.class)
                ),
                Arguments.of(
                        D.class,
                        List.of(D.class, Object.class)
                ),
                Arguments.of(
                        E.class,
                        List.of(E.class, L.class, J.class, K.class, I.class, D.class, Object.class)
                ),
                Arguments.of(
                        F.class,
                        List.of(F.class, J.class, K.class, I.class, E.class, L.class, D.class, Object.class)
                ),
                Arguments.of(
                        G.class,
                        List.of(G.class, N.class, O.class, Object.class)
                ),
                Arguments.of(
                        H.class,
                        List.of(H.class, Q.class, P.class, M.class, N.class, Object.class)
                )
        );
    }

    private List<Class<?>> collectIteration(Class<?> root) {
        List<Class<?>> data = new LinkedList<>();
        InheritanceIterator it = new InheritanceIterator(root);
        while (it.hasNext()) data.add(it.next());
        return data;
    }

    interface I {
    }

    interface J extends I {
    }

    interface K extends J, I {
    }

    interface L extends J, K {
    }

    class A {
    }

    class B extends A {
    }

    class C extends B {
    }

    class D {
    }

    class E extends D implements L {
    }

    class F extends E implements J, K {
    }

    interface M {
    }

    interface N {
    }

    interface O extends N {
    }

    interface P extends N, M {
    }

    interface Q extends P, M {
    }

    class G implements N, O {
    }

    class H implements Q {
    }
}
