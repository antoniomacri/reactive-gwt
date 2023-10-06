package com.github.antoniomacri.reactivegwt.processor;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Classpath;
import com.karuslabs.elementary.junit.annotations.Options;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.tools.JavaFileObject;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(JavacExtension.class)
@Options("-Werror -implicit:none")
@Processors(ReactiveGwtProcessor.class)
@Classpath("com.github.antoniomacri.reactivegwt.processor.GreetingService")
public class ReactiveGwtProcessorTest {

    @Test
    public void assert_processor_generates_async_interface(Results results) {
        assertThat(results.errors).isEmpty();

        assertThat(results.generated)
                .filteredOn(file -> file.isNameCompatible("GreetingServiceAsync", JavaFileObject.Kind.SOURCE))
                .hasSize(1)
                .first().satisfies(file -> {
                    assertThat(file.toUri().getPath()).isEqualTo("/SOURCE_OUTPUT/com/github/antoniomacri/reactivegwt/processor/GreetingServiceAsync.java");
                    assertThat(file.openInputStream()).hasContent("""
                            package com.github.antoniomacri.reactivegwt.processor;

                            import com.google.gwt.http.client.Request;
                            import com.google.gwt.user.client.rpc.AsyncCallback;
                            import jakarta.annotation.Generated;
                            import java.lang.Integer;
                            import java.lang.Number;
                            import java.lang.Void;
                            import java.util.ArrayList;

                            @Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
                            public interface GreetingServiceAsync {
                              Request ping(AsyncCallback<Void> callback);

                              /**
                               *  Returns server time.
                               */
                              Request time(AsyncCallback<Greeting> callback);

                              /**
                               *  Responds with a greeting message using your name.
                               */
                              Request post(Greeting name, AsyncCallback<Greeting> callback);

                              /**
                               *  Divides {@code x} by {@code y}.
                               *
                               *  @param x dividend
                               *  @param y divisor
                               *  @return {@code x} divided by {@code b}
                               */
                              Request divide(int x, int y, AsyncCallback<Integer> callback);

                              <T extends Number> Request acc(T a, T b, AsyncCallback<ArrayList<T>> callback);
                            }
                            """);
                });
    }

    @Test
    public void assert_processor_generates_mutiny_adapter(Results results) {
        assertThat(results.errors).isEmpty();

        assertThat(results.generated)
                .filteredOn(file -> file.isNameCompatible("GreetingServiceMutiny", JavaFileObject.Kind.SOURCE))
                .hasSize(1)
                .first().satisfies(file -> {
                    assertThat(file.toUri().getPath()).isEqualTo("/SOURCE_OUTPUT/com/github/antoniomacri/reactivegwt/processor/GreetingServiceMutiny.java");
                    assertThat(file.openInputStream()).hasContent("""
                            package com.github.antoniomacri.reactivegwt.processor;

                            import com.google.gwt.user.client.rpc.AsyncCallback;
                            import io.smallrye.mutiny.Uni;
                            import io.smallrye.mutiny.subscription.UniEmitter;
                            import jakarta.annotation.Generated;
                            import java.lang.Integer;
                            import java.lang.Number;
                            import java.lang.Override;
                            import java.lang.Throwable;
                            import java.lang.Void;
                            import java.util.ArrayList;

                            @Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
                            public class GreetingServiceMutiny {
                              private final GreetingServiceAsync async$;

                              public GreetingServiceMutiny(GreetingServiceAsync async) {
                                this.async$ = async;
                              }

                              private <T> AsyncCallback<T> createCallback(UniEmitter<? super T> em) {
                                return new AsyncCallback<T>() {
                                  @Override
                                  public void onFailure(Throwable e) {
                                    em.fail(e);
                                  }

                                  @Override
                                  public void onSuccess(T result) {
                                    em.complete(result);
                                  }
                                };
                              }

                              public Uni<Void> ping() {
                                return Uni.createFrom().emitter(em -> async$.ping(createCallback(em)));
                              }

                              /**
                               *  Returns server time.
                               */
                              public Uni<Greeting> time() {
                                return Uni.createFrom().emitter(em -> async$.time(createCallback(em)));
                              }

                              /**
                               *  Responds with a greeting message using your name.
                               */
                              public Uni<Greeting> post(final Greeting name) {
                                return Uni.createFrom().emitter(em -> async$.post(name, createCallback(em)));
                              }

                              /**
                               *  Divides {@code x} by {@code y}.
                               *
                               *  @param x dividend
                               *  @param y divisor
                               *  @return {@code x} divided by {@code b}
                               */
                              public Uni<Integer> divide(final int x, final int y) {
                                return Uni.createFrom().emitter(em -> async$.divide(x, y, createCallback(em)));
                              }

                              public <T extends Number> Uni<ArrayList<T>> acc(final T a, final T b) {
                                return Uni.createFrom().emitter(em -> async$.acc(a, b, createCallback(em)));
                              }
                            }
                            """);
                });
    }
}
