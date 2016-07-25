package stiletto.sample;

import javax.inject.Inject;

public class AnonymousUserController implements UserController {

    @Inject
    AnonymousUserController() {

    }

    @Override
    public boolean canPerformUserAction() {
        return false;
    }
}
