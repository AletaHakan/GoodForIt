package com.gastonheaps.goodforit;


import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.gastonheaps.goodforit.model.Loan;
import com.gastonheaps.goodforit.viewholder.LoanViewHolder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoanListFragment extends Fragment {
    private DatabaseReference mDatabase;

    private RecyclerView mLoanRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<Loan, LoanViewHolder> mFirebaseAdapter;

    public LoanListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_loan_list, container, false);

        // [START create_database_reference]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // [END create_database_reference]

        mLoanRecyclerView = (RecyclerView) rootView.findViewById(R.id.loan_list);
        mLoanRecyclerView.setHasFixedSize(true);

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);

        mLoanRecyclerView.setLayoutManager(mLinearLayoutManager);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Loan, LoanViewHolder>(
                Loan.class,
                R.layout.item_loan,
                LoanViewHolder.class,
                mDatabase.child("loans")) {

            @Override
            protected void populateViewHolder(LoanViewHolder viewHolder, final Loan loan, int position) {
                final DatabaseReference loanRef = getRef(position);

                // Set click listener for the whole post view
                final String loanKey = loanRef.getKey();
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Launch PostDetailActivity
                        Intent intent = new Intent(getActivity(), LoanDetailActivity.class);
                        intent.putExtra(LoanDetailActivity.EXTRA_LOAN_KEY, loanKey);
                        startActivity(intent);
                    }
                });

                // Bind Payment to ViewHolder, setting OnClickListener for the star button
                viewHolder.bindToTransaction(loan, new View.OnClickListener() {
                    @Override
                    public void onClick(View starView) {
                        // Need to write to both places the post is stored
                        DatabaseReference globalPostRef = mDatabase.child("loans").child(loanRef.getKey());
                        DatabaseReference userPostRef = mDatabase.child("user-loans").child(loan.uid).child(loanRef.getKey());
                    }
                });
            }
        };

        mLoanRecyclerView.setAdapter(mFirebaseAdapter);

        return rootView;
    }
}
