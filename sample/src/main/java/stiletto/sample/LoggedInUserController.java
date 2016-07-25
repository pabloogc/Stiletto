package stiletto.sample;

public class LoggedInUserController implements UserController {

    public LoggedInUserController(AndroidContext androidContext){

    }

    @Override
    public boolean canPerformUserAction() {
        return true;
    }
}
