package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class MixPermissionTypeException extends RuntimeException {

    public MixPermissionTypeException(ExecutableElement element,String permissionName) {
        super("Method '"+element.getSimpleName().toString()+"()' defines "+permissionName
        +" with other permissions at the same time.");
    }
}
