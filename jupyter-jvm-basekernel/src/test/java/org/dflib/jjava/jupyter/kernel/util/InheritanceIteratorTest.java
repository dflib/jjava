package org.dflib.jjava.jupyter.kernel.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class InheritanceIteratorTest {
    interface I {}

    interface J extends I {}

    interface K extends J, I {}

    interface L extends J, K {}

    class A {}

    class B extends A {}

    class C extends B {}

    class D {}

    class E extends D implements L {}

    class F extends E implements J, K {}

    interface M {}

    interface N {}

    interface O extends N {}

    interface P extends N, M {}

    interface Q extends P, M {}

    class G implements N, O {}

    class H implements Q {}

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { A.class, Arrays.asList(A.class, Object.class) },
                { B.class, Arrays.asList(B.class, A.class, Object.class) },
                { C.class, Arrays.asList(C.class, B.class, A.class, Object.class) },
                { int.class, Collections.singletonList(int.class) },
                { D.class, Arrays.asList(D.class, Object.class) },
                { E.class, Arrays.asList(E.class, L.class, J.class, K.class, I.class, D.class, Object.class) },
                { F.class, Arrays.asList(F.class, J.class, K.class, I.class, E.class, L.class, D.class, Object.class) },
                { G.class, Arrays.asList(G.class, N.class, O.class, Object.class) },
                { H.class, Arrays.asList(H.class, Q.class, P.class, M.class, N.class, Object.class) },
        });
    }

    private final Class root;
    private final List<Class> expectedOrder;

    public InheritanceIteratorTest(Class root, List<Class> expectedOrder) {
        this.root = root;
        this.expectedOrder = expectedOrder;
    }

    private List<Class> collectIteration(Class root) {
        List<Class> data = new LinkedList<>();
        InheritanceIterator it = new InheritanceIterator(root);
        while (it.hasNext()) data.add(it.next());
        return data;
    }

    @Test
    public void test() {
        List<Class> actual = collectIteration(this.root);

        assertEquals(this.expectedOrder, actual);
    }
}