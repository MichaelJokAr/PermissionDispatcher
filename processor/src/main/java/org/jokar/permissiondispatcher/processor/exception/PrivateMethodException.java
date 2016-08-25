package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class PrivateMethodException extends RuntimeException {

    public PrivateMethodException(ExecutableElement element,Class clazz) {
        super("Method '"+element.getSimpleName().toString()+"()' annotated with '@"+clazz.getSimpleName().toString()+"' must not be private");
    }
}
