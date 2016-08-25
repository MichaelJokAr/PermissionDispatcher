package org.jokar.permissiondispatcher.processor.exception;

import org.jokar.permissiondispatcher.processor.RuntimePermissionsElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class NoAnnotatedMethodsException extends RuntimeException {

    public NoAnnotatedMethodsException(RuntimePermissionsElement element,Class clazz) {
        super("Annotated class '"+element.getClassName()+"' doesn't have any method annotated with '@"+clazz.getSimpleName().toString()+"'");
    }
}
