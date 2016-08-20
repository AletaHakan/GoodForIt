package com.gastonheaps.goodforit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gastonheaps.goodforit.model.Loan;
import com.gastonheaps.goodforit.model.User;
import com.gastonheaps.goodforit.util.FirebaseUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gaston on 6/5/2016.
 */
public class NewLoanActivity extends BaseActivity {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    private static final String TAG = "NewLoanActivity";
    private static final String REQUIRED = "Required";

    private DatabaseReference mDatabase;

    private Integer mType = 1;
    private static Calendar mDate;
    private static String mLoanKey;

    private LinearLayout mContactLayout;
    private ContactsEditText mContactText;
    private ImageButton mSwapButton;
    private TextView mUserProfileText;
    private EditText mAmountText;
    private Button mDateButton;
    private EditText mNotesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_loan);

        // [START initialize_database_ref]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mLoanKey = mDatabase.child("loans").push().getKey();

        mContactLayout = (LinearLayout) findViewById(R.id.new_loan_contact_layout);
        mContactText = (ContactsEditText) findViewById(R.id.new_loan_contact);
        mUserProfileText = (TextView) findViewById(R.id.new_loan_user_profile);
        mAmountText = (EditText) findViewById(R.id.new_loan_amount);
        mNotesText = (EditText) findViewById(R.id.new_loan_notes);

        mSwapButton = (ImageButton) findViewById(R.id.new_loan_swap_button);
        mSwapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contact = mContactText.getText().toString();

                switch (mType) {
                    case 1:
                        mContactLayout.removeAllViews();
                        mContactLayout.addView(mUserProfileText);
                        mContactLayout.addView(mSwapButton);
                        mContactLayout.addView(mContactText);

                        mType = 2;
                        break;
                    case 2:
                        mContactLayout.removeAllViews();
                        mContactLayout.addView(mContactText);
                        mContactLayout.addView(mSwapButton);
                        mContactLayout.addView(mUserProfileText);

                        mType = 1;
                        break;
                }
                mContactText.setText(contact);
            }
        });

        mDateButton = (Button) findViewById(R.id.new_loan_date);
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerFragment(v);
            }
        });
    }

    private void submitLoan() {
        final String person = mContactText.getText().toString();
        final Integer type = mType;
        final Integer amount = Integer.valueOf(mAmountText.getText().toString());
        final Long date = mDate.getTimeInMillis();
        final String notes = mNotesText.getText().toString();

        // Title is required
        if (TextUtils.isEmpty(person)) {
            mContactText.setError(REQUIRED);
            return;
        }

        // Body is required
        if (TextUtils.isEmpty(amount.toString())) {
            mAmountText.setError(REQUIRED);
            return;
        }

        // [START single_value_read]
        final String uid = FirebaseUtil.getCurrentUserId();
        Log.d("TEST: ", uid);
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + uid + " is unexpectedly null");
                            Toast.makeText(NewLoanActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            createNewLoan(person, type, amount, date, notes, uid);
                        }

                        // Finish this Activity, back to the stream
                        finish();
                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
        // [END single_value_read]
    }

    private void createNewLoan(String person, Integer type, Integer amount, Long date, String notes, String uid) {
        Loan loan = new Loan(person, type, amount, date, notes, uid);
        Map<String, Object> loanValues = loan.toMap();

        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/loans/" + mLoanKey, loanValues);
        childUpdates.put("/user-loans/" + uid + "/" + mLoanKey, loanValues);

        mDatabase.updateChildren(childUpdates);
    }

    public void showDatePickerFragment(View v){
        DialogFragment dialogFragment = new DatePickerFragment();
        dialogFragment.show(getFragmentManager(), "date_picker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_new_loan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            submitLoan();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            mDate = Calendar.getInstance();
            mDate.set(year, month, day);
            Button dateButton = (Button) getActivity().findViewById(R.id.new_loan_date);
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
            dateButton.setText(dateFormat.format(mDate.getTime()));
        }
    }
}