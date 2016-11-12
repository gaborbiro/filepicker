package com.gaborbiro.filepicker;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionVerifier {

    private Activity mActivity;
    private String[] mPermissions;
    private String[] mRequestedPermissions;
    private int mRequestCode;

    public PermissionVerifier(Activity activity, String[] permissions) {
        mActivity = activity;
        mPermissions = Arrays.copyOfRange(permissions, 0, permissions.length);
        Arrays.sort(mPermissions);
    }

    /**
     * @param andAskForThem if set to yes and there are missing permissions, they will
     *                      be asked for
     * @return true if all specified <code>permissions</code> are already granted, false
     * otherwise
     * @throws IllegalStateException if method was already invoked but the corresponding
     *                               {@link #onRequestPermissionsResult(int, String[], int[])} was not
     */
    public boolean verifyPermissions(boolean andAskForThem, int requestCode)
            throws IllegalStateException {
        if (mRequestedPermissions != null) {
            throw new IllegalStateException(
                    "Still waiting for the results of the previous request. You " +
                            "probably want to re-create this class.");
        }
        mRequestCode = requestCode;
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : mPermissions) {
            if (!hasPermission(permission)) {
                permissionsToRequest.add(permission);
            }
        }
        if (andAskForThem && !permissionsToRequest.isEmpty()) {
            askForPermissions(permissionsToRequest.toArray(
                    new String[permissionsToRequest.size()]));
            return false;
        }
        return permissionsToRequest.isEmpty();
    }

    /**
     * Results will come in the
     * {@link android.support.v4.app.FragmentActivity#onRequestPermissionsResult(int, String[], int[])} method
     */
    private void askForPermissions(String[] permissions) {
        mRequestedPermissions = permissions;
        ActivityCompat.requestPermissions(mActivity, permissions, mRequestCode);
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mActivity, permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @return true if all the permissions were granted
     * @throws IllegalArgumentException if the specified <code>requestCode</code> does
     *                                  not equal the one used in the
     *                                  {@link #verifyPermissions(boolean, int)} method
     */
    public boolean onRequestPermissionsResult(int requestCode,
                                              @NonNull String permissions[], @NonNull int[] grantResults)
            throws IllegalArgumentException {
        // TODO verify the permissions parameter to see if everything is there
        if (requestCode == mRequestCode) {
            Arrays.sort(permissions);
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    Arrays.equals(mPermissions, permissions)) {
                return true;
            }
        } else {
            throw new IllegalArgumentException("Wrong requestCode");
        }
        return false;
    }
}
