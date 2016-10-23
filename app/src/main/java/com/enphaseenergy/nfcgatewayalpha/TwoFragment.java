package com.enphaseenergy.nfcgatewayalpha;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Locale;

public class TwoFragment extends Fragment
{
    private final static String TAG = "WRITE_FRAGMENT";
    private Switch writeSwitch;
    private Spinner securitySpinner;
    private EditText usernameField;
    private EditText passwordField;
    private EditText miscField;

    /* security variables */
    private View securityFields;
    private EditText passwordByte0;
    private EditText passwordByte1;
    private EditText passwordByte2;
    private EditText passwordByte3;
    private RadioGroup passwordNum;

    public TwoFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    private void setupSecuritySpinner(Spinner spinner)
    {
        // Create an ArrayAdapter using the string array and a default spinner layout
        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
        //        R.array.security_spinner_array, R.layout.security_spinner_item);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.security_spinner_item,
                NFCV_Security.SECURITY_SPINNER_OPTIONS);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        final Spinner final_spinner = spinner;

        final_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            public void onItemSelected(AdapterView<?> arg0, View view, int position, long id)
            {
                //int item = final_spinner.getSelectedItemPosition();
                String sector_type = final_spinner.getSelectedItem().toString();
                if (sector_type == NFCV_Security.PROTECTED_SECTOR)
                {
                    securityFields.setVisibility(View.VISIBLE);
                }
                else
                {
                    assert(sector_type == NFCV_Security.UNPROTECTED_SECTOR);
                    securityFields.setVisibility(View.GONE);
                }

            }
            public void onNothingSelected(AdapterView<?> arg0) { }
        });
    }

    private void setupSecurityVariables(View view)
    {
        securityFields = (View) view.findViewById(R.id.security_fields);
        passwordByte0 = (EditText) view.findViewById(R.id.password_byte_0);
        passwordByte1 = (EditText) view.findViewById(R.id.password_byte_1);
        passwordByte2 = (EditText) view.findViewById(R.id.password_byte_2);
        passwordByte3 = (EditText) view.findViewById(R.id.password_byte_3);

        passwordNum = (RadioGroup) view.findViewById(R.id.which_password_radio);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_two, container, false);
        writeSwitch = (Switch) view.findViewById(R.id.write_switch);
        securitySpinner = (Spinner) view.findViewById(R.id.security_spinner);
        usernameField = (EditText) view.findViewById(R.id.username_form);
        passwordField = (EditText) view.findViewById(R.id.password_form);
        miscField = (EditText) view.findViewById(R.id.misc_form);

        setupSecurityVariables(view);
        setupSecuritySpinner(securitySpinner);

        return view;
    }

    private static byte[] get_entered_password()
    {
        return new byte[]{0, 0, 0, 0};
    }

    private byte get_password_number()
    {
        int selectedId = passwordNum.getCheckedRadioButtonId();
        RadioButton chosen_pass = (RadioButton) getView().findViewById(selectedId);
        int num = Integer.parseInt(chosen_pass.getText().toString());
        return (byte) num;
    }

    public void writeNFCTag(Tag tag)
    {
        if (!writeSwitch.isChecked())
        {
            return;
        }

        // extract records from fields + combine to make message
        NdefRecord username_record = NdefWriter.createTextRecord(usernameField.getText().toString(),
                Locale.US, true);
        NdefRecord password_record = NdefWriter.createTextRecord(passwordField.getText().toString(),
                Locale.US, true);
        NdefRecord misc_record = NdefWriter.createTextRecord(miscField.getText().toString(),
                Locale.US, true);

        NdefRecord[] credential_records = {username_record, password_record, misc_record};
        NdefMessage ndef_message = new NdefMessage(credential_records);

        // write message to tag
        String sector_type = securitySpinner.getSelectedItem().toString();
        byte[] response = null;
        if (sector_type == NFCV_Security.PROTECTED_SECTOR)
        {
            byte[] password = get_entered_password();
            byte[] incorrect_password = password;
            byte password_number = get_password_number();

            // present password
            response = NFCV_Security.SendPresentPasswordCommand(tag, password_number, password);
            NFCV_Security.reportCommandError(getActivity(), NFCV_Security.PRESENT_PASSWORD_COMMAND,
                    response);

            // get wrong password if necessary
            if (response[0] == NFCV_Security.SUCCESS_BYTE)
            {
                incorrect_password = password;
                incorrect_password[0]++;
            }

            // write
            response = NFCV_Security.writeProtectedSectors(tag, ndef_message.toByteArray());
            NFCV_Security.reportCommandError(getActivity(), NFCV_Security.WRITE_BLOCKS_COMMAND,
                    response);

            // lock sector again
            response = NFCV_Security.SendPresentPasswordCommand(tag, password_number, password);
            NFCV_Security.reportCommandError(getActivity(), NFCV_Security.PRESENT_PASSWORD_COMMAND,
                    response);


        }
        else
        {
            assert(sector_type == NFCV_Security.UNPROTECTED_SECTOR);
            response = NFCV_Security.writeUnprotectedSectors(tag, ndef_message.toByteArray());
            NFCV_Security.reportCommandError(getActivity(), NFCV_Security.WRITE_BLOCKS_COMMAND, response);
        }

        //Toast.makeText(getActivity(), "Write successful!", Toast.LENGTH_LONG).show();
        //NdefWriter.writeTag(getActivity(), TAG, ndef_message, tag);
        return;
    }


}
