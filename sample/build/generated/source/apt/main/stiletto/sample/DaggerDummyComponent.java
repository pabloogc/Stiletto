package stiletto.sample;

import dagger.internal.Preconditions;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DaggerDummyComponent implements DummyComponent {
  private Provider<String> provideHelloProvider;

  private Provider<BookLoader> provideLocalBookLoaderProvider;

  private Provider<BookLoader> provideRemoteBookLoaderProvider;

  private DaggerDummyComponent(Builder builder) {
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {

    this.provideHelloProvider = DummyModule_ProvideHelloFactory.create(builder.dummyModule);

    this.provideLocalBookLoaderProvider =
        DummyModule_ProvideLocalBookLoaderFactory.create(
            builder.dummyModule, LocalBookLoader_Factory.create());

    this.provideRemoteBookLoaderProvider =
        DummyModule_ProvideRemoteBookLoaderFactory.create(
            builder.dummyModule, provideLocalBookLoaderProvider);
  }

  @Override
  public String hello() {
    return provideHelloProvider.get();
  }

  @Override
  public BookLoader localBookLoader() {
    return provideLocalBookLoaderProvider.get();
  }

  @Override
  public BookLoader remoteBookLoader() {
    return provideRemoteBookLoaderProvider.get();
  }

  public static final class Builder {
    private DummyModule dummyModule;

    private Builder() {}

    public DummyComponent build() {
      if (dummyModule == null) {
        throw new IllegalStateException(DummyModule.class.getCanonicalName() + " must be set");
      }
      return new DaggerDummyComponent(this);
    }

    public Builder dummyModule(DummyModule dummyModule) {
      this.dummyModule = Preconditions.checkNotNull(dummyModule);
      return this;
    }
  }
}
