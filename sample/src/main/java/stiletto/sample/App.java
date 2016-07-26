package stiletto.sample;

public class App {

    public static void main(String[] args) {

        DaggerMyApplicationComponent.builder()
                .myApplicationModule(MyApplicationModule.builder()
                        .context(new AndroidContext() {
                            @Override
                            public void doContextThings() {
                                System.out.println("I'm a context");
                            }
                        })
                        .build());
    }
}
