package org.jokar.permissiondispatcher.processor.utils;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.processor.RuntimePermissionsElement;
import org.jokar.permissiondispatcher.processor.event.ClassType;
import org.jokar.permissiondispatcher.processor.event.ConstantsProvider;
import org.jokar.permissiondispatcher.processor.event.TypeResolver;
import org.jokar.permissiondispatcher.processor.exception.DuplicatedValueException;
import org.jokar.permissiondispatcher.processor.exception.MixPermissionTypeException;
import org.jokar.permissiondispatcher.processor.exception.NoAnnotatedMethodsException;
import org.jokar.permissiondispatcher.processor.exception.NoParametersAllowedException;
import org.jokar.permissiondispatcher.processor.exception.NoThrowsAllowedException;
import org.jokar.permissiondispatcher.processor.exception.PrivateMethodException;
import org.jokar.permissiondispatcher.processor.exception.WrongClassException;
import org.jokar.permissiondispatcher.processor.exception.WrongParametersException;
import org.jokar.permissiondispatcher.processor.exception.WrongReturnTypeException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import static org.jokar.permissiondispatcher.processor.event.ClassType.getClassType;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.getValueFromAnnotation;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.isEmpty;
import static java.util.Arrays.deepEquals;

/**
 * Created by JokAr on 16/8/23.
 */
public final class ValidatorUtils {
    private final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    private final String SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";

    public static ClassType checkActivity(TypeElement element, TypeResolver resolver) {

        ClassType classType = getClassType(element.getQualifiedName().toString(), resolver);

        if (classType == null) {
            throw new WrongClassException(element);
        }

        return classType;
    }

    public static void checkNotEmpty(List<ExecutableElement> elements, RuntimePermissionsElement element,
                                     Class clazz) {
        if (isEmpty(elements)) {
            throw new NoAnnotatedMethodsException(element, clazz);
        }
    }

    public static void checkPrivateMethods(List<ExecutableElement> elements, Class clazz) {
        for (ExecutableElement element : elements) {
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                throw new PrivateMethodException(element, clazz);
            }
        }
    }

    /**
     * Checks the return type of the elements in the provided list.
     * <p>
     * Raises an exception if any element specifies a return type other than 'void'.
     */
    public static void checkMethodSignature(List<ExecutableElement> elements) {
        for (ExecutableElement element : elements) {
            // Allow 'void' return type only
            if (element.getReturnType().getKind() != TypeKind.VOID) {
                throw new WrongReturnTypeException(element);
            }
            // Allow methods without 'throws' declaration only
            if (!element.getThrownTypes().isEmpty()) {
                throw new NoThrowsAllowedException(element);
            }
        }
    }

    /**
     * Checks the elements in the provided list annotated with an annotation against duplicate values.
     * <p>
     * Raises an exception if any annotation value is found multiple times.
     */
    public static void checkDuplicatedValue(List<ExecutableElement> elements, Class clazz) {
        Set<String> values = new HashSet<>();
        for (ExecutableElement method : elements) {
            List<String> value = getValueFromAnnotation(method, clazz);
            if (!values.addAll(value)) {
                throw new DuplicatedValueException(value, method, clazz);
            }
        }
    }


    public static void checkMethodParameters(List<ExecutableElement> elements, int methodCount,
                                             String clazz, TypeResolver classType) {
        for (ExecutableElement element : elements) {
            List<? extends VariableElement> parameters = element.getParameters();
            if (methodCount == 0 && !parameters.isEmpty()) {
                throw new NoParametersAllowedException(element);
            }

            if (parameters.size() != methodCount) {
                throw new WrongParametersException(element, clazz);
            }

            for (VariableElement variableElement : parameters) {

                if (!classType.isSameType(variableElement.asType(), classType.typeMirrorOf(clazz))) {
                    throw new WrongParametersException(element, clazz);
                }
            }
        }
    }


    public static void checkMixPermissionType(List<ExecutableElement> elements, Class clazz) {
        for (ExecutableElement element : elements) {

            List valueFromAnnotation = getValueFromAnnotation(element, clazz);
            if (valueFromAnnotation.size() > 1) {
                if (valueFromAnnotation.contains(WRITE_SETTINGS)) {
                    throw new MixPermissionTypeException(element, WRITE_SETTINGS);
                } else if (valueFromAnnotation.contains(SYSTEM_ALERT_WINDOW)) {
                    throw new MixPermissionTypeException(element, SYSTEM_ALERT_WINDOW);
                }
            }

        }
    }


}
