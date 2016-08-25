package org.jokar.permissiondispatcher.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.processor.event.ConstantsProvider;
import org.jokar.permissiondispatcher.processor.event.TypeResolver;
import org.jokar.permissiondispatcher.processor.utils.ProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.requestCodeFieldName;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.permissionFieldName;

/**
 * Created by JokAr on 16/8/24.
 */
public class JavaFileBuilder {


    public static JavaFile createJavaFile(RuntimePermissionsElement element, TypeResolver typeResolver) {

        return JavaFile.builder(element.getPackageName(), createTypeSpec(element, typeResolver))
                .addFileComment(ConstantsProvider.FILE_COMMENT)
                .build();
    }


    private static TypeSpec createTypeSpec(RuntimePermissionsElement element, TypeResolver typeResolver) {
        return TypeSpec.classBuilder(element.getGeneratedClassName())
                .addModifiers(Modifier.FINAL)
                .addFields(createFields(element.getNeedsPermissionsMethods(), typeResolver))
                .addMethod(createConstructor())
                .addMethods(createWithCheckMethods(element))
                .build();

    }


    private static MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }


    private static List<MethodSpec> createWithCheckMethods(RuntimePermissionsElement element) {
        List<MethodSpec> methods = new ArrayList<>();
        for (ExecutableElement executableElement : element.getNeedsPermissionsMethods()) {
            methods.add(createWithCheckMethod(element, executableElement));
        }
        return methods;
    }


    private static MethodSpec createWithCheckMethod(RuntimePermissionsElement element,
                                                    ExecutableElement method) {
        String targetParam = "target";

        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString() + ConstantsProvider.METHOD_SUFFIX)
                .addTypeVariables(element.getTypeVariables())
                .addModifiers(Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(element.getTypeName(), targetParam);

        // If the method has parameters, add those as well
        List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement element1 : parameters) {
            builder.addParameter(TypeName.get(element1.asType()), element1.getSimpleName().toString());
        }

        // Delegate method body generation to implementing classes
        addWithCheckBody(builder, method, element, targetParam);
        return builder.build();
    }

    private static void addWithCheckBody(MethodSpec.Builder builder,
                                         ExecutableElement needsMethod,
                                         RuntimePermissionsElement element,
                                         String targetParam) {
        // Create field names for the constants to use
        String requestCodeField = requestCodeFieldName(needsMethod);
        String permissionField = permissionFieldName(needsMethod);

        // Add the conditional for when permission has already been granted
        String[] needsPermissionParameter = needsMethod.getAnnotation(NeedsPermission.class).value();
        String activityVar = element.getClassType().getActivity();

    }


    private static List<FieldSpec> createFields(List<ExecutableElement> needsPermissionsMethods,
                                                TypeResolver typeResolver) {
        List<FieldSpec> fields = new ArrayList<>();
        int index = 0;
        for (ExecutableElement element : needsPermissionsMethods) {
            fields.add(createRequestCodeField(requestCodeFieldName(element), index));

            String[] value = element.getAnnotation(NeedsPermission.class).value();
            fields.add(createPermissionField(permissionFieldName(element), value));

            if (!element.getParameters().isEmpty()) {
                fields.add(createPendingRequestField(element));
            }
        }
        return fields;
    }

    private static FieldSpec createPendingRequestField(ExecutableElement element) {

        return FieldSpec.builder(ClassName.get("permissions.dispatcher", "GrantableRequest"),
                ConstantsProvider.GEN_PENDING_PREFIX + element.getSimpleName().toString().toUpperCase())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .build();
    }

    private static FieldSpec createRequestCodeField(String name, int index) {
        return FieldSpec
                .builder(int.class, name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", index)
                .build();
    }

    private static FieldSpec createPermissionField(String name, String... value) {
        return FieldSpec
                .builder(String[].class, name)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$N", "new String[]" + ProcessorUtil.toString(value))
                .build();
    }
}
