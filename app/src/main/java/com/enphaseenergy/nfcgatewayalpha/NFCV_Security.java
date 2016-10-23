package com.enphaseenergy.nfcgatewayalpha;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by bon on 9/16/16.
 */
public class NFCV_Security
{
    public final static String PROTECTED_SECTOR = "Protected Sector";
    public final static String UNPROTECTED_SECTOR = "Unprotected Sector";
    public final static String[] SECURITY_SPINNER_OPTIONS = {PROTECTED_SECTOR, UNPROTECTED_SECTOR};

    // available commands

    public final static String PRESENT_PASSWORD_COMMAND = "PRESENT_PASSWORD";
    public final static String READ_BLOCKS_COMMAND      = "READ_MULTIPLE_BLOCKS";
    public final static String WRITE_BLOCK_COMMAND      = "WRITE_SINGLE_BLOCK";
    public final static String WRITE_BLOCKS_COMMAND      = "WRITE_MULTIPLE_BLOCKS";
    public final static String LOCK_SECTOR_COMMAND      = "LOCK_SECTOR";

    // maximum number of tries to send command to tag

    private final static int MAX_TRANSCEIVE_ATTEMPTS = 3;
    private final static int MAX_WRAPPER_TRIES = 3;

    // tag error values

    public final static byte ERROR_BYTE   = (byte) 0x01;
    public final static byte SUCCESS_BYTE = (byte) 0x00;

    // memory constants

    private final static int NUM_SECTORS                 = 4;
    private final static int NUM_UNPROTECTED_SECTORS     = 3;
    // block offset * 4 (4 bytes per block)
    private final static int SECTOR_BYTE_LENGTH          = 128;
    private final static int LENGTH_OFFSET_BYTE_LENGTH   = 2;
    private final static int SECTOR_0_OFFSET_BYTE_LENGTH = 8;

    // doesn't start at 0 -- in I2C mode, first block is overwritten sometimes
    // so start using block 1 to be safe
    private final static byte SECTOR_O_BLOCK_START = 2;
    private final static byte SECTOR_1_BLOCK_START = 32;
    private final static byte SECTOR_2_BLOCK_START = 64;
    private final static byte SECTOR_3_BLOCK_START = 96;
    // number of blocks total in nucleo-02a1 tag
    private final static int  RF_NUM_BLOCKS        = 128;

    private final static String TAG = "NFCV_SECURITY";

    private static byte[] try_transceive(String command, Tag tag, byte[] response, byte[] data_command)
    {
        //byte[] response = {ERROR_BYTE};

        // was the byte array successfully transceived?
        // this is true even if tag returned error
        boolean transceive_occurred = false;

        int error_count = 0;

        while (!transceive_occurred)
        {
            try
            {
                // get v tag and transceive
                NfcV nfcvTag = NfcV.get(tag);
                nfcvTag.close();
                nfcvTag.connect();
                response = nfcvTag.transceive(data_command);
                Log.e(TAG, command + " TRANSCEIVE SUCCESSFUL");

                // first response element 0 means success
                // first response element 1 means error
                // either way, transceive successfully reached tag
                //response 01 = error sent back by tag (new Android 4.2.2) or BC
                if (response[0] == ERROR_BYTE || response[0] == SUCCESS_BYTE)
                {
                    transceive_occurred = true;
                }
            }
            catch(Exception e)
            {
                Log.e(TAG, command + " ERROR", e);
                error_count++;
                if (error_count >= MAX_TRANSCEIVE_ATTEMPTS)
                {
                    return response;
                }
            }
        }
        return response;
    }

    public static byte[] SendLockSectorCommand (Tag tag, byte SectorNumber, byte LockSectorByte)
    {
        byte[] response = new byte[] {(byte) 0xFF};
        byte[] LockSectorFrame = new byte[]{(byte) 0x02, (byte) 0xB2, (byte) 0x02, SectorNumber, LockSectorByte};
        return try_transceive(LOCK_SECTOR_COMMAND, tag, response, LockSectorFrame);
    }

    public static byte[] SendPresentPasswordCommand (Tag tag, byte PasswordNumber, byte[] PasswordData)
    {
        byte[] response = {(byte) 0xFF};
        byte[] PresentPasswordFrame = {(byte) 0x02, (byte) 0xB3, (byte) 0x02, PasswordNumber,
                PasswordData[0], PasswordData[1], PasswordData[2], PasswordData[3]};
        return try_transceive(PRESENT_PASSWORD_COMMAND, tag, response, PresentPasswordFrame);
    }

    public static byte[] SendReadMultipleBlockCommand (Tag tag, byte StartAddress, byte NbOfBlockToRead)
    {

        byte[] response = {ERROR_BYTE};
        byte[] ReadMultipleBlockFrame = new byte[]{(byte) 0x02, (byte) 0x23, StartAddress, NbOfBlockToRead};
        return try_transceive(READ_BLOCKS_COMMAND, tag, response, ReadMultipleBlockFrame);
    }

    public static byte[] SendWriteSingleBlockCommand (Tag tag, byte StartAddress, byte[] DataToWrite)
    {
        assert (DataToWrite.length == 4);

        byte[] response = {(byte) 0xFF};
        byte[] WriteSingleBlockFrame = {(byte) 0x02, (byte) 0x21, StartAddress, DataToWrite[0], DataToWrite[1], DataToWrite[2], DataToWrite[3]};
        return try_transceive(WRITE_BLOCK_COMMAND, tag, response, WriteSingleBlockFrame);
    }

    // wrapper method for lower level function <SendMultipleBlockCommand>
    // read contents of a single sector
    public static byte[] readSector(Tag tag, int sector)
    {
        assert (0 <= sector && sector <= 3);

        byte num_blocks;
        byte start_address;

        if (sector == 0)
        {
            num_blocks = SECTOR_1_BLOCK_START - SECTOR_O_BLOCK_START;
            start_address = SECTOR_O_BLOCK_START;
        }
        else if (sector == 1)
        {
            num_blocks = SECTOR_2_BLOCK_START - SECTOR_1_BLOCK_START;
            start_address = SECTOR_1_BLOCK_START;
        }
        else if (sector == 2)
        {
            num_blocks = SECTOR_3_BLOCK_START - SECTOR_2_BLOCK_START;
            start_address = SECTOR_2_BLOCK_START;
        }
        else
        {
            num_blocks = (byte) (RF_NUM_BLOCKS - (int) SECTOR_3_BLOCK_START);
            start_address = SECTOR_3_BLOCK_START;
        }
        num_blocks--;

        return NFCV_Security.SendReadMultipleBlockCommand(tag, start_address, num_blocks);
    }

    // wrapper method for lower level function <SendWriteSingleBlockCommand>
    // writes multiple blocks at a time
    public static byte[] SendWriteMultipleBlockCommand (Tag myTag, byte StartAddress, byte[] DataToWrite)
    {
        byte[] response = {ERROR_BYTE};
        long cpt = 0;

        // get number of bytes to write
        int NBByteToWrite = DataToWrite.length;
        while (NBByteToWrite % 4 != 0)
        {
            // blocks consist of 4 bytes
            NBByteToWrite++;
        }

        // create final byte array to write, with filler bits if necessary
        byte[] fullByteArrayToWrite = new byte[NBByteToWrite];
        for(int j = 0; j < NBByteToWrite; j++)
        {
            if (j < DataToWrite.length)
            {
                fullByteArrayToWrite[j] = DataToWrite[j];
            }
            else
            {
                fullByteArrayToWrite[j] = (byte) 0xFF;
            }
        }

        byte[] block_to_write = new byte[4];

        for (int i = 0; i < NBByteToWrite; i = i + 4)
        {
            // set data block to write
            block_to_write[0] = (byte) fullByteArrayToWrite[i];
            block_to_write[1] = (byte) fullByteArrayToWrite[i + 1];
            block_to_write[2] = (byte) fullByteArrayToWrite[i + 2];
            block_to_write[3] = (byte) fullByteArrayToWrite[i + 3];

            response[0] = ERROR_BYTE;

            while((response[0] == ERROR_BYTE) && cpt <= MAX_WRAPPER_TRIES)
            {

                response = SendWriteSingleBlockCommand(myTag, StartAddress, block_to_write);
                cpt++;
            }

            if (response[0] == ERROR_BYTE)
            {
                return response;
            }
            // increment address to write
            StartAddress++;
            cpt = 0;
        }
        return response;
    }

    /************** high level Enphase-specific use functions **************/

    public static boolean reportCommandError(Context context, String command, byte[] response)
    {
        if (response == null)
        {
            Toast.makeText(context, command + " UNSUCCESSFUL", Toast.LENGTH_SHORT).show();
            Log.e(TAG, command + " UNSUCCESSFUL");
            return false;
        }
        else if (response[0] != SUCCESS_BYTE)
        {
            Toast.makeText(context, command + " UNSUCCESSFUL", Toast.LENGTH_SHORT).show();
            Log.e(TAG, command + " UNSUCCESSFUL");
            Log.e(context.toString(), Arrays.toString(response));
            return false;
        }
        Toast.makeText(context, command + " SUCCESSFUL", Toast.LENGTH_SHORT).show();
        return true;
    }

    public static int two_byte_to_int(byte byte1, byte byte2)
    {
        return ((byte2 & 0xFF) << 8) | (byte1 & 0xFF);
    }

    private static String readText(NdefRecord record) throws UnsupportedEncodingException
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

    public static String[] raw_data_to_payloads(Context context, boolean isProtected, byte[] raw_tag_data)
    {
        int length_checker = SECTOR_BYTE_LENGTH - SECTOR_0_OFFSET_BYTE_LENGTH;
        if (!isProtected)
        {
            length_checker = SECTOR_BYTE_LENGTH * 3;
        }
        assert (raw_tag_data != null);
        assert (raw_tag_data.length == length_checker);

        int I2CRF_OFFSET = 0;

        // first two bytes are length
        int ndef_length = two_byte_to_int(raw_tag_data[1 + I2CRF_OFFSET], raw_tag_data[0 + I2CRF_OFFSET]);
        assert (ndef_length <= length_checker - LENGTH_OFFSET_BYTE_LENGTH);

        byte[] ndef_data = new byte[ndef_length];
        Log.e(TAG, Arrays.toString(ndef_data));

        System.arraycopy(raw_tag_data, LENGTH_OFFSET_BYTE_LENGTH + I2CRF_OFFSET, ndef_data, 0, ndef_length);
        Log.e(TAG, Arrays.toString(raw_tag_data));
        Log.e(TAG, "NDEF Array Length: " + String.valueOf(ndef_data.length));

        NdefMessage ndefMessage = null;
        try
        {
            ndefMessage = new NdefMessage(ndef_data);
        }
        catch (FormatException e)
        {
            NFC_Helper.reportError(context, TAG, "Tag data is not NDEF formatted.", e);
            return null;
        }

        // have ndef message, extract payloads from each record
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
                    NFC_Helper.reportError(context, TAG, "Message contains unsupported encoding.", e);
                    return null;
                }
            }
            else
            {
                NFC_Helper.reportError(context, TAG, "Message is not text type.", null);
                return null;
            }
        }

        return payloads;
    }


    // first sector is write-protected
    // anyone can read freely
    public static byte[] readProtectedSector(Tag tag)
    {
        return readSector(tag, 0);
    }

    // other three sectors are unprotected
    // can read/write freely
    public static byte[] readUnprotectedSectors(Tag tag)
    {
        // final array to store data from all 3 sectors
        // only returned on 3 successful sector reads
        byte[] read_data = new byte[(SECTOR_BYTE_LENGTH * 3) + 1];
        read_data[0] = SUCCESS_BYTE;

        // 0 sector is protected, so start at 1
        for (int i = 0; i < NUM_UNPROTECTED_SECTORS; i++)
        {
            byte[] response = {ERROR_BYTE};
            int attempt_counter = 0;
            while (response[0] == ERROR_BYTE && attempt_counter <= MAX_WRAPPER_TRIES)
            {
                response = readSector(tag, i + 1);
                attempt_counter++;
            }

            // if error, just return error
            if (response[0] == ERROR_BYTE)
            {
                return response;
            }

            assert(response.length == SECTOR_BYTE_LENGTH);

            // otherwise, copy over byte array without response byte
            System.arraycopy(response, 1, read_data, i*SECTOR_BYTE_LENGTH + 1, SECTOR_BYTE_LENGTH);
        }

        return read_data;
    }

    public static byte[] push_two_byte_length(byte[] data_to_write)
    {
        int length = data_to_write.length;
        assert (length >= 0 && length < SECTOR_BYTE_LENGTH * NUM_SECTORS);

        // break up length integer into 2 bytes
        byte byte1 = (byte) (length & 0xFF);
        byte byte2 = (byte) ((length & 0xFF00) >> 8);

        Log.e(TAG, String.valueOf(length));
        Log.e(TAG, String.valueOf(two_byte_to_int(byte1, byte2)));

        // make new array with length bytes and return
        byte[] data_with_length = new byte[length + 2];
        data_with_length[0] = byte2;
        data_with_length[1] = byte1;
        System.arraycopy(data_to_write, 0, data_with_length, 2, length);
        return data_with_length;
    }

    public static byte[] writeProtectedSectors(Tag tag, byte[] data)
    {
        byte[] data_with_byte_length = push_two_byte_length(data);
        // check for length
        if (data.length > SECTOR_BYTE_LENGTH - SECTOR_0_OFFSET_BYTE_LENGTH)
        {
            Log.e(TAG, "Message to write is too long!");
            return null;
        }
        return SendWriteMultipleBlockCommand(tag, SECTOR_O_BLOCK_START, data_with_byte_length);
    }

    public static byte[] writeUnprotectedSectors(Tag tag, byte[] data)
    {
        byte[] data_with_byte_length = push_two_byte_length(data);
        if (data.length > SECTOR_BYTE_LENGTH * NUM_UNPROTECTED_SECTORS)
        {
            Log.e(TAG, "Message to write is too long!");
            return null;
        }
        return SendWriteMultipleBlockCommand(tag, SECTOR_1_BLOCK_START, data_with_byte_length);
    }

}
