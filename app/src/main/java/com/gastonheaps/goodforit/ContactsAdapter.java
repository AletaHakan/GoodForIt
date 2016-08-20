package com.gastonheaps.goodforit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.gastonheaps.goodforit.util.GlideUtil;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Gaston on 7/10/2016.
 */
public class ContactsAdapter extends CursorAdapter {
    private Context mContext;
    LayoutInflater mInflater;

    private final String[] PROJECTION = new String[]{
            //Data._ID,
            //Contacts.DISPLAY_NAME_PRIMARY,
            //Email.ADDRESS,
            //Phone.NUMBER
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.PHOTO_THUMBNAIL_URI
    };

    public static final int DISPLAY_NAME_PRIMARY_INDEX = 2;
    public static final int PHOTO_THUMBNAIL_URI_INDEX = 3;
    //public static final int EMAIL_ADDRESS_INDEX = 2;
    //public static final int PHONE_NUMBER_INDEX = 3;

    private final String SELECTION = /*'(' + */Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?"; //OR " + Phone.NUMBER + " LIKE ? OR " + Email.ADDRESS + " LIKE ?) AND (" +
            //Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "' OR " +
            //Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "')";


    public ContactsAdapter(Context context) {
        super(context, null, false);

        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    public class Contact {
        public String displayName;
        public CircleImageView image;
        public long id;
        public String lookupKey;
    }

    @Override
    public Object getItem(int position) {
        Cursor cursor = (Cursor) super.getItem(position);
        Contact contact = new Contact();

        String imageUri = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA_COLUMN);

        contact.id = cursor.getLong(ContactsQuery.ID_COLUMN);
        contact.lookupKey = cursor.getString(ContactsQuery.LOOKUP_KEY_COLUMN);
        contact.displayName = cursor.getString(ContactsQuery.DISPLAY_NAME_COLUMN);

/*        Uri thumbUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && imageUri != null) {
            thumbUri = Uri.parse(imageUri);
        } else {
            final Uri contactUri = Uri.withAppendedPath(Contacts.CONTENT_URI, imageUri);
            thumbUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        }*/
        //GlideUtil.loadImage(thumbUri, contact.image);

        return contact;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        Log.d("ADAPTER:",SELECTION);
        if (constraint == null || constraint.length() == 0) {
            return mContext.getContentResolver().query(
                    ContactsQuery.CONTENT_URI,
                    ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION,
                    null,
                    ContactsQuery.SORT_ORDER);
        }

        return mContext.getContentResolver().query(
                Uri.withAppendedPath(ContactsQuery.FILTER_URI, constraint.toString()),
                ContactsQuery.PROJECTION,
                ContactsQuery.SELECTION,
                null,
                ContactsQuery.SORT_ORDER);
    }



    private class ViewHolder {
        public TextView text;
        public CircleImageView image;
        //public TextView mEmailAddress;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
/*        ViewHolder holder = new ViewHolder();
        View view = LayoutInflater.from(context).inflate(R.layout.contact_list_item*//*android.R.layout.simple_list_item_2*//*, parent, false);
        holder.mDisplayName = (TextView) view.findViewById(R.id.contact_item_display_name);
        holder.mPhoto = (CircleImageView) view.findViewById(R.id.contact_item_photo);
        //holder.mEmailAddress = (TextView) view.findViewById(android.R.id.text2);
        view.setTag(holder);
        return view;*/

        final View dropdownView = mInflater.inflate(R.layout.contact_list_item,
                parent, false);

        ViewHolder holder = new ViewHolder();
        holder.text = (TextView) dropdownView.findViewById(R.id.contact_item_display_name);
        holder.image = (CircleImageView) dropdownView.findViewById(R.id.contact_item_photo);

        dropdownView.setTag(holder);

        return dropdownView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String displayName = cursor.getString(DISPLAY_NAME_PRIMARY_INDEX);
        final String imageUri = cursor.getString(PHOTO_THUMBNAIL_URI_INDEX);
        //String emailAddress = cursor.getString(EMAIL_ADDRESS_INDEX);
        holder.text.setText(displayName);

/*        Uri thumbUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && imageUri != null) {
            thumbUri = Uri.parse(imageUri);
        } else {
            final Uri contactUri = Uri.withAppendedPath(Contacts.CONTENT_URI, imageUri);
            thumbUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        }
        GlideUtil.loadImage(thumbUri, holder.image);*/
        Log.d("ADAPTER:", imageUri.toString());
        //holder.mEmailAddress.setText(emailAddress);
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     */
    private static interface ContactsQuery {

        // A content URI for the Contacts table
        final static Uri CONTENT_URI = Contacts.CONTENT_URI;

        // The search/filter query Uri
        final static Uri FILTER_URI = Contacts.CONTENT_FILTER_URI;

        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name, are linked to visible groups,
        // and have a phone number.  Notice that the search on the string provided by the user
        // is implemented by appending the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        final static String SELECTION =
                (Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + Contacts.IN_VISIBLE_GROUP + "=1 AND " +
                        Contacts.HAS_PHONE_NUMBER + "=1";

        // The desired sort order for the returned Cursor. Not sure what apps like Mms use, but
        // TIMES_CONTACTED seems to be fairly useful for this purpose.
        final static String SORT_ORDER = Contacts.TIMES_CONTACTED + " DESC";

        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {

                // The contact's row id
                Contacts._ID,

                // A pointer to the contact that is guaranteed to be more permanent than _ID. Given
                // a contact's current _ID value and LOOKUP_KEY, the Contacts Provider can generate
                // a "permanent" contact URI.
                Contacts.LOOKUP_KEY,

                // In platform version 3.0 and later, the Contacts table contains
                // DISPLAY_NAME_PRIMARY, which either contains the contact's displayable name or
                // some other useful identifier such as an email address. This column isn't
                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
                // instead.
                Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME,

                // In Android 3.0 and later, the thumbnail image is pointed to by
                // PHOTO_THUMBNAIL_URI. In earlier versions, there is no direct pointer; instead,
                // you generate the pointer from the contact's ID value and constants defined in
                // android.provider.ContactsContract.Contacts.
                Utils.hasHoneycomb() ? Contacts.PHOTO_THUMBNAIL_URI : Contacts._ID
        };

        // The query column numbers which map to each value in the projection
        final static int ID_COLUMN = 0;
        final static int LOOKUP_KEY_COLUMN = 1;
        final static int DISPLAY_NAME_COLUMN = 2;
        final static int PHOTO_THUMBNAIL_DATA_COLUMN = 3;
    }

    private static class Utils {

        // Prevents instantiation.
        private Utils() {}

        /**
         * Uses static final constants to detect if the device's platform version is Honeycomb or
         * later.
         */
        public static boolean hasHoneycomb() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        }
    }
}
