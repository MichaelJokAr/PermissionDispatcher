package org.jokar.permissiondispatcher.library;

/**
 * Created by JokAr on 16/8/22.
 */
public interface PermissionRequest {
    void proceed();

    void cancel();
}
