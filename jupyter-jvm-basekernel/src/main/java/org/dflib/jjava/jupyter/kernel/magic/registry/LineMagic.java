package org.dflib.jjava.jupyter.kernel.magic.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LineMagic {
    String value() default "";

    String[] aliases() default {};
}
