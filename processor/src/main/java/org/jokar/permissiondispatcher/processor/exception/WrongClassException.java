package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.TypeElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class WrongClassException extends RuntimeException {
    public WrongClassException(TypeElement message) {
        super("Class '"+message.getSimpleName().toString()+"' can't be annotated with '@RuntimePermissions");
    }
}
