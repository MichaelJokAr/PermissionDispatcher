package org.jokar.permissiondispatcher.library;

/**
 * Created by JokAr on 16/8/23.
 */
public interface GrantableRequest extends PermissionRequest {

    void grant();
}
