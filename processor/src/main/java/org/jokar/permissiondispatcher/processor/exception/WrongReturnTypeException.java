package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class WrongReturnTypeException extends RuntimeException {
    public WrongReturnTypeException(ExecutableElement element) {
        super("Method '"+element.getSimpleName().toString()+"()' must specify return type 'void', not '"+element.getReturnType()+"'");
    }
}
