package com.gastonheaps.goodforit.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gastonheaps.goodforit.R;
import com.gastonheaps.goodforit.model.Loan;

import java.text.DateFormat;

/**
 * Created by Gaston on 6/5/2016.
 */
public class LoanViewHolder extends RecyclerView.ViewHolder{
    public TextView profileView;
    public TextView amountView;
    public TextView dateView;
    public TextView notesView;

    public LoanViewHolder(View itemView) {
        super(itemView);

        profileView = (TextView) itemView.findViewById(R.id.loan_person);
        amountView = (TextView) itemView.findViewById(R.id.loan_amount);
        dateView = (TextView) itemView.findViewById(R.id.loan_date);
        notesView = (TextView) itemView.findViewById(R.id.loan_notes);
    }

    public void bindToTransaction(Loan loan, View.OnClickListener starClickListener) {
        profileView.setText(loan.uid);
        amountView.setText(String.valueOf(loan.amount));
        dateView.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(loan.getDateCalendar().getTime()));
        notesView.setText(loan.notes);

        //starView.setOnClickListener(starClickListener);
    }
}
