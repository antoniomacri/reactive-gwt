# Reactive GWT

Call GWT RPC services from a plain JVM application — without GWT's client compiler, without JSNI, and without recompiling your existing GWT services.

The library implements the GWT RPC wire protocol on top of `java.net.http.HttpClient` and exposes service interfaces through dynamic proxies, so any class annotated with `@RemoteServiceRelativePath` on the server can be invoked from a standard Java (or Android) client. An optional annotation processor generates both the `Async` interface and a reactive adapter that returns SmallRye Mutiny `Uni`s.

This project started as a fork of [GWT-SyncProxy](https://github.com/jcricket/gwt-syncproxy) and has been modernized: JDK 17 baseline, the modern `HttpClient`, JUnit 5 + Arquillian test stack, Maven Central publishing, and reactive adapters.


## Modules

- **`reactive-gwt-proxy`** — the runtime library
- **`reactive-gwt-processor`** — an annotation processor that scans GWT interfaces (`@RemoteServiceRelativePath`) and generates the GWT `XxxAsync` companion interface and the `XxxMutiny` reactive adapter
- **`examples/`** — integration examples against GWT 2.0.3, 2.7.0 and 2.8.2, plus a Quarkus-based modern client that wraps a GWT RPC backend and re-exposes it as a JAX-RS REST endpoint.


## Installation

The artifacts are published to [Maven Central](https://central.sonatype.com/artifact/io.github.antoniomacri/reactive-gwt-proxy):

Add the following dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.antoniomacri</groupId>
    <artifactId>reactive-gwt-processor</artifactId>
    <version>1.7.6</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>io.github.antoniomacri</groupId>
    <artifactId>reactive-gwt-proxy</artifactId>
    <version>1.7.6</version>
</dependency>
```


## Usage

Suppose you have an existing GWT RPC service shared between client and server:

```java
@RemoteServiceRelativePath("orders")
public interface OrderService extends RemoteService {
    OrderItem echo(OrderItem item);
    int putList(List<OrderItem> list);
}
```

You have two choices on how to use the library: directly with the Async style or with Mutiny.


### Async style (no extra dependency)

From the `OrderService` interface, the annotation processor generates an `OrderServiceAsync` companion (the GWT-style callback-based interface):

```java
@Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
public interface OrderServiceAsync {
    Request echo(OrderItem item, AsyncCallback<OrderItem> callback);
    Request putList(List<OrderItem> list, AsyncCallback<Integer> callback);
}
```

You can now use `ReactiveGWT.create` to make the library generate the client proxy for the Async interface:

```java
public class MyService {
    String moduleBaseUrl = "http://localhost:8080/AppModule/";
    OrderServiceAsync service = ReactiveGWT.create(OrderService.class, moduleBaseUrl);

    public void addOrder() {
        OrderItem item = new OrderItem();
        service.echo(item, new AsyncCallback<OrderItem>() {
            @Override
            public void onSuccess(OrderItem result) { /* ... */ }

            @Override
            public void onFailure(Throwable caught) { /* ... */ }
        });
    }
}
```

You need to specify also the URL of the backend application which exposes services via GWT-RPC.

The `service` variable holds an implementation of the Async interface which implements all the logic for communicating to the backend server via GWT-RPC.


### Reactive style with Mutiny

When Mutiny is on the classpath, the annotation processor generates an `OrderServiceMutiny` adapter, alongside the async interface:

```java
@Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
public class OrderServiceMutiny {
    private final OrderServiceAsync async$;

    public OrderServiceMutiny(OrderServiceAsync async) {
        this.async$ = async;
    }

    private <T> AsyncCallback<T> createCallback(UniEmitter<? super T> em) {
        return new AsyncCallback<T>() {
            @Override public void onFailure(Throwable e) { em.fail(e); }
            @Override public void onSuccess(T result) { em.complete(result); }
        };
    }

    public Uni<OrderItem> echo(final OrderItem item) {
        return Uni.createFrom().emitter(em -> async$.echo(item, createCallback(em)));
    }

    public Uni<Integer> putList(final List<OrderItem> list) {
        return Uni.createFrom().emitter(em -> async$.putList(list, createCallback(em)));
    }
}
```

First, use `ReactiveGWT.create` to make the library generate the client proxy for the Async interface, and then instantiate the generated Mutiny service:

```java
OrderServiceAsync async = ReactiveGWT.create(OrderService.class, moduleBaseUrl);
OrderServiceMutiny service = new OrderServiceMutiny(async);

Uni<OrderItem> result = service.echo(item);
```

This composes naturally with reactive frameworks such as Quarkus. A typical setup uses a CDI producer to build the proxy and the Mutiny adapter once, and a JAX-RS resource to re-expose them as REST endpoints:

```java
@ApplicationScoped
public class OrderServiceProducer {

    @Produces
    public OrderServiceAsync produce() {
        String moduleBaseURL = "http://localhost:8090/AppModule/";
        ProxySettings proxySettings = new ProxySettings(moduleBaseURL, OrderService.class.getName());
        return ReactiveGWT.create(OrderService.class, proxySettings);
    }

    @Produces
    public OrderServiceMutiny produce(OrderServiceAsync serviceAsync) {
        return new OrderServiceMutiny(serviceAsync);
    }
}
```

```java
@ApplicationScoped
@Path("orders")
public class OrderServiceRest {
    @Inject
    OrderServiceMutiny orderServiceMutiny;

    @POST
    @Path("echo")
    public Uni<OrderItem> echo(OrderItem orderItem) {
        return orderServiceMutiny.echo(orderItem);
    }

    @POST
    @Path("putList")
    public Uni<Integer> putList(List<OrderItem> list) {
        return orderServiceMutiny.putList(list);
    }
}
```


### Configuring the proxy

Use `ProxySettings` for finer control (custom headers, cookies, executor, OAuth tokens, RPC stream version, etc.):

```java
ProxySettings settings = new ProxySettings(moduleBaseUrl, OrderService.class.getName())
        .setSerializationStreamVersion(5)            // match older servers (e.g. GWT 2.0/2.7)
        .setExecutor(myExecutor);

OrderServiceAsync service = ReactiveGWT.create(OrderService.class, settings);
```


## License

Apache License 2.0 — see [`License.md`](License.md).


# Releasing

Check the token is currently valid or issue a new token. Login to https://central.sonatype.com then go to https://central.sonatype.com/usertoken and generate a new token.

Paste the token in the `settings.xml` as
```xml
<server>
    <id>central</id>
    <username>...</username>
    <password>...</password>
</server>
```

Make sure a variable pointing to a JDK 1.8 is defined in `settings.xml`, eg:
```xml

<profile>
    <id>common</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
        <jdk8>/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home</jdk8>
    </properties>
</profile>
```

Finally, run the release plugin:
```shell
./mvnw -Darguments='-Dmaven.test.skip=true' release:prepare
./mvnw release:perform
```
