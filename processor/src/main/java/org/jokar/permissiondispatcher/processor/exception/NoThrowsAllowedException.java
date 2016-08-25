package org.jokar.permissiondispatcher.processor.exception;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class NoThrowsAllowedException extends RuntimeException {

    public NoThrowsAllowedException(ExecutableElement element) {
        super("Method '"+element.getSimpleName().toString()+"()'  must not have any 'throws' declaration in its signature");
    }
}
