package org.secuso.privacyfriendlypasswordgenerator.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.secuso.privacyfriendlypasswordgenerator.R;
import org.secuso.privacyfriendlypasswordgenerator.database.MetaData;
import org.secuso.privacyfriendlypasswordgenerator.database.MetaDataSQLiteHelper;
import org.secuso.privacyfriendlypasswordgenerator.generator.PasswordGeneratorTask;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * @author Karola Marky
 * @version 20170113
 */

public class UpdatePasswordDialog extends DialogFragment {

    Activity activity;
    View rootView;

    MetaDataSQLiteHelper database;
    int position;
    MetaData metaData;
    MetaData oldMetaData;

    boolean bindToDevice_enabled;
    String hashAlgorithm;
    int number_iterations;

    ProgressBar spinnerOld;
    ProgressBar spinnerNew;

    ImageButton visibilityButton;
    boolean visibility;

    EditText editTextUpdateMasterpassword;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        rootView = inflater.inflate(R.layout.dialog_update_passwords, null);

        visibility = false;

        spinnerOld = (ProgressBar) rootView.findViewById(R.id.oldProgressBar);
        spinnerOld.setVisibility(View.GONE);

        spinnerNew = (ProgressBar) rootView.findViewById(R.id.newProgressBar);
        spinnerNew.setVisibility(View.GONE);

        Bundle bundle = getArguments();

        position = bundle.getInt("position");
        bindToDevice_enabled = bundle.getBoolean("bindToDevice_enabled");
        //Log.d("UPDATER BIND", Boolean.toString(bindToDevice_enabled));
        hashAlgorithm = bundle.getString("hash_algorithm");
        //Log.d("UPDATER HASH", hashAlgorithm);
        setOldMetaData(bundle);
        number_iterations = bundle.getInt("number_iterations");

        database = MetaDataSQLiteHelper.getInstance(getActivity());

        builder.setView(rootView);

        builder.setIcon(R.mipmap.ic_drawer);

        builder.setTitle(getActivity().getString(R.string.passwords_heading));

        Button displayButton = (Button) rootView.findViewById(R.id.displayButton);
        displayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);


                    displayPasswords();
            }
        });

        ImageButton copyOldButton = (ImageButton) rootView.findViewById(R.id.copyOldButton);
        ImageButton copyNewButton = (ImageButton) rootView.findViewById(R.id.copyNewButton);

        copyOldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView oldPassword = (TextView) rootView.findViewById(R.id.textViewOldPassword);
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", oldPassword.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, activity.getString(R.string.copy_clipboar_old), Toast.LENGTH_SHORT).show();
            }
        });

        copyNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView newPassword = (TextView) rootView.findViewById(R.id.textViewNewPassword);
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", newPassword.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, activity.getString(R.string.copy_clipboar_new), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setPositiveButton(getActivity().getString(R.string.done), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onClickDone();
            }
        });

        visibilityButton = (ImageButton) rootView.findViewById(R.id.visibilityButton);

        visibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextUpdateMasterpassword = (EditText) rootView.findViewById(R.id.editTextUpdateMasterpassword);

                if (!visibility) {
                    visibilityButton.setImageResource(R.drawable.ic_visibility_off);
                    editTextUpdateMasterpassword.setInputType(
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    editTextUpdateMasterpassword.setSelection(editTextUpdateMasterpassword.getText().length());
                    visibility = true;
                } else {
                    visibilityButton.setImageResource(R.drawable.ic_visibility);
                    editTextUpdateMasterpassword.setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    editTextUpdateMasterpassword.setSelection(editTextUpdateMasterpassword.getText().length());
                    visibility = false;
                }
            }
        });

        return builder.create();
    }

    public void onClickDone() {
        activity.recreate();
        this.dismiss();
    }

    public void displayPasswords() {
        editTextUpdateMasterpassword = (EditText) rootView.findViewById(R.id.editTextUpdateMasterpassword);
        TextView textViewOld = (TextView) rootView.findViewById(R.id.textViewOldPassword);
        TextView textViewNew = (TextView) rootView.findViewById(R.id.textViewNewPassword);

        textViewOld.setText("");
        textViewNew.setText("");

        if (editTextUpdateMasterpassword.getText().toString().length() == 0) {
            Toast toast = Toast.makeText(activity.getBaseContext(), getString(R.string.enter_masterpassword), Toast.LENGTH_SHORT);
            toast.show();
        } else {

            spinnerOld.setVisibility(View.VISIBLE);
            spinnerNew.setVisibility(View.VISIBLE);

            final TextView textViewOldPassword = (TextView) rootView.findViewById(R.id.textViewOldPassword);
            final TextView textViewNewPassword = (TextView) rootView.findViewById(R.id.textViewNewPassword);

            String deviceID;

            if (bindToDevice_enabled) {
                deviceID = Settings.Secure.getString(getContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                Log.d("DEVICE ID", Settings.Secure.getString(getContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID));
            } else {
                deviceID = "SECUSO";
            }

            //pack old parameters to String-Array
            String[] paramsOld = new String[12];
            paramsOld[0] = oldMetaData.getDOMAIN();
            paramsOld[1] = oldMetaData.getUSERNAME();
            paramsOld[2] = editTextUpdateMasterpassword.getText().toString();
            paramsOld[3] = deviceID;
            paramsOld[4] = String.valueOf(oldMetaData.getITERATION());
            paramsOld[5] = String.valueOf(number_iterations);
            paramsOld[6] = hashAlgorithm;
            paramsOld[7] = String.valueOf(oldMetaData.getHAS_SYMBOLS());
            paramsOld[8] = String.valueOf(oldMetaData.getHAS_LETTERS_LOW());
            paramsOld[9] = String.valueOf(oldMetaData.getHAS_LETTERS_UP());
            paramsOld[10] = String.valueOf(oldMetaData.getHAS_NUMBERS());
            paramsOld[11] = String.valueOf(oldMetaData.getLENGTH());

            new PasswordGeneratorTask() {
                @Override
                protected void onPostExecute(String result) {
                    textViewOldPassword.setText(result);
                    spinnerOld.setVisibility(View.GONE);
                }
            }.execute(paramsOld);

            //generate new password
            metaData = database.getMetaData(position);

            //pack new parameters to String-Array
            String[] paramsNew = new String[12];
            paramsNew[0] = metaData.getDOMAIN();
            paramsNew[1] = metaData.getUSERNAME();
            paramsNew[2] = editTextUpdateMasterpassword.getText().toString();
            paramsNew[3] = deviceID;
            paramsNew[4] = String.valueOf(metaData.getITERATION());
            paramsNew[5] = String.valueOf(number_iterations);
            paramsNew[6] = hashAlgorithm;
            paramsNew[7] = String.valueOf(metaData.getHAS_SYMBOLS());
            paramsNew[8] = String.valueOf(metaData.getHAS_LETTERS_LOW());
            paramsNew[9] = String.valueOf(metaData.getHAS_LETTERS_UP());
            paramsNew[10] = String.valueOf(metaData.getHAS_NUMBERS());
            paramsNew[11] = String.valueOf(metaData.getLENGTH());

            new PasswordGeneratorTask() {
                @Override
                protected void onPostExecute(String result) {
                    textViewNewPassword.setText(result);
                    spinnerNew.setVisibility(View.GONE);
                }
            }.execute(paramsNew);

        }
    }

    public void setOldMetaData(Bundle bundle) {
        oldMetaData = new MetaData(0, 0,
                bundle.getString("olddomain"),
                bundle.getString("oldusername"),
                bundle.getInt("oldlength"),
                bundle.getInt("oldnumbers"),
                bundle.getInt("oldsymbols"),
                bundle.getInt("oldlettersup"),
                bundle.getInt("oldletterslow"),
                bundle.getInt("olditeration")
        );

    }

}
