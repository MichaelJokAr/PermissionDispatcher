package org.jokar.permissiondispatcher.processor.utils;

import com.squareup.javapoet.CodeBlock;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.annotation.OnNeverAskAgain;
import org.jokar.permissiondispatcher.annotation.OnPermissionDenied;
import org.jokar.permissiondispatcher.annotation.OnShowRationale;
import org.jokar.permissiondispatcher.annotation.RuntimePermissions;
import org.jokar.permissiondispatcher.processor.RuntimePermissionsElement;
import org.jokar.permissiondispatcher.processor.event.ConstantsProvider;
import org.jokar.permissiondispatcher.processor.event.TypeResolver;
import org.jokar.permissiondispatcher.processor.helper.SensitivePermissionInterface;
import org.jokar.permissiondispatcher.processor.helper.SystemAlertWindowHelper;
import org.jokar.permissiondispatcher.processor.helper.WriteSettingsHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static java.util.Arrays.asList;
import static java.util.Arrays.deepEquals;
import static java.util.Collections.emptyList;

/**
 * Created by JokAr on 16/8/23.
 */
public final class ProcessorUtil {

    public static List<RuntimePermissionsElement> getAnnotatedClasses(RoundEnvironment roundEnv, TypeResolver typeResolver) {
        List<RuntimePermissionsElement> runtimePermissionsElementList = new ArrayList<>();

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(RuntimePermissions.class);
        for (Element element : elements) {
            runtimePermissionsElementList.add(new RuntimePermissionsElement((TypeElement) element, typeResolver));
        }
        return runtimePermissionsElementList;
    }

    public static String getPackageName(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }

    public static String getClassName(String name) {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    public static List<ExecutableElement> findMethods(Element element, Class<? extends Annotation> clazz) {
        List<ExecutableElement> methods = new ArrayList<>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            Annotation annotation = enclosedElement.getAnnotation(clazz);
            if (annotation != null) {
                methods.add((ExecutableElement) enclosedElement);
            }
        }
        return methods;
    }

    public static <A extends Annotation> List<String> getValueFromAnnotation(ExecutableElement element, Class<A> clazz) {

        if (Objects.equals(clazz, NeedsPermission.class)) {
            return asList(element.getAnnotation(NeedsPermission.class).value());
        } else if (Objects.equals(clazz, OnShowRationale.class)) {
            return asList(element.getAnnotation(OnShowRationale.class).value());
        } else if (Objects.equals(clazz, OnPermissionDenied.class)) {
            return asList(element.getAnnotation(OnPermissionDenied.class).value());
        } else if (Objects.equals(clazz, OnNeverAskAgain.class)) {
            return asList(element.getAnnotation(OnNeverAskAgain.class).value());
        } else {
            return emptyList();
        }
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static ExecutableElement findOnRationaleMatchingMethod(String[] annotation1,
                                                                  List<ExecutableElement> otherElements) {

        for (ExecutableElement element : otherElements) {

            String[] value = element.getAnnotation(OnShowRationale.class).value();

            if (deepEquals(annotation1, value)) {
                return element;
            }
        }

        return null;
    }


    public static ExecutableElement findOnDeniedMatchingMethod(String[] value,
                                                               List<ExecutableElement> elements) {
        for (ExecutableElement element : elements) {
            String[] value2 = element.getAnnotation(OnPermissionDenied.class).value();
            if (deepEquals(value, value2)) {
                return element;
            }
        }

        return null;
    }

    public static ExecutableElement findOnNeverAskMatchingMethod(String[] value1,
                                                                 List<ExecutableElement> elements) {
        for (ExecutableElement element : elements) {
            String[] value = element.getAnnotation(OnNeverAskAgain.class).value();
            if (deepEquals(value1, value)) {
                return element;
            }
        }

        return null;
    }

    public static String toString(String... array) {
        if (array == null) {
            return null;
        }
        int max = array.length - 1;
        StringBuilder b = new StringBuilder();
        b.append('{');
        for (int i = 0; ; i++) {
            b.append("\"").append(array[i]).append("\"");
            if (i == max)
                return b.append('}').toString();
            b.append(", ");
        }
    }

    public static String requestCodeFieldName(ExecutableElement element) {

        return ConstantsProvider.REQUEST_CODE_PREFIX + element.getSimpleName().toString().toUpperCase();
    }

    public static String permissionFieldName(ExecutableElement element) {

        return ConstantsProvider.PERMISSION_PREFIX + element.getSimpleName().toString().toUpperCase();

    }

    public static String pendingRequestFieldName(ExecutableElement element) {
        return ConstantsProvider.GEN_PENDING_PREFIX + element.getSimpleName().toString().toUpperCase();
    }


    public static String permissionRequestTypeName(ExecutableElement element) {
        return toUpperCaseFirstOne(element.getSimpleName().toString()) + ConstantsProvider.GEN_PERMISSIONREQUEST_SUFFIX;
    }

    //首字母转大写
    private static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    public static CodeBlock varargsParametersCodeBlock(ExecutableElement needsElement) {
        CodeBlock.Builder varargsCall = CodeBlock.builder();
        List<? extends VariableElement> parameters = needsElement.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement variableElement = parameters.get(i);
            varargsCall.add("$L", variableElement.getSimpleName().toString());
            if (i < parameters.size() - 1) {
                varargsCall.add(", ");
            }
        }
        return varargsCall.build();
    }

    public static String withCheckMethodName(ExecutableElement element) {
        return element.getSimpleName().toString() + ConstantsProvider.METHOD_SUFFIX;
    }

    public static boolean containsKey(List<String[]> ADD_WITH_CHECK_BODY_MAP, String[] value) {
        for(String[] strings: ADD_WITH_CHECK_BODY_MAP){
            if(Arrays.equals(strings,value)){
                return true;
            }
        }
        return false;
    }

    public static SensitivePermissionInterface getSensitivePermission(List<String[]> ADD_WITH_CHECK_BODY_MAP,
                                                                      String[] value) {
        if (containsKey(ADD_WITH_CHECK_BODY_MAP, value)) {
            SensitivePermissionInterface permissionInterface;
            String[] MANIFEST_WRITE_SETTING = new String[]{"android.permission.WRITE_SETTINGS"};
            String MANIFEST_SYSTEM_ALERT_WINDOW[] = new String[]{"android.permission.SYSTEM_ALERT_WINDOW"};

            if (deepEquals(MANIFEST_WRITE_SETTING, value)) {
                permissionInterface = new WriteSettingsHelper();
                return permissionInterface;
            } else if (deepEquals(MANIFEST_SYSTEM_ALERT_WINDOW, value)) {
                permissionInterface = new SystemAlertWindowHelper();
                return permissionInterface;
            }
        }
        return null;

    }
}
