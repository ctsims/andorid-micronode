package org.commcare.hub.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.commcare.hub.R;
import org.commcare.hub.mirror.SyncableUser;

/**
 * Created by ctsims on 12/14/2015.
 */
public class AddUserDialogFragment extends DialogFragment {

    private EditText mUsername;
    private EditText mDomain;
    private EditText mPassword;

    SyncableUser user;
    UserDialogFragmentListener listener;

    public interface UserDialogFragmentListener {
        void onFinishEditDialog(SyncableUser user);

        void removeUser(SyncableUser user);
    }



    public AddUserDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public void attachDetails(SyncableUser user, UserDialogFragmentListener listener) {
        this.user = user;
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_add_user, container);
        mUsername = (EditText) view.findViewById(R.id.fragment_add_username);
        mDomain = (EditText) view.findViewById(R.id.fragment_add_domain);
        mPassword = (EditText) view.findViewById(R.id.fragment_add_password);

        if(user != null) {
            mUsername.setText(user.getUsername());
            mDomain.setText(user.getDomain());
            mPassword.setText(user.getPassword());

        }

        getDialog().setTitle("User Details");

        // Show soft keyboard automatically
        mUsername.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);


        ((Button)view.findViewById(R.id.delete_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.removeUser(user);
                AddUserDialogFragment.this.dismiss();
            }
        });

        ((Button)view.findViewById(R.id.cancel_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddUserDialogFragment.this.dismiss();
            }
        });

        ((Button)view.findViewById(R.id.confirm_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user == null) {
                    user = new SyncableUser(mUsername.getText().toString(), mDomain.getText().toString());
                    user.setPassword(mPassword.getText().toString());
                } else {
                    user.setUsername(mUsername.getText().toString());
                    user.setDomain(mDomain.getText().toString());
                    user.setPassword(mPassword.getText().toString());

                    if(user.getStatus() == SyncableUser.UserStatus.AuthError) {
                        user.updateStatus(SyncableUser.UserStatus.Requested);
                    }
                }

                // Return input text to activity
                listener.onFinishEditDialog(user);
                AddUserDialogFragment.this.dismiss();
            }
        });

        return view;
    }
}