package edu.temple.markopromo;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 * Created by Robyn on 11/6/2016.
 */

public class MessageGridViewAdapter extends ArrayAdapter {
    Context context;
    String[] list;

    public MessageGridViewAdapter(Context context, int resource, Object[] list) {
        super(context, resource, list);
        this.context = context;
        this.list = (String[]) list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView textView;
        if (convertView == null) {
            textView = new TextView(context);
            //textView.setLayoutParams(new GridView.LayoutParams(100, 80));
            textView.setTextSize(20);   //text size in gridview
        }

        return view;
    }
}
