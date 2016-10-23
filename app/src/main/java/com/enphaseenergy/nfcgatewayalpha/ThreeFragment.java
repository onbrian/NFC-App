package com.enphaseenergy.nfcgatewayalpha;

import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

public class ThreeFragment extends Fragment
{
    private final static String TAG = "INFO_FRAGMENT";
    private TextView noTagDetected;
    private LinearLayout tagDetected;
    private TextView timeText;
    private TextView uidText;
    private TextView techlistView;

    public ThreeFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_three, container, false);

        noTagDetected = (TextView) view.findViewById(R.id.no_tag_detected);
        tagDetected = (LinearLayout) view.findViewById(R.id.tag_detected);
        timeText = (TextView) view.findViewById(R.id.info_time);
        uidText = (TextView) view.findViewById(R.id.uid);
        techlistView = (TextView) view.findViewById(R.id.techlist);

        // set visibilities
        tagDetected.setVisibility(View.GONE);

        return view;
    }

    // construct hex string UID for tagV from byte array
    public static String tagFromBytes(byte[] tagId)
    {
        StringBuilder uid = new StringBuilder();
        for (int i = tagId.length - 1; i >= 0; --i)
        {
            uid.append(String.format("%02x", tagId[i]));
        }
        return uid.toString();
    }

    public void displayInfo(Tag tag)
    {
        // hide default message
        noTagDetected.setVisibility(View.GONE);

        // show info layout
        tagDetected.setVisibility(View.VISIBLE);

        // set time stamp
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        timeText.setText(currentDateTimeString);

        // set uid
        uidText.setText(tagFromBytes(tag.getId()));

        // set tech list
        StringBuilder tech = new StringBuilder();
        // tech list
        String[] techlist = tag.getTechList();
        for (int i = 0; i < techlist.length; i++)
        {
            tech.append(techlist[i]);
            tech.append(System.getProperty("line.separator"));
        }
        techlistView.setText(tech.toString());

        // try to unlock
        //byte[] sector_address = {0, 0};
        //byte lock_byte = 0;
        //byte[] resp = NFCV_Security.SendLockSectorCommand(tag, false, false, sector_address, lock_byte);
        //Toast.makeText(getActivity(), "hi", Toast.LENGTH_SHORT).show();
        //Toast.makeText(getActivity(), Arrays.toString(resp), Toast.LENGTH_SHORT).show();

        /*
        byte[] password = {0, 0, 0, 0};
        byte pass_num = 1;
        byte[] resp = NFCV_Security.SendPresentPasswordCommand(tag, pass_num, password);
        Log.e(TAG, Arrays.toString(resp));
        */

        //byte[] startAddress = {0, 0};
        //byte num_blocks = 10;
        //resp = NFCV_Security.SendReadMultipleBlockCommand(tag, startAddress, num_blocks, false, false);

        //resp = NFCV_Security.readSector(tag, 3);
        /*
        resp = NFCV_Security.readProtectedSector(tag);
        Log.e(TAG, Arrays.toString(resp));
        byte[] stripped_resp = new byte[resp.length - 1];
        System.arraycopy(resp, 1, stripped_resp, 0, stripped_resp.length);
        String file_string = new String(stripped_resp);
        Log.e(TAG, file_string);
        */



        // 3
        /*

        byte[] write_data = {(byte) 0x00, (byte) 0xF4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte start_address = 1;
        resp = NFCV_Security.SendWriteMultipleBlockCommand(tag, start_address, write_data);

        resp = NFCV_Security.readSector(tag, 0);
        Log.e(TAG, String.valueOf(resp.length));
        Log.e(TAG, Arrays.toString(resp));


        resp = NFCV_Security.readSector(tag, 1);
        Log.e(TAG, String.valueOf(resp.length));
        Log.e(TAG, Arrays.toString(resp));

        resp = NFCV_Security.readSector(tag, 2);
        Log.e(TAG, String.valueOf(resp.length));
        Log.e(TAG, Arrays.toString(resp));

        resp = NFCV_Security.readSector(tag, 3);
        Log.e(TAG, String.valueOf(resp.length));
        Log.e(TAG, Arrays.toString(resp));
        */

        return;
    }
}
