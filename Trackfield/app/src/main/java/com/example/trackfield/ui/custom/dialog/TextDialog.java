package com.example.trackfield.ui.custom.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.trackfield.R;

public class TextDialog extends BaseDialog {

    // bundle keys
    private static final String BUNDLE_TEXT = "text";
    private static final String BUNDLE_HINT = "hint";

    // dialog tags
    private static final String TAG_DEFAULT = "textDialog";

    private DialogListener listener;

    // arguments
    private String text;
    private String hint;

    //

    public static TextDialog newInstance(@StringRes int titleRes, @StringRes int messageRes, String text, String hint,
        @StringRes int posBtnTxtRes, String tag) {

        TextDialog instance = new TextDialog();
        Bundle bundle = putBundleBase(titleRes, messageRes, posBtnTxtRes, tag);

        bundle.putString(BUNDLE_TEXT, text);
        bundle.putString(BUNDLE_HINT, hint);

        instance.setArguments(bundle);
        return instance;
    }

    // on

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DialogListener) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement DialogListener");
        }
    }

    // extends

    @Override
    protected void unpackBundle() {
        Bundle bundle = unpackBundleBase(TAG_DEFAULT);

        if (bundle != null) {
            text = bundle.getString(BUNDLE_TEXT, "");
            hint = bundle.getString(BUNDLE_HINT, "");
        }
    }

    @Override
    protected AlertDialog buildDialog() {
        final View dialogView = inflater.inflate(R.layout.dialog_text, null);
        final EditText et = dialogView.findViewById(R.id.et_dialog_text);

        et.setText(text);
        et.setHint(hint);
        if (!message.equals("")) builder.setMessage(message);

        builder.setView(dialogView).setTitle(title)
            .setPositiveButton(posBtnTxtRes, (dialog, id) -> {
                String input = et.getText().toString().trim();
                listener.onTextDialogPositiveClick(input, tag);
            })
            .setNegativeButton(negBtnTxtRes, (dialog, id) -> getDialog().cancel());

        return builder.show();
    }

    // interface

    public interface DialogListener {

        void onTextDialogPositiveClick(String input, String tag);

    }

}
