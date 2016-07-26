# Stiletto

Stilleto is a Dagger extension to generate boilerplate Dagger 2 modules. Styletto is *not complete a replacement for writing Dagger modules*, the tool is designed to avoid boilerplate of some specific cases. Non straightforward provision rules are better written manually.

The basic idea is to automatically generate a module following rules specified by annotations. Only basic checks are performed, Dagger compiler will do the hard work. The generated code is intended to be readable since dagger will report errors based on that code.

Stiletto will transform any class annotated with ```@Stiletto.Module``` into a Module class where every ```@ProvidedBy.X``` is satisfied. If a method annotated with ```@ProvidedBy``` fails to compile the whole process will be aborted.

From this sample component:

```java
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
}
```

Stiletto will generate ```MyApplicationModule```. The name of the generated class will be ```NameOfClass+Module``` by default. If the class is also annotated with ```@Component``` it will remove the ```Component``` suffix and replace it with ```Module```.

Note that the code is self referential  ```@Component(modules = MyApplicationModule.class)``` (one processor depending on another), Dagger will handle this internally using [Auto Common Utilities](https://github.com/google/auto/tree/master/common) so everything is kept as simple as possible.

The generated module looks like the code we would usually write

```java
@Module
public class MyApplicationModule {
  private final AndroidContext context;

  public MyApplicationModule(AndroidContext context) {
    this.context = context;
  }

  @Provides
  @Named("user")
  public UserController provideLoggedInUserControler(AndroidContext androidContext) {
    return new LoggedInUserController(androidContext);
  }

  @Provides
  @Named("anonymous")
  public UserController provideAnonymousUserController(AnonymousUserController anonymousUserController) {
    return anonymousUserController;
  }

  @Provides
  public AndroidContext provideContext() {
    return this.context;
  }

  static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private AndroidContext context;

    public Builder context(AndroidContext context) {
      this.context = context;
      return this;
    }

    public MyApplicationModule build() {
      if(context == null) throw new NullPointerException("context == null");
      return new MyApplicationModule(context);
    }
  }
}
```

This tool only covers 3 use cases: User provided dependencies during runtime (Android), wrapping implementation by interfaces and creating basic objects without @Inject

<br><br>
## Runtime provisions
It's very common when working with dagger to have framework types that we want to provide but can't create instances of. Android types are a great example of this.

```java
public interface AndroidContext {
    void doContextThings();
}
```

Stiletto will generate a ```Builder``` class for your module and every ```@ProvidedBy.Runtime``` that will have its corresponding builder method. Null checks are performed unless you annotate the provision with ```@NotNull```. 

#### Declaration 
```java
  @ProvidedBy.Runtime 
  AndroidContext context();
```

#### Usage
```java
  DaggerMyApplicationComponent.builder()
          .myApplicationModule(MyApplicationModule.builder()
                  .context(activity)
                  .build());
```
Annotation and qualifiers are carried over to the module (```@Named("ApplicationContext")``` will work).

<br><br>
## Inject provision
When our types are injectable but we want to expose them by a their interface we are forced to create a module. We can use ```@ProvidedBy.Injection``` to avoid it.

#### Class we want to provide
```java
public interface UserController {
    boolean canPerformUserAction();
}
```
One implementation
```java
public class AnonymousUserController implements UserController {
    @Inject //<--- Injectable!
    AnonymousUserController() {
    }
    //. . .
}
```
<br>

Stiletto will generate the provision method given a valid asignable subtype.

#### Declaration 
```java
  @Named("anonymous")
  @ProvidedBy.Injection(AnonymousUserController.class) //The specific, injectable type
  UserController anonymousUserController();
```

#### Generated module
```java
  @Provides
  @Named("anonymous")
  public UserController provideAnonymousUserController(AnonymousUserController anonymousUserController) {
    return anonymousUserController;
  }
```
<br>

Annotation and qualifiers are carried over to the module **for the return type only**. 
If we want to qualify the injected parameter we must separate the ```@Component``` and ```@Stiletto.Module``` declarations and 
add a parameter (type is irrelevant) with the qualifier annotations. If don't do that dagger will try (and fail) to generate a member injector since it's a method with a parameter within a ```@Component```. This restriction might me improved in the future.

#### Declaration 
```java
@Stiletto.Module
//@Component Can't have @Component here
interface QualifierInject {

    @ProvidedBy.Injection(AnonymousUserController.class)
    UserController anonymousUserController(@Named("This is now qualified") Void p); //We don't care about parameter type
}
```

#### Generated module
```java
@Module
public class QualifierInjectModule {
  @Provides
  public UserController provideAnonymousUserController(@Named("This is now qualified") AnonymousUserController anonymousUserController) {
    return anonymousUserController;
  }
}
```

<br><br>
#Instance creation

This usage covers a similar patter as inected provisions. If we don't want to modify our code using ```@Inject``` we can use ```@ProvidedBy.NewInstance``` instead.

#### Class we want to provide
```java
public class LoggedInUserController implements UserController {
    //No inject here, we don't want to modify this class or make it injectable
    public LoggedInUserController(AndroidContext androidContext){
    }
}
```

#### Declaration 
```java
  @Named("user")
  @ProvidedBy.NewInstance(LoggedInUserController.class)
  UserController loggedInUserControler();
```

This is also valid
```java
  @ProvidedBy.NewInstance
  LoggedInUserController loggedInUserControler();
```

#### Generated module
```java
  @Provides
  @Named("user")
  public UserController provideLoggedInUserControler(AndroidContext androidContext) {
    return new LoggedInUserController(androidContext);
  }
```

This annotation can only be applied if the class has exactly 1 public constructor. The types (```AndroidContext```) can be qualified and will be carried over to the module declaration. If you have multiple constructors you can use the other two alternatives or simply generate the module class manually.

<br><br>
# How to use in your project

* Find any APT plugin (Android or Java) and include it in your project.
* Add [jitpack.io](https://jitpack.io) repository. Instructions are detailed in the website.
* Add the Stiletto dependencies. You can use ```-SNAPSHOT``` or check for a release tag like ```1.0.1-alpha```.
```java
    compile 'com.github.pabloogc.stiletto:stiletto:1.0.1-alpha'
    apt 'com.github.pabloogc.stiletto:stiletto-compiler:1.0.1-alpha'
```
* Add Dagger 2 dependency. Stiletto uses dagger 2.5 annotations, however, they are not included by default (the dependency is only present in the compiler) so can provide your own dagger version.
```java
    compile 'com.google.dagger:dagger:2.5'
    apt 'com.google.dagger:dagger-compiler:2.5'
````
* Reflect about that module you don't have to write.

