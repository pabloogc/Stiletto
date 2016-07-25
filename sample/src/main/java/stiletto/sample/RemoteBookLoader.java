package stiletto.sample;

import javax.inject.Named;

public class RemoteBookLoader implements BookLoader {

    public RemoteBookLoader(@Named("local") BookLoader localBookLoader) {
    }

    @Override
    public void doSomething() {
        System.out.println("Loading");
    }
}
