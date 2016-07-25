package stiletto.sample;

import javax.inject.Inject;

public class LocalBookLoader implements BookLoader {

    @Inject
    public LocalBookLoader() {

    }

    @Override
    public void doSomething() {
        System.out.println("Loading");
    }
}
