package com.gaborbiro.filepicker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class FileDialogActivity extends ListActivity {

    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 1;

    public static final int SELECTION_MODE_CREATE = 1;
    public static final int SELECTION_MODE_OPEN = 2;

    private static final String ROOT = "/";
    private static final String PARENT = "../";

    public static final String EXTRA_START_PATH = "START_PATH";
    public static final String EXTRA_FORMAT_FILTER = "FORMAT_FILTER";
    public static final String EXTRA_RESULT_PATH = "RESULT_PATH";
    public static final String EXTRA_SELECTION_MODE = "SELECTION_MODE";
    public static final String EXTRA_CAN_SELECT_DIR = "CAN_SELECT_DIR";

    private int mSelectionMode = SELECTION_MODE_CREATE;
    private String[] mFormatFilters = null;
    private boolean mCanSelectDir = false;

    private TextView mPathView;
    private EditText mFileNameView;
    private Button mSelectButton;
    private LinearLayout mSelectionButtonsContainer;
    private LinearLayout mCreationButtonsContainer;
    private InputMethodManager mInputManager;

    private FileListAdapter mAdapter;
    private File mClickedFile;
    private String mCurrentPath = ROOT;
    private String mParentPath;
    private HashMap<String, Integer> mLastPositions = new HashMap<>();

    private PermissionVerifier mPermissionVerifier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        String[] permissions;

        if (mSelectionMode == SELECTION_MODE_CREATE) {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        mPermissionVerifier = new PermissionVerifier(this, permissions);
        mPermissionVerifier.verifyPermissions(true,
                REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);

        setResult(RESULT_CANCELED, getIntent());

        mSelectionMode =
                getIntent().getIntExtra(EXTRA_SELECTION_MODE, SELECTION_MODE_CREATE);
        mFormatFilters = getIntent().getStringArrayExtra(EXTRA_FORMAT_FILTER);
        mCanSelectDir = getIntent().getBooleanExtra(EXTRA_CAN_SELECT_DIR, false);

        mInputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        mPathView = (TextView) findViewById(R.id.path);
        mFileNameView = (EditText) findViewById(R.id.file_name);
        mSelectButton = (Button) findViewById(R.id.select_btn);
        mSelectButton.setEnabled(false);
        mSelectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedFile != null) {
                    getIntent().putExtra(EXTRA_RESULT_PATH, mClickedFile.getPath());
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        });
        Button newButton = (Button) findViewById(R.id.new_btn);
        newButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setCreateVisible(v);
                mFileNameView.setText("");
                mFileNameView.requestFocus();
            }
        });
        if (mSelectionMode == SELECTION_MODE_OPEN) {
            newButton.setEnabled(false);
        }

        mSelectionButtonsContainer =
                (LinearLayout) findViewById(R.id.selection_buttons_container);
        mCreationButtonsContainer =
                (LinearLayout) findViewById(R.id.creation_buttons_container);
        mCreationButtonsContainer.setVisibility(View.GONE);
        Button cancelButton = (Button) findViewById(R.id.cancel_btn);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectVisible(v);
            }
        });
        Button createButton = (Button) findViewById(R.id.create_btn);
        createButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mFileNameView.getText()
                        .length() > 0) {
                    getIntent().putExtra(EXTRA_RESULT_PATH,
                            mCurrentPath + "/" + mFileNameView.getText());
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        });

        String startPath = getIntent().getStringExtra(EXTRA_START_PATH);
        startPath = startPath != null ? startPath : ROOT;
        if (mCanSelectDir) {
            mClickedFile = new File(startPath);
            mSelectButton.setEnabled(true);
        }
        setData(startPath);
    }

    private void setData(String dirPath) {
        boolean useAutoSelection = dirPath.length() < mCurrentPath.length();
        List<DirUtils.FileInfo> files = null;
        try {
            files = DirUtils.getDirInfo(dirPath, mFormatFilters);
        } catch (DirReadException e) {
            e.printStackTrace();
            Toast.makeText(FileDialogActivity.this, "Unable to read folder " + dirPath,
                    Toast.LENGTH_SHORT)
                    .show();
            try {
                dirPath = ROOT;
                files = DirUtils.getDirInfo(dirPath, mFormatFilters);
            } catch (DirReadException e1) {
                e1.printStackTrace();
            }
        }
        mParentPath = null;

        if (files != null) {
            if (!dirPath.equals(ROOT)) {
                files.add(0, new DirUtils.FileInfo(ROOT, true));
                files.add(1, new DirUtils.FileInfo(PARENT, true));
                mParentPath = new File(dirPath).getParent();
            }

            mCurrentPath = dirPath;
            mAdapter = new FileListAdapter(this, files);
            setListAdapter(mAdapter);

            Integer position = mLastPositions.get(mParentPath);
            if (position != null && useAutoSelection) {
                getListView().setSelection(position);
            }
            mPathView.setText(getText(R.string.location) + ": " + mCurrentPath);
        } else {
            setListAdapter(null);
            mAdapter = null;
            mPathView.setText(null);
        }
    }

    private static class FileListAdapter extends ArrayAdapter<DirUtils.FileInfo> {

        public FileListAdapter(Context context, List<DirUtils.FileInfo> items) {
            super(context, 0, items);
        }

        private class ViewHolder {
            ImageView mIcon;
            TextView mFilename;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.file_picker_list_item, parent, false);
                holder = new ViewHolder();
                holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                holder.mFilename = (TextView) view.findViewById(R.id.file_name);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }
            DirUtils.FileInfo fileInfo = getItem(position);
            holder.mIcon.setImageResource(
                    fileInfo.isFolder ? R.drawable.folder : R.drawable.file);
            holder.mFilename.setText(fileInfo.path);
            return view;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String path = mAdapter.getItem(position).path;
        File file;
        if (path.equals(ROOT)) {
            file = new File(path);
        } else {
            file = new File(mCurrentPath, path);
        }
        setSelectVisible(v);

        if (file.isDirectory()) {
            mSelectButton.setEnabled(false);
            if (file.canRead()) {
                mLastPositions.put(mCurrentPath, position);
                setData(file.getAbsolutePath());

                if (mCanSelectDir) {
                    mClickedFile = file;
                    v.setSelected(true);
                    mSelectButton.setEnabled(true);
                }
            } else {
                new AlertDialog.Builder(this).setTitle(
                        "Folder [" + file.getName() + "] " +
                                getText(R.string.cant_read_folder))
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            mClickedFile = file;
            v.setSelected(true);
            mSelectButton.setEnabled(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            mSelectButton.setEnabled(false);

            if (mCreationButtonsContainer.getVisibility() == View.VISIBLE) {
                mCreationButtonsContainer.setVisibility(View.GONE);
                mSelectionButtonsContainer.setVisibility(View.VISIBLE);
            } else {
                if (mParentPath != null) {
                    setData(mParentPath);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void setCreateVisible(View v) {
        mCreationButtonsContainer.setVisibility(View.VISIBLE);
        mSelectionButtonsContainer.setVisibility(View.GONE);
        mInputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        mSelectButton.setEnabled(false);
    }

    private void setSelectVisible(View v) {
        mCreationButtonsContainer.setVisibility(View.GONE);
        mSelectionButtonsContainer.setVisibility(View.VISIBLE);
        mInputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        mSelectButton.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (!mPermissionVerifier.onRequestPermissionsResult(requestCode, permissions,
                grantResults)) {
            Toast.makeText(this, "Missing permissions!",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
