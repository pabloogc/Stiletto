package stiletto.sample;

import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DummyComponent_DummyModule_ProvideStringFactory implements Factory<String> {
  private final DummyComponent.DummyModule module;

  public DummyComponent_DummyModule_ProvideStringFactory(DummyComponent.DummyModule module) {
    assert module != null;
    this.module = module;
  }

  @Override
  public String get() {
    return Preconditions.checkNotNull(
        module.provideString(), "Cannot return null from a non-@Nullable @Provides method");
  }

  public static Factory<String> create(DummyComponent.DummyModule module) {
    return new DummyComponent_DummyModule_ProvideStringFactory(module);
  }
}
