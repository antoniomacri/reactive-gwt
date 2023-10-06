package com.github.antoniomacri.reactivegwt.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import jakarta.annotation.Generated;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic.Kind;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class ReactiveGwtProcessor extends AbstractProcessor {

    private static final String ASYNC_FIELD = "async$";
    private static final String GWT_RPC = "com.google.gwt.user.client.rpc";
    private static final Set<String> SUPPORTED_ANNOTATIONS = Set.of(GWT_RPC + ".RemoteServiceRelativePath");

    private static final ClassName AsyncCallback = ClassName.get(GWT_RPC, "AsyncCallback");
    private static final ClassName Request = ClassName.get("com.google.gwt.http.client", "Request");
    private static final ClassName Uni = ClassName.get("io.smallrye.mutiny", "Uni");
    private static final ClassName UniEmitter = ClassName.get("io.smallrye.mutiny.subscription", "UniEmitter");


    @Override
    public Set<String> getSupportedOptions() {
        return singleton("debug");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement)
                .map(e -> (TypeElement) e)
                .forEach(rpcService -> {
                    try {
                        process(rpcService);
                    } catch (Exception e) {
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        } else {
                            throw new RuntimeException("Error processing RPC service " + rpcService, e);
                        }
                    }
                });
        return true;
    }

    private void process(TypeElement rpcService) throws Exception {
        ClassName rpcName = ClassName.get(rpcService);
        log("RPC service interface: " + rpcName);

        List<ExecutableElement> methods = rpcService.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()))
                .toList();

        AnnotationSpec generated = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName()).build();

        Filer filer = processingEnv.getFiler();

        ClassName asyncName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Async");
        JavaFile asyncInterface = createAsyncInterface(rpcService, rpcName, methods, asyncName, generated);
        asyncInterface.writeTo(filer);

        JavaFile reactiveAdapter = createReactiveAdapter(rpcService, rpcName, methods, asyncName, generated);
        reactiveAdapter.writeTo(filer);
    }

    private JavaFile createAsyncInterface(TypeElement rpcService, ClassName rpcName, List<ExecutableElement> methods, ClassName asyncName, AnnotationSpec generated) {
        TypeSpec.Builder asyncTypeBuilder = TypeSpec.interfaceBuilder(asyncName.simpleName())
                .addOriginatingElement(rpcService)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generated);

        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();

            MethodSpec.Builder asyncMethod = MethodSpec.methodBuilder(methodName)
                    .addModifiers(PUBLIC, ABSTRACT)
                    .returns(Request);
            getDoc(method).ifPresent(asyncMethod::addJavadoc);

            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                asyncMethod.addTypeVariable(TypeVariableName.get((TypeVariable) typeParameterElement.asType()));
            }

            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                asyncMethod.addParameter(ParameterSpec.builder(type, name).build());
            }

            TypeName returnTypeName = TypeName.get(method.getReturnType());
            final TypeName returnType = returnTypeName.box();
            asyncMethod.addParameter(ParameterSpec
                    .builder(ParameterizedTypeName.get(AsyncCallback, returnType), "callback")
                    .build());
            asyncTypeBuilder.addMethod(asyncMethod.build());
        }

        return JavaFile.builder(rpcName.packageName(), asyncTypeBuilder.build()).build();
    }

    private JavaFile createReactiveAdapter(TypeElement rpcService, ClassName rpcName, List<ExecutableElement> methods, ClassName asyncName, AnnotationSpec generated) {
        ClassName reactiveName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Mutiny");

        TypeVariableName targetType = TypeVariableName.get("T");
        TypeSpec callback = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(AsyncCallback, targetType))
                .addMethod(MethodSpec.methodBuilder("onFailure")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(Throwable.class, "e")
                        .addStatement("$N.fail($N)", "em", "e")
                        .build())
                .addMethod(MethodSpec.methodBuilder("onSuccess")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(targetType, "result")
                        .addStatement("$N.complete($N)", "em", "result")
                        .build())
                .build();

        TypeSpec.Builder reactiveTypeBuilder = TypeSpec.classBuilder(reactiveName.simpleName())
                .addOriginatingElement(rpcService)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generated)
                .addField(asyncName, ASYNC_FIELD, PRIVATE, FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(asyncName, "async")
                        .addStatement("this.$L = async", ASYNC_FIELD).build());

        reactiveTypeBuilder.addMethod(MethodSpec.methodBuilder("createCallback")
                .addTypeVariable(targetType)
                .addModifiers(PRIVATE)
                .addParameter(ParameterizedTypeName.get(UniEmitter, WildcardTypeName.supertypeOf(targetType)), "em")
                .addStatement("return $L", callback)
                .returns(ParameterizedTypeName.get(AsyncCallback, targetType)).build());

        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();

            MethodSpec.Builder reactiveMethod = MethodSpec.methodBuilder(methodName)
                    .addModifiers(PUBLIC);
            getDoc(method).ifPresent(reactiveMethod::addJavadoc);

            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                reactiveMethod.addTypeVariable(TypeVariableName.get((TypeVariable) typeParameterElement.asType()));
            }

            StringBuilder params = new StringBuilder(); // accumulate params name to use in the async call
            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                params.append(name).append(", ");
                reactiveMethod.addParameter(ParameterSpec.builder(type, name).addModifiers(FINAL).build());
            }

            TypeName returnTypeName = TypeName.get(method.getReturnType());
            final TypeName returnType = returnTypeName.box();
            reactiveMethod.addStatement("return $T.createFrom().emitter(em -> $L.$L($LcreateCallback(em)))",
                    Uni, ASYNC_FIELD, methodName, params.toString());
            reactiveMethod.returns(ParameterizedTypeName.get(Uni, returnType));
            reactiveTypeBuilder.addMethod(reactiveMethod.build());
        }

        return JavaFile.builder(rpcName.packageName(), reactiveTypeBuilder.build()).build();
    }

    private Optional<String> getDoc(ExecutableElement method) {
        String docComment = processingEnv.getElementUtils().getDocComment(method);
        if (docComment == null || docComment.trim().isEmpty()) {
            return Optional.empty();
        }
        if (!docComment.endsWith("\n")) {
            docComment += "\n";
        }
        return Optional.of(docComment);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
        }
    }
}
