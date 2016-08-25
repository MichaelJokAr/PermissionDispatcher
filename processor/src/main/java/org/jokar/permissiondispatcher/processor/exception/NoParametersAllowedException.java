package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/24.
 */
public class NoParametersAllowedException extends RuntimeException {

    public NoParametersAllowedException(ExecutableElement element) {
        super("Method '"+element.getSimpleName().toString()+"()' must not have any parameters");
    }
}
