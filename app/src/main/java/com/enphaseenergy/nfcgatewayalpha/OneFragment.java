package com.enphaseenergy.nfcgatewayalpha;

import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class OneFragment extends Fragment
{
    private final static String TAG = "READ_FRAGMENT";

    private Spinner securitySpinner;
    private Switch readSwitch;
    private TextView timeText;
    private ListView recordsList;
    public OneFragment() {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_one, container, false);
        securitySpinner = (Spinner) view.findViewById(R.id.security_spinner);
        readSwitch = (Switch) view.findViewById(R.id.read_switch);
        timeText = (TextView) view.findViewById(R.id.time_read);
        recordsList = (ListView) view.findViewById(R.id.records_list);

        setupSecuritySpinner(securitySpinner);

        //readText.setText("hey this was set in function");
        return view;
    }

    private RecordsListAdapter getRecordsListAdapter(String[] payloads)
    {
        RecordsListAdapter adapter = new RecordsListAdapter(getActivity());
        for (int i = 0; i < payloads.length; i++)
        {
            String header = "Record " + String.valueOf(i);
            RecordsListAdapter.RecordItem record = new RecordsListAdapter.RecordItem(header, payloads[i]);
            adapter.addRecordItem(record);
        }
        return adapter;
    }

    public void readNFCTag(Tag tag)
    {
        if (!readSwitch.isChecked())
        {
            return;
        }


        String sector_type = securitySpinner.getSelectedItem().toString();
        boolean isProtected = sector_type == NFCV_Security.PROTECTED_SECTOR;
        byte[] retrieved_data = null;
        if (isProtected)
        {
            retrieved_data = NFCV_Security.readProtectedSector(tag);
        }
        else
        {
            assert(sector_type == NFCV_Security.UNPROTECTED_SECTOR);
            retrieved_data = NFCV_Security.readUnprotectedSectors(tag);
        }

        boolean success = NFCV_Security.reportCommandError(getActivity(), NFCV_Security.READ_BLOCKS_COMMAND,
                retrieved_data);

        if (!success)
        {
            return;
        }

        // strip off success bit
        byte[] retrieved_data_no_report_bit = new byte[retrieved_data.length - 1];
        System.arraycopy(retrieved_data, 1, retrieved_data_no_report_bit, 0,
                retrieved_data_no_report_bit.length);

        String[] payloads = NFCV_Security.raw_data_to_payloads(getActivity(), isProtected,
                retrieved_data_no_report_bit);

        if (payloads == null)
        {
            return;
        }

        //String retrieved_data_string = new String(retrieved_data);
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        timeText.setText("Read on " + currentDateTimeString);

        RecordsListAdapter adapter = new RecordsListAdapter(getActivity());

        for (int i = 0; i < payloads.length; i++)
        {
            String header = "Record " + String.valueOf(i);
            RecordsListAdapter.RecordItem record = new RecordsListAdapter.RecordItem(header, payloads[i]);
            adapter.addRecordItem(record);
        }
        this.recordsList.setAdapter(adapter);
        return;

        //Toast.makeText(getActivity(), "Read successful!", Toast.LENGTH_SHORT).show();

        //new NdefReaderTask(getActivity(), this.TAG, timeText, recordsList).execute(tag);
    }
}
