package org.jokar.permissiondispatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Register some methods which permissions are needed.
 * Created by JokAr on 16/8/22.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NeedsPermission {
    String[] value();
}
