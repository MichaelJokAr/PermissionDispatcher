package org.jokar.permissiondispatcher.processor.exception;

import java.util.List;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by JokAr on 16/8/23.
 */
public class DuplicatedValueException extends RuntimeException {

    public DuplicatedValueException(List<String> value, ExecutableElement element,
                                    Class clazz) {
        super(value +" is duplicated in '"+element.getSimpleName().toString()+"()' annotated with '@"
                +clazz.getSimpleName().toString()+"'");
    }
}
