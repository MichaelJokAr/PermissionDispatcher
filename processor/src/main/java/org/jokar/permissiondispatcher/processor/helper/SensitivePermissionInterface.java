package org.jokar.permissiondispatcher.processor.helper;

import com.squareup.javapoet.MethodSpec;

/**
 * Created by JokAr on 16/8/26.
 */
public interface SensitivePermissionInterface {
    void addHasSelfPermissionsCondition(MethodSpec.Builder builder, String activityVar, String permissionField);

    void addRequestPermissionsStatement(MethodSpec.Builder builder, String activityVar, String requestCodeField);
}
