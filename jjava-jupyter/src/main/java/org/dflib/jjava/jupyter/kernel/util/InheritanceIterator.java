package org.dflib.jjava.jupyter.kernel.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterate over the types that an object is an {@code instanceof}. {@link Class}es in
 * the iteration will not be duplicated (once a class is seen it will not be seen again
 * even if, for example, an interface is declared to be implemented by 2 classes).
 * <p>
 * Example:
 * <pre>
 * {@code interface I {}
 *   interface J extends I {}
 *   interface K extends J, I {} // Redundant but allowed
 *   interface L extends J, K {}
 *
 *   class D {}
 *   class E extends D implements L {}
 *   class F extends E implements J, K {}
 * }
 * </pre>
 * Iterating over {@code new InheritanceIterator(F.class)} will yield:
 * {@code F.class, J.class, K.class, I.class, E.class, L.class, D.class, Object.class}
 */
public class InheritanceIterator implements Iterator<Class> {
    private final Set<Class> observedInterfaces;

    private Class concrete;
    private Iterator<Class> implementedInterfaces;

    public InheritanceIterator(Class root) {
        this.concrete = root;
        this.observedInterfaces = new LinkedHashSet<>();
    }

    /**
     * Construct an iterator that walks the implemented interfaces by the current {@link #concrete}
     * class. The should skip all {@link #observedInterfaces}.
     *
     * @return and iterator over the implemented interfaces.
     */
    private Iterator<Class> initializeImplementedInterfaces() {
        List<Class> implemented = new ArrayList<>();
        getAllInterfaces(implemented, this.concrete.getInterfaces());
        return implemented.iterator();
    }

    private void getAllInterfaces(List<Class> allInterfaces, Class[] declaredImplementations) {
        for (Class implementedInterface : declaredImplementations) {
            if (this.observedInterfaces.add(implementedInterface))
                allInterfaces.add(implementedInterface);
        }

        for (Class implementedInterface : declaredImplementations)
            getAllInterfaces(allInterfaces, implementedInterface.getInterfaces());
    }

    @Override
    public boolean hasNext() {
        return this.implementedInterfaces == null
                || this.implementedInterfaces.hasNext()
                || this.concrete.getSuperclass() != null;
    }

    @Override
    public Class next() {
        if (this.implementedInterfaces == null) {
            this.implementedInterfaces = this.initializeImplementedInterfaces();
            return this.concrete;
        }

        if (this.implementedInterfaces.hasNext())
            return this.implementedInterfaces.next();

        Class superClass = this.concrete.getSuperclass();
        if (superClass != null) {
            this.concrete = superClass;
            this.implementedInterfaces = this.initializeImplementedInterfaces();
            return superClass;
        }

        throw new NoSuchElementException();
    }
}
