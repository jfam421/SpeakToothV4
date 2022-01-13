package com.example.speaktoothv4;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;
//sender id = 1 (I sent it); sender id = 0 (The message was sent to me)

public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    ArrayList<MessageModel> list;
    public static final int MESSAGE_TYPE_IN = 0;


    public CustomAdapter(Context context, ArrayList<MessageModel> list) {
        this.context = context;
        this.list = list;
    }


    private class MessageInViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;

        MessageInViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.message_text);
            //On long click listener
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //Copy whole text from item to clipboard
                    String text = messageTV.getText().toString();
                    ClipboardManager clipboard = (ClipboardManager) CustomAdapter.this.context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(null, text);
                    clipboard.setPrimaryClip(clip);
                    //Make toast
                    Toast toast = Toast.makeText(CustomAdapter.this.context, "The text copied", Toast.LENGTH_SHORT);
                    toast.show();
                    return false;
                }
            });
        }

        void bind(int position) {
            MessageModel messageModel = list.get(position);
            messageTV.setText(messageModel.message);
        }
    }

    private class MessageOutViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;

        MessageOutViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.message_text);
            //On long click listener
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //Copy whole text from item to clipboard
                    String text = messageTV.getText().toString();
                    ClipboardManager clipboard = (ClipboardManager) CustomAdapter.this.context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(null, text);
                    clipboard.setPrimaryClip(clip);
                    //Make Toast
                    Toast toast = Toast.makeText(CustomAdapter.this.context, "The text copied", Toast.LENGTH_SHORT);
                    toast.show();
                    return false;
                }
            });

        }

        void bind(int position) {
            MessageModel messageModel = list.get(position);
            messageTV.setText(messageModel.message);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE_IN) {
            return new MessageInViewHolder(LayoutInflater.from(context).inflate(R.layout.message_incoming, parent, false));
        }
        return new MessageOutViewHolder(LayoutInflater.from(context).inflate(R.layout.messages_outgoing, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (list.get(position).messageType == MESSAGE_TYPE_IN) {
            ((MessageInViewHolder) holder).bind(position);
        } else {
            ((MessageOutViewHolder) holder).bind(position);
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).messageType;
    }
}