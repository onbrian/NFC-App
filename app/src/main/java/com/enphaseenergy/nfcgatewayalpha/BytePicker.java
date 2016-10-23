package com.enphaseenergy.nfcgatewayalpha;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.NumberPicker;

/**
 * Created by bon on 9/21/16.
 */
public class BytePicker extends NumberPicker
{

    public BytePicker(Context context)
    {
        super(context);
    }

    public BytePicker(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        processAttributeSet(attrs);
    }

    public BytePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        processAttributeSet(attrs);
    }
    private void processAttributeSet(AttributeSet attrs)
    {
        //This method reads the parameters given in the xml file and sets the properties according to it
        //this.setMinValue(attrs.getAttributeIntValue(null, "min", 0));
        //this.setMaxValue(attrs.getAttributeIntValue(null, "max", 0));
        this.setMinValue(0);
        this.setMaxValue(255);
        this.setOnLongPressUpdateInterval(0);
    }
}

