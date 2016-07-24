package stiletto.sample;

import dagger.internal.Preconditions;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DaggerDummyComponent implements DummyComponent {
  private Provider<String> provideStringProvider;

  private DaggerDummyComponent(Builder builder) {
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static DummyComponent create() {
    return builder().build();
  }

  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {

    this.provideStringProvider =
        DummyComponent_DummyModule_ProvideStringFactory.create(builder.dummyModule);
  }

  @Override
  public String provideString() {
    return provideStringProvider.get();
  }

  public static final class Builder {
    private DummyComponent.DummyModule dummyModule;

    private Builder() {}

    public DummyComponent build() {
      if (dummyModule == null) {
        this.dummyModule = new DummyComponent.DummyModule();
      }
      return new DaggerDummyComponent(this);
    }

    public Builder dummyModule(DummyComponent.DummyModule dummyModule) {
      this.dummyModule = Preconditions.checkNotNull(dummyModule);
      return this;
    }
  }
}
