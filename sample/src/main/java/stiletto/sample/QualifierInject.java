package stiletto.sample;


import javax.inject.Named;

import dagger.Component;
import stiletto.ProvidedBy;
import stiletto.Stiletto;

@Stiletto.Module
//@Component Can't have @Component here
interface QualifierInject {

    @ProvidedBy.Injection(AnonymousUserController.class)
    UserController anonymousUserController(@Named("This is now qualified") AnonymousUserController p);
}
