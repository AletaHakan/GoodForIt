package com.gastonheaps.goodforit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gastonheaps.goodforit.model.Loan;
import com.gastonheaps.goodforit.model.Payment;
import com.gastonheaps.goodforit.util.GlideUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoanDetailActivity extends AppCompatActivity {

    private static final String TAG = "LoanDetailActivity";
    public static final String EXTRA_LOAN_KEY = "loan_key";

    private String mLoanKey;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mLoanReference;
    private DatabaseReference mPaymentsReference;
    private DatabaseReference mLoanPaymentsReference;
    private ValueEventListener mLoanListener;
    private PaymentAdapter mAdapter;

    private String mImageUrl;
    private CircleImageView mProfileImage;
    private TextView mAmount;
    private RecyclerView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.loan_detail_toolbar);
        setSupportActionBar(toolbar);

        mProfileImage = (CircleImageView) findViewById(R.id.loan_detail_profile_image);
        mAmount = (TextView) findViewById(R.id.loan_detail_amount);
        mList = (RecyclerView) findViewById(R.id.loan_detail_list);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.loan_detail_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mImageUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        // Get loan key from intent
        mLoanKey = getIntent().getStringExtra(EXTRA_LOAN_KEY);
        if (mLoanKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_LOAN_KEY");
        }

        mLoanReference = FirebaseDatabase.getInstance().getReference()
                .child("loans").child(mLoanKey);
        mPaymentsReference = FirebaseDatabase.getInstance().getReference()
                .child("payments");
        mLoanPaymentsReference = FirebaseDatabase.getInstance().getReference()
                .child("loan-payments").child(mLoanKey);

        mAdapter = new PaymentAdapter(this, mPaymentsReference);
        mList.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ValueEventListener loanListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Loan loan = dataSnapshot.getValue(Loan.class);

                GlideUtil.loadProfileIcon(mImageUrl, mProfileImage);
                mAmount.setText(String.valueOf(loan.getAmount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadLoan:onCancelled", databaseError.toException());
                // [START_EXCLUDE]
                Toast.makeText(LoanDetailActivity.this, "Failed to load post.",
                        Toast.LENGTH_SHORT).show();
                // [END_EXCLUDE]
            }
        };
        mLoanReference.addValueEventListener(loanListener);

        mLoanListener = loanListener;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove post value event listener
        if (mLoanListener != null) {
            mLoanReference.removeEventListener(mLoanListener);
        }
    }

    private static class PaymentViewHolder extends RecyclerView.ViewHolder {

        public TextView amountView;
        public TextView dateView;

        public PaymentViewHolder(View itemView) {
            super(itemView);

            amountView = (TextView) itemView.findViewById(R.id.payment_amount);
            dateView = (TextView) itemView.findViewById(R.id.payment_date);
        }
    }

    private static class PaymentAdapter extends RecyclerView.Adapter<PaymentViewHolder> {

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mPaymentIds = new ArrayList<>();
        private List<Payment> mPayments = new ArrayList<>();

        public PaymentAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                    // A new comment has been added, add it to the displayed list
                    Payment payment = dataSnapshot.getValue(Payment.class);

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mPaymentIds.add(dataSnapshot.getKey());
                    mPayments.add(payment);
                    notifyItemInserted(mPayments.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    Payment newPayment = dataSnapshot.getValue(Payment.class);
                    String paymentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int paymentIndex = mPaymentIds.indexOf(paymentKey);
                    if (paymentIndex > -1) {
                        // Replace with the new data
                        mPayments.set(paymentIndex, newPayment);

                        // Update the RecyclerView
                        notifyItemChanged(paymentIndex);
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + paymentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so remove it.
                    String paymentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int paymentIndex = mPaymentIds.indexOf(paymentKey);
                    if (paymentIndex > -1) {
                        // Remove data from the list
                        mPaymentIds.remove(paymentIndex);
                        mPayments.remove(paymentIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(paymentIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + paymentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                    // A comment has changed position, use the key to determine if we are
                    // displaying this comment and if so move it.
                    Payment movedPayment = dataSnapshot.getValue(Payment.class);
                    String paymentKey = dataSnapshot.getKey();

                    // ...
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load comments.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public PaymentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_payment, parent, false);
            return new PaymentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PaymentViewHolder holder, int position) {
            Payment payment = mPayments.get(position);
            holder.amountView.setText(String.valueOf(payment.amount));
            holder.dateView.setText(payment.paymentDate);
        }

        @Override
        public int getItemCount() {
            return mPayments.size();
        }

        public void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }
}
