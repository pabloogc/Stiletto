package stiletto.sample;


import org.jetbrains.annotations.Nullable;

import javax.inject.Named;

import dagger.Component;
import stiletto.ProvidedBy;
import stiletto.Stiletto;

@Stiletto.Module
@Component(modules = DummyModule.class)
interface DummyComponent {

    @ProvidedBy.Runtime
    String hello();

    @Named("local")
    @ProvidedBy.Injection(LocalBookLoader.class)
    BookLoader localBookLoader();

    @Named("remote")
    @ProvidedBy.NewInstance(RemoteBookLoader.class)
    BookLoader remoteBookLoader();
}
