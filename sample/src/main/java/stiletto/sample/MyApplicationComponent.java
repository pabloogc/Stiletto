package stiletto.sample;


import javax.inject.Named;

import dagger.Component;
import stiletto.ProvidedBy;
import stiletto.Stiletto;

@Stiletto.Module
@Component(modules = MyApplicationModule.class)
interface MyApplicationComponent {

    @ProvidedBy.Runtime
    AndroidContext context();

    @Named("anonymous")
    @ProvidedBy.Injection(AnonymousUserController.class)
    UserController anonymousUserController();

    @Named("user")
    @ProvidedBy.NewInstance(LoggedInUserController.class)
    UserController loggedInUserControler();

    @ProvidedBy.Runtime
    int someInt();
}
