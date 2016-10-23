package com.enphaseenergy.nfcgatewayalpha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bon on 9/13/16.
 */
public class RecordsListAdapter extends BaseAdapter
{
    public static class RecordItem
    {
        public String header;
        public String content;

        public RecordItem(String header, String content)
        {
            this.header = header;
            this.content = content;
        }
    }


    private List<RecordItem> records;
    private final Context context;

    public RecordsListAdapter(Context context)
    {
        this.records = new ArrayList<RecordItem>();
        this.context = context;
    }

    @Override
    public int getCount()
    {
        return records.size();
    }

    public void addRecordItem(RecordItem record)
    {
        records.add(record);
    }

    @Override
    public Object getItem(int position)
    {
        return records.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        // don't intend to use this function, but must override anyway
        return (long) position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.records_list_layout, parent, false);
        TextView header = (TextView) itemView.findViewById(R.id.item_header);
        TextView content = (TextView) itemView.findViewById(R.id.item_content);
        RecordItem record = records.get(position);
        header.setText(record.header);
        content.setText(record.content);
        return itemView;
    }
}
