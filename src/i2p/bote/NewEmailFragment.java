package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import net.i2p.data.DataFormatException;

import i2p.bote.email.Attachment;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.util.BoteHelper;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

public class NewEmailFragment extends Fragment {
    private Callbacks mCallbacks = sDummyCallbacks;

    public interface Callbacks {
        public void onTaskFinished();
    }
    private static Callbacks sDummyCallbacks = new Callbacks() {
        public void onTaskFinished() {};
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks))
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    Spinner mSpinner;
    int mDefaultPos;
    MultiAutoCompleteTextView mRecipients;
    EditText mSubject;
    EditText mContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_email, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSpinner = (Spinner) view.findViewById(R.id.sender_spinner);
        IdentityAdapter identities = new IdentityAdapter(getActivity());
        mSpinner.setAdapter(identities);
        mSpinner.setSelection(mDefaultPos);

        mRecipients = (MultiAutoCompleteTextView) view.findViewById(R.id.recipients);
        mRecipients.setAdapter(null); // TODO: Implement
        mRecipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        mSubject = (EditText) view.findViewById(R.id.subject);
        mContent = (EditText) view.findViewById(R.id.message);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_email, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_send_email:
            sendEmail();
            mCallbacks.onTaskFinished();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void sendEmail() {
        Email email = new Email(I2PBote.getInstance().getConfiguration().getIncludeSentTime());
        try {
            // Set sender
            EmailIdentity sender = (EmailIdentity) mSpinner.getSelectedItem();
            InternetAddress ia = new InternetAddress(
                    sender == null ? "Anonymous" :
                        BoteHelper.getNameAndDestination(sender.getKey()));
            email.setFrom(ia);
            // We must continue to set "Sender:" even with only one mailbox
            // in "From:", which is against RFC 2822 but required for older
            // Bote versions to see a sender (and validate the signature).
            email.setSender(ia);

            // TODO: Implement properly
            email.addRecipient(Message.RecipientType.TO, ia);

            email.setSubject(mSubject.getText().toString(), "UTF-8");

            // Set the text and add attachments
            email.setContent(mContent.getText().toString(), (List<Attachment>) null);

            // Send the email
            I2PBote.getInstance().sendEmail(email);
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DataFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class IdentityAdapter extends ArrayAdapter<EmailIdentity> {
        private LayoutInflater mInflater;

        public IdentityAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            try {
                Collection<EmailIdentity> identities = I2PBote.getInstance().getIdentities().getAll();
                mDefaultPos = 0;
                for (EmailIdentity identity : identities) {
                    add(identity);
                    if (identity.isDefault())
                        mDefaultPos = getPosition(identity);
                }
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }

        @Override
        public EmailIdentity getItem(int position) {
            if (position > 0)
                return super.getItem(position - 1);
            else
                return null;
        }

        @Override
        public int getPosition(EmailIdentity item) {
            if (item != null)
                return super.getPosition(item) + 1;
            else
                return 0;
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null)
                v = mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            else
                v = convertView;

            setViewText(v, position);
            return v;
        }

        @Override
        public View getDropDownView (int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null)
                v = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            else
                v = convertView;

            setViewText(v, position);
            return v;
        }

        private void setViewText(View v, int position) {
            TextView text = (TextView) v.findViewById(android.R.id.text1);
            EmailIdentity identity = getItem(position);
            if (identity == null)
                text.setText("Anonymous");
            else
                text.setText(identity.getPublicName());
        }
    }
}
