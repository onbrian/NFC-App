package com.enphaseenergy.nfcgatewayalpha;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by bon on 9/13/16.
 */
public class NdefReaderTask extends AsyncTask<Tag, Void, String[]>
{
    private String TAG;
    private Activity context;
    private TextView timeReadView;
    private ListView recordsListView;

    public NdefReaderTask(Activity activity, String TAG, TextView timeReadView, ListView recordsListView)
    {
        super();
        this.TAG = TAG;
        this.context = activity;
        this.timeReadView = timeReadView;
        this.recordsListView = recordsListView;
    }

    @Override
    protected String[] doInBackground(Tag... params)
    {
        Tag tag = params[0];

        Ndef ndef = Ndef.get(tag);
        if (ndef == null)
        {
            // NDEF is not supported by this Tag.
            return null;
        }

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();
        String[] payloads = new String[records.length];
        Log.e(TAG, "this is a test");

        for (int i = 0; i < records.length; i++)
        {
            NdefRecord ndefRecord = records[i];
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT))
            {
                try
                {
                    payloads[i] = readText(ndefRecord);
                }
                catch (UnsupportedEncodingException e)
                {
                    NFC_Helper.reportError(this.context, this.TAG, "Message contains unsupported encoding.", e);
                    return null;
                }
            }
            else
            {
                NFC_Helper.reportError(this.context, this.TAG, "Message is not text type.", null);
                return null;
            }
        }

        return payloads;
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException
    {
        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // e.g. "en"

        // Get the Text
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    @Override
    protected void onPostExecute(String[] payloads)
    {
        if (payloads == null)
        {
            return;
            //mTextView.setText("Read content: " + result);
        }

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        timeReadView.setText("Read on " + currentDateTimeString);
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.context, R.layout.records_list_layout, payloads);
        RecordsListAdapter adapter = new RecordsListAdapter(this.context);

        for (int i = 0; i < payloads.length; i++)
        {
            String header = "Record " + String.valueOf(i);
            RecordsListAdapter.RecordItem record = new RecordsListAdapter.RecordItem(header, payloads[i]);
            adapter.addRecordItem(record);
        }
        this.recordsListView.setAdapter(adapter);
        Toast.makeText(this.context, "Read successful!", Toast.LENGTH_SHORT).show();
    }
}