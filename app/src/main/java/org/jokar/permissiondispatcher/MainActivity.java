package org.jokar.permissiondispatcher;

import android.Manifest;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.jokar.permissiondispatcher.annotation.NeedsPermission;
import org.jokar.permissiondispatcher.annotation.OnNeverAskAgain;
import org.jokar.permissiondispatcher.annotation.OnPermissionDenied;
import org.jokar.permissiondispatcher.annotation.OnShowRationale;
import org.jokar.permissiondispatcher.annotation.RuntimePermissions;
import org.jokar.permissiondispatcher.library.PermissionRequest;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button button1;
    private Button button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void takeCamera(String url) {
        Toast.makeText(getApplicationContext(), url, Toast.LENGTH_SHORT).show();
    }

    //
    @OnShowRationale(Manifest.permission.CAMERA)
    void ohowRationaleTakeCamera(final PermissionRequest request) {

        showRationDialog(request,"Request Camera");
    }

    private void showRationDialog(final PermissionRequest request,String message) {
        new AlertDialog.Builder(this)
                .setTitle("RequestPermission")
                .setMessage(message)
                .setNegativeButton("deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();

                    }
                })
                .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                }).show();
    }

    //
    @OnPermissionDenied(Manifest.permission.CAMERA)
    void onDeniedTakeCamera() {
        Toast.makeText(getApplicationContext(), "Camera Denied", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void onNeverAskTakeCamera() {
        Toast.makeText(getApplicationContext(), "Camera NeverAsk", Toast.LENGTH_SHORT).show();
    }

    @NeedsPermission({Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS})
    void showContacts() {
        Toast.makeText(getApplicationContext(), "Contacts", Toast.LENGTH_SHORT).show();
    }

    @OnShowRationale({Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS})
    void showRationaleForContact(PermissionRequest request) {
        showRationDialog(request,"Request Contacts");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1: {
                MainActivityPermissionsDispatcher.takeCameraWithCheck(this, "camera");
                break;
            }
            case R.id.button2: {
                MainActivityPermissionsDispatcher.showContactsWithCheck(this);
                break;
            }
        }

    }
}
