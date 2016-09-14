package com.gastonheaps.goodforit.ui;

import android.app.Activity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.gastonheaps.goodforit.R;

public class AddPaymentDialogFragment extends DialogFragment {

    private EditText mEditTextAmount;
    private EditText mEditTextNotes;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.dialog_add_payment, null);
        mEditTextAmount = (EditText) rootView.findViewById(R.id.add_payment_amount);
        mEditTextNotes = (EditText) rootView.findViewById(R.id.add_payment_notes);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(rootView)
                // Add action buttons
                .setPositiveButton(R.string.add_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(
                                AddPaymentDialogFragment.this,
                                mEditTextAmount.getText().toString(),
                                mEditTextNotes.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(AddPaymentDialogFragment.this);
                        AddPaymentDialogFragment.this.getDialog().cancel();
                    }
                })
                .setTitle(R.string.dialog_add_payment_title);
        return builder.create();
    }

    public String getAmount() {
        return mEditTextAmount.getText().toString();
    }

    public String getNotes() {
        return mEditTextNotes.getText().toString();
    }

    /* The activity that creates an instance of this dialog fragment must
 * implement this interface in order to receive event callbacks.
 * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String amount, String notes);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}
