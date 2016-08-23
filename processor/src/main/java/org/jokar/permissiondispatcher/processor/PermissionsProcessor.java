package org.jokar.permissiondispatcher.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

import org.jokar.permissiondispatcher.annotation.RuntimePermissions;
import org.jokar.permissiondispatcher.processor.Interface.TypeResolver;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by JokAr on 16/8/23.
 */
@AutoService(Processor.class)
public class PermissionsProcessor extends AbstractProcessor implements TypeResolver {

    private Types mTypes;
    private Elements mElements;
    private Filer mFiler;
    private Messager mMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mTypes = processingEnv.getTypeUtils();
        mElements = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(RuntimePermissions.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean isSubTypeOf(String subTypeClass, String superTypeClass) {
        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();
        TypeMirror subType = types.getDeclaredType(elements.getTypeElement(subTypeClass));
        TypeMirror superType = types.getDeclaredType(elements.getTypeElement(superTypeClass));
        return types.isSubtype(subType, superType);
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {

        return mTypes.isSameType(t1, t2);
    }

    @Override
    public TypeMirror typeMirrorOf(String className) {

        return mElements.getTypeElement(className).asType();
    }
}
