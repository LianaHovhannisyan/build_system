package instigate.simCardChangeNotifier.ui;

/**
 * Created by Instigate Mobile on 11/25/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.logic.SharedData;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * This class is designed because of in EditTextPreference we haven't any
 * interface to check inputed text before saving it. One more reason is
 * EditTextPreference's bug that is when user press "OK" button when showing
 * soft keyboard, keyboard will stay showing.
 */
public class CustomEditTextPreference extends EditTextPreference {
  private int minLength;
    Button okBtn;

    public CustomEditTextPreference(Context cont) {
        super(cont);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateDialogView() {
        if (getKey().equals("unhide_number")) {
            minLength = SharedData.UNHIDE_NUMBER_LENGTH;
        } else if (getKey().equals("unlock_password")) {
            minLength = SharedData.UNLOCK_PASSWORD_LENGTH;
        }
        setDialogMessage(getContext().getResources().getString(R.string.minimum_requirement,
                minLength));

        return super.onCreateDialogView();
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (getDialog() != null) {
                    okBtn = (Button) getDialog().findViewById(android.R.id.button1);
                    if (getEditText().length() < minLength) {
                        okBtn.setEnabled(false);
                    } else {
                        okBtn.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        hideKeyboard();
        super.onClick(dialog, which);
    }   

    //
    // Hides soft keyboard from screen.
    //
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getEditText().getWindowToken(), 0);
    }

}
