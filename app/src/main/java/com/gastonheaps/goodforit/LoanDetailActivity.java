package com.gastonheaps.goodforit;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.gastonheaps.goodforit.model.Payment;
import com.gastonheaps.goodforit.ui.AddPaymentDialogFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoanDetailActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener,
        AddPaymentDialogFragment.NoticeDialogListener{

    private static final String TAG = "LoanDetailActivity";
    public static final String EXTRA_LOAN_KEY = "loan_key";

    private String mLoanKey;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mRef;

    private DatabaseReference mLoanRef;
    private DatabaseReference mPaymentsRef;
    private DatabaseReference mLoanPaymentsRef;

    private String mImageUrl;
    private CircleImageView mProfileImage;
    private RecyclerView mList;
    private LinearLayoutManager mManager;
    private FirebaseRecyclerAdapter<Payment, PaymentHolder> mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.loan_detail_toolbar);
        setSupportActionBar(toolbar);

        mProfileImage = (CircleImageView) findViewById(R.id.loan_detail_profile_image);
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
            mImageUrl = mUser.getPhotoUrl().toString();
        }

        // Get loan key from intent
        mLoanKey = getIntent().getStringExtra(EXTRA_LOAN_KEY);
        if (mLoanKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_LOAN_KEY");
        }

        mRef = FirebaseDatabase.getInstance().getReference();

        mLoanRef = mRef.child("loans").child(mLoanKey);
        mPaymentsRef = mRef.child("payments");
        mLoanPaymentsRef = mRef.child("loan-payments").child(mLoanKey);
        mLoanPaymentsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Payment newPayment = dataSnapshot.getValue(Payment.class);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

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

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String amount, String notes) {
        createNewPayment(Integer.valueOf(amount), "2016-01-01", notes, ServerValue.TIMESTAMP, mUser.getUid());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }
}
