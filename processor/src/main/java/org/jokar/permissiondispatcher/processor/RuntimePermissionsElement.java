package org.jokar.permissiondispatcher.processor;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.annotation.OnNeverAskAgain;
import org.jokar.permissiondispatcher.annotation.OnPermissionDenied;
import org.jokar.permissiondispatcher.annotation.OnShowRationale;
import org.jokar.permissiondispatcher.processor.event.ClassType;
import org.jokar.permissiondispatcher.processor.event.ConstantsProvider;
import org.jokar.permissiondispatcher.processor.event.TypeResolver;
import org.jokar.permissiondispatcher.processor.utils.ProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkActivity;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.findMethods;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkNotEmpty;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkPrivateMethods;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkMethodSignature;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkMixPermissionType;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkDuplicatedValue;
import static org.jokar.permissiondispatcher.processor.utils.ValidatorUtils.checkMethodParameters;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.findOnRationaleMatchingMethod;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.findOnDeniedMatchingMethod;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.findOnNeverAskMatchingMethod;

/**
 * Created by JokAr on 16/8/23.
 */
public class RuntimePermissionsElement {
    private String packageName;

    private String className;
    private String generatedClassName;

    private ClassType classType;

    private TypeResolver mTypeResolver;
    private TypeName mTypeName;
    private List<TypeVariableName> typeVariables;

    private List<ExecutableElement> needsPermissionsMethods;

    private List<ExecutableElement> showsRationaleMethods;

    private List<ExecutableElement> deniedPermissionMethods;

    private List<ExecutableElement> neverAskMethods;

    public RuntimePermissionsElement(TypeElement element, TypeResolver resolver) {
        mTypeResolver = resolver;
        mTypeName = TypeName.get(element.asType());
        typeVariables = new ArrayList<>();
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        for (TypeParameterElement element1 : typeParameters) {
            typeVariables.add(TypeVariableName.get(element1));
        }
        String claseName = element.getQualifiedName().toString();
        packageName = ProcessorUtil.getPackageName(claseName);
        className = ProcessorUtil.getClassName(claseName);
        classType = checkActivity(element, resolver);
        generatedClassName = element.getSimpleName().toString() + ConstantsProvider.GEN_CLASS_SUFFIX;
        needsPermissionsMethods = findMethods(element, NeedsPermission.class);
        validateNeedsMethods();

        showsRationaleMethods = findMethods(element, OnShowRationale.class);
        validateRationaleMethods();

        deniedPermissionMethods = findMethods(element, OnPermissionDenied.class);
        validateDeniedMethods();

        neverAskMethods = findMethods(element, OnNeverAskAgain.class);
        validateNeverAskMethods();
    }


    private void validateNeedsMethods() {
        checkNotEmpty(needsPermissionsMethods, this, NeedsPermission.class);
        checkPrivateMethods(needsPermissionsMethods, NeedsPermission.class);
        checkMethodSignature(needsPermissionsMethods);
        checkMixPermissionType(needsPermissionsMethods, NeedsPermission.class);
    }

    private void validateRationaleMethods() {
        checkDuplicatedValue(showsRationaleMethods, OnShowRationale.class);
        checkPrivateMethods(showsRationaleMethods, OnShowRationale.class);
        checkMethodSignature(showsRationaleMethods);
        checkMethodParameters(showsRationaleMethods, 1,
                "org.jokar.permissiondispatcher.library.PermissionRequest", mTypeResolver);
    }

    private void validateDeniedMethods() {
        checkDuplicatedValue(deniedPermissionMethods, OnPermissionDenied.class);
        checkPrivateMethods(deniedPermissionMethods, OnPermissionDenied.class);
        checkMethodSignature(deniedPermissionMethods);
        checkMethodParameters(deniedPermissionMethods, 0, "", mTypeResolver);
    }

    public void validateNeverAskMethods() {
        checkDuplicatedValue(neverAskMethods, OnNeverAskAgain.class);
        checkPrivateMethods(neverAskMethods, OnNeverAskAgain.class);
        checkMethodSignature(neverAskMethods);
        checkMethodParameters(neverAskMethods, 0, "", mTypeResolver);
    }

    public ExecutableElement findOnRationaleForNeeds(String[] value) {
        return findOnRationaleMatchingMethod(value, showsRationaleMethods);
    }

    public ExecutableElement findOnDeniedForNeeds(String[] value) {
        return findOnDeniedMatchingMethod(value, deniedPermissionMethods);
    }

    public ExecutableElement findOnNeverAskForNeeds(String[] value) {
        return findOnNeverAskMatchingMethod(value, neverAskMethods);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public ClassType getClassType() {
        return classType;
    }

    public String getGeneratedClassName() {
        return generatedClassName;
    }

    public List<ExecutableElement> getNeedsPermissionsMethods() {
        return needsPermissionsMethods;
    }

    public TypeResolver getTypeResolver() {
        return mTypeResolver;
    }

    public List<ExecutableElement> getShowsRationaleMethods() {
        return showsRationaleMethods;
    }

    public List<ExecutableElement> getDeniedPermissionMethods() {
        return deniedPermissionMethods;
    }

    public List<ExecutableElement> getNeverAskMethods() {
        return neverAskMethods;
    }

    public TypeName getTypeName() {
        return mTypeName;
    }

    public List<TypeVariableName> getTypeVariables() {
        return typeVariables;
    }
}
