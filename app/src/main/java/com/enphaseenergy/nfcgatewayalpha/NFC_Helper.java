package com.enphaseenergy.nfcgatewayalpha;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by bon on 9/12/16.
 */
public class NFC_Helper
{
    public static final String MIME_TEXT_PLAIN = "text/plain";

    public static void alertDeviceNFCStatus(Activity activity, String tag, NfcAdapter mNfcAdapter)
    {

        if (mNfcAdapter == null)
        {
            // Stop here, we definitely need NFC
            reportError(activity, tag, "This device doesn't support NFC.", null);
        }
        else if (!mNfcAdapter.isEnabled())
        {
            reportError(activity, tag, "NFC is disabled.", null);
        }
        return;
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, String tag, NfcAdapter adapter)
    {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[2];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        filters[1] = new IntentFilter();
        filters[1].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[1].addCategory(Intent.CATEGORY_DEFAULT);
        try
        {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            reportError(activity, tag, "Check your MIME type", e);
        }
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter)
    {
        adapter.disableForegroundDispatch(activity);
    }

    public static boolean isValidNDEFIntent(Activity activity, String tag, Intent intent)
    {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {
            Toast.makeText(activity, "NFC Tag Detected", Toast.LENGTH_SHORT).show();
            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type))
            {
                return true;
            }
            else
            {
                reportError(activity, tag, "Wrong MIME type: " + type, null);
                return false;
            }
        }
        else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action))
        {
            Toast.makeText(activity, "NFC Tag Detected", Toast.LENGTH_SHORT).show();
            // do error checking for enphase specific formatted tag
            return true;
        }
        return false;
    }

    public static void reportError(Context context, String tag, String message, Throwable tr)
    {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (tr == null)
        {
            Log.e(tag, message);
        }
        else
        {
            Log.e(tag, message, tr);
        }
        return;
    }
}
