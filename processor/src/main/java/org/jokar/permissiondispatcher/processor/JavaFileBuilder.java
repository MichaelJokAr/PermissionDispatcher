package org.jokar.permissiondispatcher.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.processor.event.ClassType;
import org.jokar.permissiondispatcher.processor.event.ConstantsProvider;
import org.jokar.permissiondispatcher.processor.event.TypeResolver;
import org.jokar.permissiondispatcher.processor.helper.SensitivePermissionInterface;
import org.jokar.permissiondispatcher.processor.helper.SystemAlertWindowHelper;
import org.jokar.permissiondispatcher.processor.helper.WriteSettingsHelper;
import org.jokar.permissiondispatcher.processor.utils.ProcessorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
//import static java.util.Arrays.deepEquals;

import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.requestCodeFieldName;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.permissionFieldName;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.varargsParametersCodeBlock;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.getValueFromAnnotation;
import static org.jokar.permissiondispatcher.processor.utils.ProcessorUtil.withCheckMethodName;

/**
 * Created by JokAr on 16/8/24.
 */
public class JavaFileBuilder {
    protected static ClassName PERMISSION_UTILS = ClassName.get("org.jokar.permissiondispatcher.library", "PermissionUtils");
    private static ClassName ACTIVITY_COMPAT = ClassName.get("android.support.v4.app", "ActivityCompat");
    private static String[] MANIFEST_WRITE_SETTING = new String[]{"android.permission.WRITE_SETTINGS"};
    private static String MANIFEST_SYSTEM_ALERT_WINDOW[] = new String[]{"android.permission.SYSTEM_ALERT_WINDOW"};
    private static HashMap<String[], SensitivePermissionInterface> ADD_WITH_CHECK_BODY_MAP =
            new HashMap<String[], SensitivePermissionInterface>();

    static {
        ADD_WITH_CHECK_BODY_MAP.put(MANIFEST_WRITE_SETTING, new WriteSettingsHelper());
        ADD_WITH_CHECK_BODY_MAP.put(MANIFEST_SYSTEM_ALERT_WINDOW, new SystemAlertWindowHelper());

    }

    /**
     * Creates the JavaFile for the provided @RuntimePermissions element.
     * <p>
     * This will delegate to other methods that compose generated code.
     */
    public static JavaFile createJavaFile(RuntimePermissionsElement element, TypeResolver typeResolver) {

        return JavaFile.builder(element.getPackageName(), createTypeSpec(element, typeResolver))
                .addFileComment(ConstantsProvider.FILE_COMMENT)
                .build();
    }


    private static TypeSpec createTypeSpec(RuntimePermissionsElement element,
                                           TypeResolver typeResolver) {
        return TypeSpec.classBuilder(element.getGeneratedClassName())
                .addModifiers(Modifier.FINAL)
                .addFields(createFields(element.getNeedsPermissionsMethods()))
                .addMethod(createConstructor())
                .addMethods(createWithCheckMethods(element))
                .addMethods(createPermissionHandlingMethods(element))
                .addTypes(createPermissionRequestClasses(element))
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
            // For each @NeedsPermission method, create the "WithCheck" equivalent
            methods.add(createWithCheckMethod(element, executableElement));
        }
        return methods;
    }


    private static List<MethodSpec> createPermissionHandlingMethods(RuntimePermissionsElement element) {
        List<MethodSpec> methods = new ArrayList<>();
        if (hasNormalPermission(element)) {
            methods.add(createPermissionResultMethod(element));
        }

        if (hasSystemAlertWindowPermission(element) || hasWriteSettingPermission(element)) {
            methods.add(createOnActivityResultMethod(element));
        }
        return methods;
    }

    private static List<TypeSpec> createPermissionRequestClasses(RuntimePermissionsElement element) {
        List<TypeSpec> classes = new ArrayList<>();
        List<ExecutableElement> needsPermissionsMethods = element.getNeedsPermissionsMethods();
        for (ExecutableElement needMethods : needsPermissionsMethods) {
            String[] value = needMethods.getAnnotation(NeedsPermission.class).value();
            ExecutableElement onRationale = element.findOnRationaleForNeeds(value);
            if (onRationale != null || !needMethods.getParameters().isEmpty()) {
                classes.add(createPermissionRequestClass(element, needMethods));
            }
        }
        return classes;
    }

    private static TypeSpec createPermissionRequestClass(RuntimePermissionsElement element,
                                                         ExecutableElement needsMethod) {
        // Select the superinterface of the generated class
        // based on whether or not the annotated method has parameters
        boolean hasParameters = needsMethod.getParameters().isEmpty();
        String superInterfaceName = hasParameters ? "PermissionRequest" : "GrantableRequest";
        TypeName targetType = element.getTypeName();

        TypeSpec.Builder builder = TypeSpec.classBuilder(ProcessorUtil.permissionRequestTypeName(needsMethod))
                .addTypeVariables(element.getTypeVariables())
                .addSuperinterface(ClassName.get("org.jokar.permissiondispatcher.library", superInterfaceName))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        // Add required fields to the target
        String weakFieldName = "weakTarget";
        ParameterizedTypeName weakFieldType = ParameterizedTypeName.get(ClassName.get("java.lang.ref", "WeakReference"), targetType);
        builder.addField(weakFieldType, weakFieldName, Modifier.PRIVATE, Modifier.FINAL);

        List<? extends VariableElement> parameters = needsMethod.getParameters();
        for (VariableElement variableElement : parameters) {
            builder.addField(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString(), Modifier.PRIVATE, Modifier.FINAL);
        }

        // Add constructor
        String targetParam = "target";
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(targetType, targetParam)
                .addStatement("this.$L = new WeakReference<>($N)", weakFieldName, targetParam);

        for (VariableElement variableElement : parameters) {
            String fieldName = variableElement.getSimpleName().toString();
            constructorBuilder
                    .addParameter(TypeName.get(variableElement.asType()), fieldName)
                    .addStatement("this.$L = $N", fieldName, fieldName);
        }

        builder.addMethod(constructorBuilder.build());

        // Add proceed() override
        MethodSpec.Builder proceedMethod = MethodSpec.methodBuilder("proceed")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addStatement("$T target = $N.get()", targetType, weakFieldName)
                .addStatement("if (target == null) return");

        String requestCodeField = ProcessorUtil.requestCodeFieldName(needsMethod);
        String[] value = needsMethod.getAnnotation(NeedsPermission.class).value();

//        SensitivePermissionInterface sensitivePermissionInterface;
//        if(deepEquals(value,MANIFEST_WRITE_SETTING)){
//            sensitivePermissionInterface = new WriteSettingsHelper();
//            sensitivePermissionInterface.addRequestPermissionsStatement(proceedMethod,
//                    element.getClassType().getActivity(), requestCodeField);
//        }else if(deepEquals(value,MANIFEST_SYSTEM_ALERT_WINDOW)){
//            sensitivePermissionInterface = new SystemAlertWindowHelper();
//            sensitivePermissionInterface.addRequestPermissionsStatement(proceedMethod,
//                    element.getClassType().getActivity(), requestCodeField);
//        }else {
//            addRequestPermissionsStatement(proceedMethod, targetParam, permissionFieldName(needsMethod), requestCodeField, element.getClassType());
//
//        }
        if (ADD_WITH_CHECK_BODY_MAP.get(value) != null) {
            SensitivePermissionInterface sensitivePermissionInterface = ADD_WITH_CHECK_BODY_MAP.get(value);
            sensitivePermissionInterface.addRequestPermissionsStatement(proceedMethod,
                    element.getClassType().getActivity(), requestCodeField);
        } else {
            addRequestPermissionsStatement(proceedMethod, targetParam, permissionFieldName(needsMethod), requestCodeField, element.getClassType());
        }

        builder.addMethod(proceedMethod.build());

        //Add cancel() override method
        MethodSpec.Builder cancelMethod = MethodSpec.methodBuilder("cancel")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        ExecutableElement onDenied = element.findOnDeniedForNeeds(value);
        if (onDenied != null) {
            cancelMethod
                    .addStatement("$T target = $N.get()", targetType, weakFieldName)
                    .addStatement("if (target == null) return")
                    .addStatement("target.$N()", onDenied.getSimpleName().toString());
        }
        builder.addMethod(cancelMethod.build());

        // For classes with additional parameters, add a "grant()" method
        if (!hasParameters) {
            MethodSpec.Builder grantMethod = MethodSpec.methodBuilder("grant")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID);

            grantMethod
                    .addStatement("$T target = $N.get()", targetType, weakFieldName)
                    .addStatement("if (target == null) return");

            // Compose the call to the permission-protected method;
            // since the number of parameters is variable, utilize the low-level CodeBlock here
            // to compose the method call and its parameters
            grantMethod.addCode(
                    CodeBlock.builder()
                            .add("target.$N(", needsMethod.getSimpleName().toString())
                            .add(varargsParametersCodeBlock(needsMethod))
                            .addStatement(")")
                            .build()
            );
            builder.addMethod(grantMethod.build());

        }
        return builder.build();
    }

    private static MethodSpec createOnActivityResultMethod(RuntimePermissionsElement rpe) {
        String targetParam = "target";
        String requestCodeParam = "requestCode";
        String grantResultsParam = "grantResults";
        MethodSpec.Builder builder = MethodSpec.methodBuilder("onActivityResult")
                .addTypeVariables(rpe.getTypeVariables())
                .addModifiers(Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(rpe.getTypeName(), targetParam)
                .addParameter(TypeName.INT, requestCodeParam);

        builder.beginControlFlow("switch ($N)", requestCodeParam);
        List<ExecutableElement> needsPermissionsMethods = rpe.getNeedsPermissionsMethods();
        for (ExecutableElement element : needsPermissionsMethods) {
            String[] needsPermissionParameter = element.getAnnotation(NeedsPermission.class).value();
            if (!ADD_WITH_CHECK_BODY_MAP.containsKey(needsPermissionParameter)) {
                continue;
            }

            builder.addCode("case $N:\n", requestCodeFieldName(element));

            addResultCaseBody(builder, element, rpe, targetParam, grantResultsParam);
        }
        builder
                .addCode("default:\n")
                .addStatement("break")
                .endControlFlow();

        return builder.build();
    }


    private static MethodSpec createPermissionResultMethod(RuntimePermissionsElement element) {
        String targetParam = "target";
        String requestCodeParam = "requestCode";
        String grantResultsParam = "grantResults";
        MethodSpec.Builder builder = MethodSpec.methodBuilder("onRequestPermissionsResult")
                .addTypeVariables(element.getTypeVariables())
                .addModifiers(Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(element.getTypeName(), targetParam)
                .addParameter(TypeName.INT, requestCodeParam)
                .addParameter(ArrayTypeName.of(TypeName.INT), grantResultsParam);

        // For each @NeedsPermission method, add a switch case
        builder.beginControlFlow("switch ($N)", requestCodeParam);
        List<ExecutableElement> needsPermissionsMethods = element.getNeedsPermissionsMethods();
        for (ExecutableElement needsMethod : needsPermissionsMethods) {
            String[] needsPermissionParameter = needsMethod.getAnnotation(NeedsPermission.class).value();

            if (ADD_WITH_CHECK_BODY_MAP.containsKey(needsPermissionParameter)) {
                continue;
            }

            builder.addCode("case $N:\n", requestCodeFieldName(needsMethod));

            // Delegate switch-case generation to implementing classes
            addResultCaseBody(builder, needsMethod, element, targetParam, grantResultsParam);

        }

        // Add the default case
        builder
                .addCode("default:\n")
                .addStatement("break")
                .endControlFlow();

        return builder.build();
    }

    private static void addResultCaseBody(MethodSpec.Builder builder,
                                          ExecutableElement needsMethod,
                                          RuntimePermissionsElement rpe,
                                          String targetParam,
                                          String grantResultsParam) {

        String[] needsPermissionParameter = needsMethod.getAnnotation(NeedsPermission.class).value();
        ExecutableElement onDenied = rpe.findOnDeniedForNeeds(needsPermissionParameter);
        boolean hasDenied = onDenied != null;
        String permissionField = ProcessorUtil.permissionFieldName(needsMethod);
        if (!ADD_WITH_CHECK_BODY_MAP.containsKey(needsPermissionParameter)) {
            builder.beginControlFlow("if ($T.getTargetSdkVersion($N) < 23 && !$T.hasSelfPermissions($N, $N))",
                    PERMISSION_UTILS, rpe.getClassType().getActivity(), PERMISSION_UTILS, rpe.getClassType().getActivity(), permissionField);
            if (hasDenied) {
                builder.addStatement("$N.$N()", targetParam, onDenied != null ? onDenied.getSimpleName().toString() : "");
            }
            builder.addStatement("return");
            builder.endControlFlow();
        }
        // Add the conditional for "permission verified"
        if (ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter) != null) {
            SensitivePermissionInterface sensitivePermissionInterface = ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter);
            sensitivePermissionInterface.addHasSelfPermissionsCondition(builder, rpe.getClassType().getActivity(), permissionField);
        } else {
            builder.beginControlFlow("if ($T.verifyPermissions($N))", PERMISSION_UTILS, grantResultsParam);
        }
        // Based on whether or not the method has parameters, delegate to the "pending request" object or invoke the method directly
        boolean hasParameters = needsMethod.getParameters().isEmpty();
        if (!hasParameters) {
            String pendingField = ProcessorUtil.pendingRequestFieldName(needsMethod);
            builder.beginControlFlow("if ($N != null)", pendingField);
            builder.addStatement("$N.grant()", pendingField);
            builder.endControlFlow();
        } else {
            builder.addStatement("target.$N()", needsMethod.getSimpleName().toString());
        }
        // Add the conditional for "permission denied" and/or "never ask again", if present
        ExecutableElement onNeverAsk = rpe.findOnNeverAskForNeeds(needsPermissionParameter);
        boolean hasNeverAsk = onNeverAsk != null;

        if (hasDenied || hasNeverAsk) {
            builder.nextControlFlow("else");
        }
        if (hasNeverAsk) {
            // Split up the "else" case with another if condition checking for "never ask again" first
            addShouldShowRequestPermissionRationaleCondition(builder, targetParam, permissionFieldName(needsMethod), false, rpe.getClassType());
            builder.addStatement("target.$N()", onNeverAsk != null ? onNeverAsk.getSimpleName().toString() : "");

            // If a "permission denied" is present as well, go into an else case, otherwise close this temporary branch
            if (hasDenied) {
                builder.nextControlFlow("else");
            } else {
                builder.endControlFlow();
            }
        }

        if (hasDenied) {
            // Add the "permissionDenied" statement
            builder.addStatement("$N.$N()", targetParam, onDenied != null ? onDenied.getSimpleName().toString() : "");

            // Close the additional control flow potentially opened by a "never ask again" method
            if (hasNeverAsk) {
                builder.endControlFlow();
            }
        }
        // Close the "switch" control flow
        builder.endControlFlow();

        // Remove the temporary pending request field, in case it was used for a method with parameters
        if (!hasParameters) {
            builder.addStatement("$N = null", ProcessorUtil.pendingRequestFieldName(needsMethod));
        }
        builder.addStatement("break");
    }


    private static MethodSpec createWithCheckMethod(RuntimePermissionsElement element,
                                                    ExecutableElement method) {
        String targetParam = "target";

        MethodSpec.Builder builder = MethodSpec.methodBuilder(withCheckMethodName(method))
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

        if (ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter) != null) {
            SensitivePermissionInterface sensitivePermissionInterface = ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter);

            sensitivePermissionInterface.addHasSelfPermissionsCondition(builder, activityVar, permissionField);
        } else {
            builder.beginControlFlow("if ($T.hasSelfPermissions($N, $N))", PERMISSION_UTILS,
                    activityVar, permissionField);
        }

        builder.addCode(CodeBlock.builder()
                .add("$N.$N(", targetParam, needsMethod.getSimpleName().toString())
                .add(varargsParametersCodeBlock(needsMethod))
                .addStatement(")")
                .build()
        );
        builder.nextControlFlow("else");

        // Add the conditional for "OnShowRationale", if present
        ExecutableElement onRationale = element.findOnRationaleForNeeds(needsPermissionParameter);
        boolean hasParameters = needsMethod.getParameters().isEmpty();
        if (!hasParameters) {
            // If the method has parameters, precede the potential OnRationale call with
            // an instantiation of the temporary Request object
            CodeBlock.Builder varargsCall = CodeBlock.builder()
                    .add("$N = new $N($N, ",
                            ProcessorUtil.pendingRequestFieldName(needsMethod),
                            ProcessorUtil.permissionRequestTypeName(needsMethod),
                            targetParam
                    )
                    .add(varargsParametersCodeBlock(needsMethod))
                    .addStatement(")");
            builder.addCode(varargsCall.build());
        }

        if (onRationale != null) {
            addShouldShowRequestPermissionRationaleCondition(builder, targetParam, permissionField,
                    true, element.getClassType());
            if (!hasParameters) {
                // For methods with parameters, use the PermissionRequest instantiated above
                builder.addStatement("$N.$N($N)", targetParam, onRationale.getSimpleName().toString(),
                        ProcessorUtil.pendingRequestFieldName(needsMethod));
            } else {
                // Otherwise, create a new PermissionRequest on-the-fly
                builder.addStatement("$N.$N(new $N($N))", targetParam, onRationale.getSimpleName().toString(),
                        ProcessorUtil.permissionRequestTypeName(needsMethod), targetParam);
            }
            builder.nextControlFlow("else");
        }

        // Add the branch for "request permission"
        if (ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter) != null) {
            SensitivePermissionInterface sensitivePermissionInterface = ADD_WITH_CHECK_BODY_MAP.get(needsPermissionParameter);
            sensitivePermissionInterface.addRequestPermissionsStatement(builder, activityVar, requestCodeField);
        } else {
            addRequestPermissionsStatement(builder, targetParam, permissionField, requestCodeField, element.getClassType());
        }
        if (onRationale != null) {
            builder.endControlFlow();
        }

        builder.endControlFlow();

    }


    private static List<FieldSpec> createFields(List<ExecutableElement> needsPermissionsMethods) {
        List<FieldSpec> fields = new ArrayList<>();
        int index = 0;
        for (ExecutableElement element : needsPermissionsMethods) {
            // For each method annotated with @NeedsPermission, add REQUEST integer and PERMISSION String[] fields
            fields.add(createRequestCodeField(requestCodeFieldName(element), index));

            String[] value = element.getAnnotation(NeedsPermission.class).value();
            fields.add(createPermissionField(permissionFieldName(element), value));

            if (!element.getParameters().isEmpty()) {
                fields.add(createPendingRequestField(element));
            }
            index++;
        }
        return fields;
    }

    private static FieldSpec createPendingRequestField(ExecutableElement element) {

        return FieldSpec.builder(ClassName.get("org.jokar.permissiondispatcher.library", "GrantableRequest"),
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

    private static void addShouldShowRequestPermissionRationaleCondition(MethodSpec.Builder builder,
                                                                         String targetParam,
                                                                         String permissionField,
                                                                         Boolean isPositiveCondition,
                                                                         ClassType classType) {
        if (classType == ClassType.ACTIVITY) {
            builder.beginControlFlow("if ($N$T.shouldShowRequestPermissionRationale($N, $N))",
                    isPositiveCondition ? "" : "!", PERMISSION_UTILS, targetParam, permissionField);
        } else if (classType == ClassType.V4FRAGMENT) {
            builder.beginControlFlow("if ($N$T.shouldShowRequestPermissionRationale($N.getActivity(), $N))", isPositiveCondition ? "" : "!",
                    PERMISSION_UTILS, targetParam, permissionField);

        }
    }

    private static void addRequestPermissionsStatement(MethodSpec.Builder builder, String targetParam,
                                                       String permissionField, String requestCodeField,
                                                       ClassType classType) {
        if (classType == ClassType.ACTIVITY) {
            builder.addStatement("$T.requestPermissions($N, $N, $N)", ACTIVITY_COMPAT,
                    targetParam, permissionField, requestCodeField);
        } else if (classType == ClassType.V4FRAGMENT) {
            builder.addStatement("$N.requestPermissions($N, $N)", targetParam, permissionField, requestCodeField);

        }
    }

    private static boolean hasNormalPermission(RuntimePermissionsElement element) {

        List<ExecutableElement> needsPermissionsMethods = element.getNeedsPermissionsMethods();
        for (ExecutableElement executableElement : needsPermissionsMethods) {
            List<String> permissionValue = getValueFromAnnotation(executableElement, NeedsPermission.class);
            if (!permissionValue.contains(MANIFEST_SYSTEM_ALERT_WINDOW) && !permissionValue.contains(MANIFEST_WRITE_SETTING)) {
                return true;
            }

        }
        return false;
    }

    private static boolean hasSystemAlertWindowPermission(RuntimePermissionsElement rpe) {
        return isDefinePermission(rpe, MANIFEST_SYSTEM_ALERT_WINDOW);
    }

    private static boolean hasWriteSettingPermission(RuntimePermissionsElement rpe) {

        return isDefinePermission(rpe, MANIFEST_WRITE_SETTING);
    }


    private static boolean isDefinePermission(RuntimePermissionsElement rpe, String[] permissionName) {
        List<ExecutableElement> needsPermissionsMethods = rpe.getNeedsPermissionsMethods();
        for (ExecutableElement executableElement : needsPermissionsMethods) {
            List<String> permissionValue = getValueFromAnnotation(executableElement, NeedsPermission.class);
            if (permissionValue.contains(permissionName)) {
                return true;
            }
        }
        return false;
    }

}
