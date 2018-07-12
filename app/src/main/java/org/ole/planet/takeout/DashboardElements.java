package org.ole.planet.takeout;

import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Extra class for excess methods in Dashboard activities
 */

public class DashboardElements extends AppCompatActivity {

    private EditText feedbackText;
    private RadioGroup choice1, choice2;

    /**
     * Disables the submit button until the feedback form is complete
     */
    public void disableSubmit(MaterialDialog dialog) {
        final View submitButton = dialog.getActionButton(DialogAction.POSITIVE);
        submitButton.setEnabled(false);
        feedbackText = dialog.getCustomView().findViewById(R.id.user_feedback);
        choice1 = dialog.getCustomView().findViewById(R.id.choice1);
        choice2 = dialog.getCustomView().findViewById(R.id.choice2);
        feedbackText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (choice1.getCheckedRadioButtonId() == -1 || choice2.getCheckedRadioButtonId() == -1 || s.toString().length() == 0) {
                    submitButton.setEnabled(false);
                } else {
                    submitButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() != 0) {
                    submitButton.setEnabled(true);
                }
            }
        });
    }
}
