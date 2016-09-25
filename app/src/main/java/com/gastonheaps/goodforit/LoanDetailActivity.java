package com.gastonheaps.goodforit;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.gastonheaps.goodforit.model.Loan;
import com.gastonheaps.goodforit.model.Payment;
import com.gastonheaps.goodforit.ui.AddPaymentDialogFragment;
import com.gastonheaps.goodforit.ui.ContactsEditText;
import com.gastonheaps.goodforit.util.GlideUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoanDetailActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener,
        AddPaymentDialogFragment.NoticeDialogListener,
        AppBarLayout.OnOffsetChangedListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "LoanDetailActivity";
    public static final String EXTRA_LOAN_KEY = "loan_key";

    private String mLoanKey;
    private Loan mLoan;
    private Integer mTotalAmount;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mRef;

    private DatabaseReference mLoanRef;
    private DatabaseReference mPaymentsRef;
    private DatabaseReference mLoanPaymentsRef;

    private static final int PERCENTAGE_TO_ANIMATE_AVATAR = 20;
    private boolean mIsAvatarShown = true;
    private AppBarLayout mAppbarLayout;
    private int mMaxScrollSize;

    private CollapsingToolbarLayout mToolbarLayout;
    private Uri mContactUri;
    private CircleImageView mContactImage;
    private TextView mContactName;
    private CircleImageView mUserImage;
    private TextView mUserName;
    private RecyclerView mList;
    private LinearLayoutManager mManager;
    private FirebaseRecyclerAdapter<Payment, PaymentHolder> mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);

        mAppbarLayout = (AppBarLayout) findViewById(R.id.loan_detail_app_bar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.loan_detail_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.loan_detail_toolbar_layout);
        mToolbarLayout.setTitle("$0");

        mAppbarLayout.addOnOffsetChangedListener(this);
        mMaxScrollSize = mAppbarLayout.getTotalScrollRange();

        mContactImage = (CircleImageView) findViewById(R.id.loan_detail_contact_image);
        mContactName = (TextView) findViewById(R.id.loan_detail_contact_name);
        mUserImage = (CircleImageView) findViewById(R.id.loan_detail_user_image);
        mUserName = (TextView) findViewById(R.id.loan_detail_user_name);
        mList = (RecyclerView) findViewById(R.id.loan_payment_list);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.loan_detail_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showAddPaymentDialog();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(this);
        mUser = mAuth.getCurrentUser();

        if (mUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            GlideUtil.loadImage(mUser.getPhotoUrl(), mUserImage);
            mUserName.setText(mUser.getDisplayName());
        }

        // Get loan key from intent
        mLoanKey = getIntent().getStringExtra(EXTRA_LOAN_KEY);
        if (mLoanKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_LOAN_KEY");
        }

        mRef = FirebaseDatabase.getInstance().getReference();

        mLoanRef = mRef.child("loans").child(mLoanKey);
        mLoanRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mLoan = dataSnapshot.getValue(Loan.class);
                mContactUri = Uri.parse(mLoan.getPerson());

                getSupportLoaderManager().restartLoader(ContactDetailQuery.QUERY_ID, null, LoanDetailActivity.this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mPaymentsRef = mRef.child("payments");
        mLoanPaymentsRef = mRef.child("loan-payments").child(mLoanKey);
        mLoanPaymentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mTotalAmount = mLoan.getAmount();
                for (DataSnapshot paymentSnapshot: dataSnapshot.getChildren()) {
                    Payment payment = paymentSnapshot.getValue(Payment.class);
                    mTotalAmount -= payment.getAmount();
                }
                mToolbarLayout.setTitle(mTotalAmount.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mList = (RecyclerView) findViewById(R.id.loan_payment_list);

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(false);

        mList.setHasFixedSize(false);
        mList.setLayoutManager(mManager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Default Database rules do not allow unauthenticated reads, so we need to
        // sign in before attaching the RecyclerView adapter otherwise the Adapter will
        // not be able to read any data from the Database.
        if (!isSignedIn()) {
            signIn();
        } else {
            attachRecyclerViewAdapter();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mRecyclerViewAdapter != null) {
            mRecyclerViewAdapter.cleanup();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(this);
        }
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        //updateUI();
    }

    private void attachRecyclerViewAdapter() {
        Query lastFifty = mLoanPaymentsRef.limitToLast(50);
        mRecyclerViewAdapter = new FirebaseRecyclerAdapter<Payment, PaymentHolder>(
                Payment.class, R.layout.list_item_payment, PaymentHolder.class, lastFifty) {


            @Override
            public void populateViewHolder(PaymentHolder paymentView, Payment payment, int position) {
                paymentView.setAmount(payment.getAmount().toString());
                paymentView.setDate(payment.getPaymentDate());

                FirebaseUser currentUser = mAuth.getCurrentUser();
/*                if (currentUser != null && message.getUid().equals(currentUser.getUid())) {
                    messageView.setIsSender(true);
                } else {
                    messageView.setIsSender(false);
                }*/
            }
        };

        // Scroll to bottom on new messages
        mRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mManager.smoothScrollToPosition(mList, null, mRecyclerViewAdapter.getItemCount());
            }
        });

        mList.setAdapter(mRecyclerViewAdapter);
    }

    private void signIn() {
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                        if (task.isSuccessful()) {
                            Toast.makeText(LoanDetailActivity.this, "Signed In",
                                    Toast.LENGTH_SHORT).show();
                            attachRecyclerViewAdapter();
                        } else {
                            Toast.makeText(LoanDetailActivity.this, "Sign In Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public boolean isSignedIn() {
        return (mAuth.getCurrentUser() != null);
    }

    private void createNewPayment(Integer amount, String paymentDate, String notes, Object timestamp, String uid) {
        String key = mPaymentsRef.push().getKey();

        Payment payment = new Payment(amount, paymentDate, notes, timestamp, uid);
        Map<String, Object> paymentValues = payment.toMap();

        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/payments/" + key, paymentValues);
        childUpdates.put("/loan-payments/" + mLoanKey + "/" + key, paymentValues);

        mRef.updateChildren(childUpdates);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (mMaxScrollSize == 0)
            mMaxScrollSize = appBarLayout.getTotalScrollRange();

        int percentage = (Math.abs(verticalOffset)) * 100 / mMaxScrollSize;

        if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
            mIsAvatarShown = false;
            mContactImage.animate().scaleY(0).scaleX(0).setDuration(200).start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mContactImage.animate()
                    .scaleY(1).scaleX(1)
                    .start();
        }
    }

    public static class PaymentHolder extends RecyclerView.ViewHolder {
        View mView;

        public PaymentHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

/*         public void setIsSender(Boolean isSender) {
            FrameLayout left_arrow = (FrameLayout) mView.findViewById(R.id.left_arrow);
            FrameLayout right_arrow = (FrameLayout) mView.findViewById(R.id.right_arrow);
            RelativeLayout messageContainer = (RelativeLayout) mView.findViewById(R.id.message_container);
            LinearLayout message = (LinearLayout) mView.findViewById(R.id.message);

            int color;
            if (isSender) {
                color = ContextCompat.getColor(mView.getContext(), R.color.material_green_300);

                left_arrow.setVisibility(View.GONE);
                right_arrow.setVisibility(View.VISIBLE);
                messageContainer.setGravity(Gravity.END);
            } else {
                color = ContextCompat.getColor(mView.getContext(), R.color.material_gray_300);

                left_arrow.setVisibility(View.VISIBLE);
                right_arrow.setVisibility(View.GONE);
                messageContainer.setGravity(Gravity.START);
            }

            ((GradientDrawable) message.getBackground()).setColor(color);
            ((RotateDrawable) left_arrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
            ((RotateDrawable) right_arrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
        }*/

        public void setAmount(String name) {
            TextView field = (TextView) mView.findViewById(R.id.payment_amount);
            field.setText(name);
        }

        public void setDate(String date) {
            TextView field = (TextView) mView.findViewById(R.id.payment_date);
            field.setText(date);
        }
    }

    public void showAddPaymentDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddPaymentDialogFragment();
        dialog.show(getFragmentManager(), "AddPaymentDialogFragment");
    }

    public Uri getPhotoUri(Uri lookupUri) {
        ContentResolver contentResolver = getContentResolver();

        try {
            Cursor cursor = contentResolver
                    .query(ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID
                                    + "="
                                    + lookupUri.getLastPathSegment()
                                    + " AND "

                                    + ContactsContract.Data.MIMETYPE
                                    + "='"
                                    + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                    + "'", null, null);

            if (cursor != null) {
                if (!cursor.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return Uri.withAppendedPath(ContactsContract.Contacts.lookupContact(contentResolver, lookupUri),
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String amount, String notes) {
        createNewPayment(Integer.valueOf(amount), "2016-01-01", notes, ServerValue.TIMESTAMP, mUser.getUid());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            // Two main queries to load the required information
            case ContactDetailQuery.QUERY_ID:
                // This query loads main contact details, see
                // ContactDetailQuery for more information.
                Log.d("LOADER", mContactUri.toString());
                return new CursorLoader(this, mContactUri,
                        ContactDetailQuery.PROJECTION,
                        null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

        // If this fragment was cleared while the query was running
        // eg. from from a call like setContact(uri) then don't do
        // anything.
        if (mContactUri == null) {
            return;
        }

        switch (loader.getId()) {
            case ContactDetailQuery.QUERY_ID:
                // Moves to the first row in the Cursor
                if (data.moveToFirst()) {
                    // For the contact details query, fetches the contact display name.
                    // ContactDetailQuery.DISPLAY_NAME maps to the appropriate display
                    // name field based on OS version.
                    String contactName = data.getString(ContactDetailQuery.DISPLAY_NAME);
                    if (contactName != null) {
                        // In the two pane layout, there is a dedicated TextView
                        // that holds the contact name.
                        mContactName.setText(contactName);
                    }
                    GlideUtil.loadImage(Uri.withAppendedPath(ContactsContract.Contacts.lookupContact(getContentResolver(), mContactUri),
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY), mContactImage);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    /**
     * This interface defines constants used by contact retrieval queries.
     */
    public interface ContactDetailQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 1;

        // The query projection (columns to fetch from the provider)
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        };

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int DISPLAY_NAME = 1;
    }
}
