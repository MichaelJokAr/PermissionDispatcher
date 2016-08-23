package org.jokar.permissiondispatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Register an <code>Activity</code> or <code>Fragment</code> to handle permissions.
 * Created by JokAr on 16/8/22.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface RuntimePermissions {
}
