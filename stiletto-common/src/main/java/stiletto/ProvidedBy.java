package stiletto;


import java.lang.annotation.*;


public interface ProvidedBy {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented @interface Implementation {
        Class<?> value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented @interface Constructor {
        Class<?> value() default Void.class;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented @interface External {
        Class<?> value() default Void.class;
    }
}
