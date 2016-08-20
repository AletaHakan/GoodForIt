package com.gastonheaps.goodforit;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by Gaston on 6/5/2016.
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG_DIALOG_FRAGMENT = "tagDialogFragment";

    protected void showProgressDialog(String message) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getExistingDialogFragment();
        if (prev == null) {
            ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(message);
            fragment.show(ft, TAG_DIALOG_FRAGMENT);
        }
    }

    protected void dismissProgressDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getExistingDialogFragment();
        if (prev != null) {
            ft.remove(prev).commit();
        }
    }

    private Fragment getExistingDialogFragment() {
        return getFragmentManager().findFragmentByTag(TAG_DIALOG_FRAGMENT);
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

}