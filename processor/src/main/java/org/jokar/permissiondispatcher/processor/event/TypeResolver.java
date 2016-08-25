package org.jokar.permissiondispatcher.processor.event;

import javax.lang.model.type.TypeMirror;

/**
 * Created by JokAr on 16/8/23.
 */
public interface TypeResolver {
    boolean isSubTypeOf(String subTypeClass, String superTypeClass);

    boolean isSameType(TypeMirror t1, TypeMirror t2);

    TypeMirror typeMirrorOf(String className);
}
