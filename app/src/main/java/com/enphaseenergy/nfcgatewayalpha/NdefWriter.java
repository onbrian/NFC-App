package com.enphaseenergy.nfcgatewayalpha;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Created by bon on 9/13/16.
 */
public class NdefWriter
{
    public static NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8)
    {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                                           NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }

    public static void writeTag(Activity activity, String TAG, NdefMessage message, Tag tag)
    {
        try
        {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null)
            {
                ndef.connect();
                ndef.writeNdefMessage(message);
                ndef.close();
                Toast.makeText(activity, "Write successful!", Toast.LENGTH_LONG).show();
            }
            else
            {
                NFC_Helper.reportError(activity, TAG, "NDEF is null -- failed to write to tag.", null);
            }
        }
        catch (Exception e)
        {
            NFC_Helper.reportError(activity, TAG, "Failed to write tag.", e);
        }
    }
}