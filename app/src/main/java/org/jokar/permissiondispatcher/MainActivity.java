package org.jokar.permissiondispatcher;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.annotation.OnShowRationale;
import org.jokar.permissiondispatcher.annotation.RuntimePermissions;
import org.jokar.permissiondispatcher.library.PermissionRequest;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void takeCamera(){

    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void ohowRationaleTakeCamera(PermissionRequest permissionRequest){

    }
}
