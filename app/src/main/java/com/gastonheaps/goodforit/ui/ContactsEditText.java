package com.gastonheaps.goodforit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gastonheaps.goodforit.R;
import com.gastonheaps.goodforit.util.GlideUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Gaston on 7/24/2016.
 */
public class ContactsEditText extends AutoCompleteTextView {

    private DatabaseReference mDatabase;

    private ContactsAdapter mAdapter;

    public ContactsEditText(Context context) {
        super(context);
        init(context);
    }

    public ContactsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContactsEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set adapter
        mAdapter = new ContactsAdapter(context);
        setAdapter(mAdapter);

        // Pop up suggestions after 1 character is typed.
        setThreshold(1);
    }

    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        return ((Contact) selectedItem).displayName;
    }

    public class Contact {
        public long id;
        public long contactId;
        public String displayName;
        public CircleImageView image;
        public String contactInformation;
        public String contactType;
        public boolean registered;
    }

    private class ContactsAdapter extends CursorAdapter {
        Context mContext;
        LayoutInflater mInflater;

        public ContactsAdapter(Context context) {
            super(context, null, false);

            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public Object getItem(int position) {
            Cursor cursor = (Cursor) super.getItem(position);
            Contact contact = new Contact();

            String imageUri = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA_COLUMN);

            contact.id = cursor.getLong(ContactsQuery.ID_COLUMN);
            contact.contactId = cursor.getLong(ContactsQuery.CONTACT_ID_COLUMN);
            contact.displayName = cursor.getString(ContactsQuery.DISPLAY_NAME_PRIMARY_COLUMN);
            contact.contactInformation = cursor.getString(ContactsQuery.CONTACT_INFORMATION_COLUMN);
            contact.contactType = cursor.getString(ContactsQuery.CONTACT_TYPE_COLUMN);

            Uri thumbUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && imageUri != null) {
                thumbUri = Uri.parse(imageUri);
            } else {
                final Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, imageUri);
                thumbUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            }
            //GlideUtil.loadImage(thumbUri, contact.image);

            Log.d("CONTACT", String.valueOf(contact.registered));
            return contact;
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (constraint == null || constraint.length() == 0) {
                return mContext.getContentResolver().query(
                        ContactsQuery.CONTENT_URI,
                        ContactsQuery.PROJECTION,
                        ContactsQuery.SELECTION,
                        null,
                        ContactsQuery.SORT_ORDER);
            }

            return mContext.getContentResolver().query(
                    ContactsQuery.CONTENT_URI,
                    ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION_FILTERED,
                    new String[]{"%" + constraint.toString() + "%"},
                    ContactsQuery.SORT_ORDER);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View dropdownView = mInflater.inflate(R.layout.contact_list_item,
                    parent, false);

            ViewHolder holder = new ViewHolder();
            holder.registeredImage = (ImageView) dropdownView.findViewById(R.id.contact_item_registered_indicator);
            holder.image = (CircleImageView) dropdownView.findViewById(R.id.contact_item_photo);
            holder.nameText = (TextView) dropdownView.findViewById(R.id.contact_item_display_name);
            holder.contactInformationText = (TextView) dropdownView.findViewById(R.id.contact_item_contact_information);

            dropdownView.setTag(holder);

            return dropdownView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();

            final String imageUri = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA_COLUMN);
            final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME_PRIMARY_COLUMN);
            final String contactInformation = cursor.getString(ContactsQuery.CONTACT_INFORMATION_COLUMN);
            final String contactType = cursor.getString(ContactsQuery.CONTACT_TYPE_COLUMN);

            holder.nameText.setText(displayName);
            holder.contactInformationText.setText(contactInformation);

            mDatabase.child("users").orderByChild("email").equalTo(contactInformation)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                holder.registeredImage.setVisibility(View.VISIBLE);
                            } else {
                                holder.registeredImage.setVisibility(View.INVISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

            Uri thumbUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && imageUri != null) {
                thumbUri = Uri.parse(imageUri);
            } else {
                final Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, imageUri);
                thumbUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            }
            GlideUtil.loadImage(thumbUri, holder.image);
            //holder.mEmailAddress.setText(emailAddress);
        }
    }

    private static class ViewHolder {

        public CircleImageView image;
        public ImageView registeredImage;
        public TextView nameText;
        public TextView contactInformationText;
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     */
    private static interface ContactsQuery {

        // A content URI for the Contacts table
        final static Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;

        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name, are linked to visible groups,
        // and have a phone number.  Notice that the search on the string provided by the user
        // is implemented by appending the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        final static String SELECTION =
                (Utils.hasHoneycomb() ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME) + "<>'' AND (" +
                        ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "' OR " +
                        ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "')";

        final static String SELECTION_FILTERED =
                (Utils.hasHoneycomb() ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME) + "<>'' AND (" +
                        ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "' OR " +
                        ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') AND " +
                        (Utils.hasHoneycomb() ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME) + " LIKE ?";

        // The desired sort order for the returned Cursor. Not sure what apps like Mms use, but
        // TIMES_CONTACTED seems to be fairly useful for this purpose.
        final static String SORT_ORDER = ContactsContract.Data.CONTACT_ID + " DESC";

        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {

                // The id
                ContactsContract.Data._ID,

                // The row's contactId
                ContactsContract.Data.CONTACT_ID,

                // In platform version 3.0 and later, the Contacts table contains
                // DISPLAY_NAME_PRIMARY, which either contains the contact's displayable name or
                // some other useful identifier such as an email address. This column isn't
                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
                // instead.
                Utils.hasHoneycomb() ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME,

                // In Android 3.0 and later, the thumbnail image is pointed to by
                // PHOTO_THUMBNAIL_URI. In earlier versions, there is no direct pointer; instead,
                // you generate the pointer from the contact's ID value and constants defined in
                // android.provider.ContactsContract.Contacts.
                Utils.hasHoneycomb() ? ContactsContract.Data.PHOTO_THUMBNAIL_URI : ContactsContract.Contacts._ID,

                ContactsContract.Data.DATA1,
                ContactsContract.Data.MIMETYPE
        };

        // The query column numbers which map to each value in the projection
        final static int ID_COLUMN = 0;
        final static int CONTACT_ID_COLUMN = 1;
        final static int DISPLAY_NAME_PRIMARY_COLUMN = 2;
        final static int PHOTO_THUMBNAIL_DATA_COLUMN = 3;
        final static int CONTACT_INFORMATION_COLUMN = 4;
        final static int CONTACT_TYPE_COLUMN = 5;
    }

    private static class Utils {

        // Prevents instantiation.
        private Utils() {
        }

        /**
         * Uses static final constants to detect if the device's platform version is Honeycomb or
         * later.
         */
        public static boolean hasHoneycomb() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        }
    }
}
