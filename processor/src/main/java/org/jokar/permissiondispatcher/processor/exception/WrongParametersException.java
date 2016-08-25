package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Created by JokAr on 16/8/24.
 */
public class WrongParametersException extends RuntimeException {
    public WrongParametersException(ExecutableElement element, String clazz) {
        super("Method '" + element.getSimpleName().toString() + "()' must declare parameters of type " + clazz);

    }
}
