package stiletto.sample;


import stiletto.ProvidedBy;
import stiletto.Module;
import dagger.Component;
import dagger.Provides;

@Component(modules = DummyComponent.DummyModule.class)
@Module
public interface DummyComponent {

    @ProvidedBy.Constructor
    String provideString();

    @dagger.Module
    class DummyModule {

        @Provides
        String provideString() {
            return "HEY";
        }
    }
}
