package com.stromberg.scott.thaumcraftaspectgrapher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    private final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 2;

    private static ArrayList<Aspect> aspectList;
    public static ArrayList<Aspect> graphAspects = new ArrayList<>();
    public static Aspect selectedAspect;
    public static Aspect selectedLinkingModeAspect;
    public Aspect lastAddedAspect = null;

    public static int ASPECT_LIST_DIALOG_TYPE_ADD = 0;
    public static int ASPECT_LIST_DIALOG_TYPE_LINK = 1;
    public static int ASPECT_LIST_DIALOG_TYPE_MAP = 2;

    public static int defaultAspectWidth;
    public static int defaultAspectHeight;
    public static int defaultAspectBackgroundWidth;
    public static int defaultAspectBackgroundHeight;
    public static int horizontalPadding;
    public static int verticalPadding;

    private FloatingActionButton fab;
    private FloatingActionMenu searchFam;
    private FloatingActionMenu fam;
    private AutoCompleteTextView searchTextView;
    private String backupLocation = "/data/TCAspectGrapher/backup/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFam.close(true);
                showAspectListDialog(ASPECT_LIST_DIALOG_TYPE_ADD, new AspectListOnClickListener() {
                    @Override
                    public void onClick(Aspect aspect, boolean ignoreMe) {
                        addAspect(aspect);
                    }
                });
            }
        });

        fam = (FloatingActionMenu) findViewById(R.id.fam);
        fam.setClosedOnTouchOutside(true);
        fam.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if(!opened) {
                    MainActivity.selectedAspect = null;

                    fab.setVisibility(View.VISIBLE);
                    fam.setVisibility(View.GONE);
                }
            }
        });

        searchTextView = (AutoCompleteTextView) findViewById(R.id.fab_search_text_view);
        searchFam = (FloatingActionMenu) findViewById(R.id.searchFam);
        searchFam.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                searchTextView.setText("");

                if(opened) {
                    selectedAspect = null;
                    toggleAspectMenu(null, false);

                    Animation in = AnimationUtils.makeInAnimation(MainActivity.this, true);
                    searchTextView.startAnimation(in);
                    searchTextView.setVisibility(View.VISIBLE);

                    searchTextView.setAdapter(new AspectSearchListAdapter(MainActivity.this, graphAspects));
                    searchTextView.setThreshold(1);
                    searchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            selectedAspect = graphAspects.get(position);
                            lastAddedAspect = selectedAspect;

                            toggleAspectMenu(selectedAspect, true);
                        }
                    });
                } else {
                    Animation out = AnimationUtils.makeOutAnimation(MainActivity.this, false);
                    searchTextView.startAnimation(out);
                    searchTextView.setVisibility(View.INVISIBLE);
                }
            }
        });

        Bitmap aspectBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unknown_aspect);
        Bitmap aspectBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.aspect_background);

        defaultAspectWidth = aspectBitmap.getWidth();
        defaultAspectHeight = aspectBitmap.getHeight();
        defaultAspectBackgroundWidth = aspectBackgroundBitmap.getWidth();
        defaultAspectBackgroundHeight = aspectBackgroundBitmap.getHeight();
        horizontalPadding = defaultAspectBackgroundWidth + Aspect.horizontalSpacing;
        verticalPadding = defaultAspectBackgroundHeight + Aspect.verticalSpacing;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getAspectList();
                loadAspects(false);
            }
        }, 250);
    }

    @Override
    public void onBackPressed() {
        if(searchFam.isOpened() || fam.isOpened()) {
            searchFam.close(true);
            fam.close(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_add_all_aspects) {
            addAllAspects();
        } else if(id == R.id.action_clear) {
            clearAspects();
        } else if(id == R.id.action_backup) {
            backupAspects();
        } else if(id == R.id.action_import) {
            importAspects();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveAspects(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    importAspects();
                }
                return;
            }

            case REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backupAspects();
                }
                return;
            }
        }
    }

    private void addAllAspects() {
        synchronized (graphAspects) {
            ArrayList<Aspect> aspects = cloneAspectList(getAspectList());
            aspects.removeAll(graphAspects);
            graphAspects.addAll(aspects);
        }
    }

    private void clearAspects() {
        final ArrayList<Aspect> backupAspects = cloneAspectList(graphAspects);

        synchronized (graphAspects) {
            for (Aspect graphAspect : graphAspects) {
                graphAspect.getLinkedAspectIds().clear();
            }

            graphAspects.clear();
        }

        fam.close(true);

        Snackbar.make(getWindow().getDecorView(), "Cleared all aspects", Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (graphAspects) {
                            for (Aspect backupAspect : backupAspects) {
                                graphAspects.add(backupAspect);
                            }
                        }
                    }
                })
                .setCallback(getUndoSnackbarCallback())
                .show();
    }

    private void loadAspects(boolean showToast) {
        synchronized (graphAspects) {
            final SharedPreferences prefs = getSharedPreferences("ThaumcraftAspectGrapher", Activity.MODE_PRIVATE);
            graphAspects = new Gson().fromJson(prefs.getString("graphAspects", ""), new TypeToken<ArrayList<Aspect>>() {}.getType());

            if (graphAspects != null) {
                synchronized (aspectList) {
                    for (Aspect graphAspect : graphAspects) {
                        int resID = getResources().getIdentifier(graphAspect.getName().toLowerCase(), "drawable", getPackageName());
                        graphAspect.setImageResourceId(resID);

                        Aspect existingAspect = MainActivity.getAspectById(graphAspect.getId());
                        aspectList.remove(existingAspect);
                    }

                    for (Aspect graphAspect : graphAspects) {
                        aspectList.add(graphAspect);
                    }
                }
            } else {
                graphAspects = new ArrayList<>();
            }

            if(showToast) {
                Toast.makeText(MainActivity.this, "Aspects loaded", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveAspects(boolean showToast) {
        synchronized (graphAspects) {
            final SharedPreferences prefs = getSharedPreferences("ThaumcraftAspectGrapher", Activity.MODE_PRIVATE);
            prefs.edit().putString("graphAspects", new Gson().toJson(MainActivity.graphAspects)).apply();

            if(showToast) {
                Toast.makeText(MainActivity.this, "Aspects saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addAspect(Aspect aspect) {
        final Aspect aspectToAdd = new Aspect(aspect.getId(), aspect.getImageResourceId(), aspect.getHorizontalPosition(), aspect.getVerticalPosition(), aspect.getName());

        synchronized (graphAspects) {
            graphAspects.add(aspectToAdd);
        }

        MainActivity.selectedAspect = aspectToAdd;
        toggleAspectMenu(aspectToAdd, true);
        lastAddedAspect = aspectToAdd;

        Snackbar.make(getWindow().getDecorView(), "Added " + aspectToAdd.getName(), Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (graphAspects) {
                            graphAspects.remove(aspectToAdd);
                        }

                        MainActivity.selectedAspect = null;
                        toggleAspectMenu(null, false);
                        lastAddedAspect = null;
                    }

                })
                .setCallback(getUndoSnackbarCallback())
                .show();
    }

    private void backupAspects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Backup");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);
        input.setText(new Date().toString());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();

                try {
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + backupLocation);
                    dir.mkdirs();
                    File file = new File(dir, fileName + ".txt");

                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(new Gson().toJson(graphAspects).getBytes());

                    Toast.makeText(MainActivity.this, "Backup successful", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Backup failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void importAspects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
            return;
        }

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + backupLocation);

        final File[] files = dir.listFiles();
        if(files != null) {
            final String[] fileNames = new String[files.length];

            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i].getName();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("Import");
            builder.setItems(fileNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    try {
                        File sdCard = Environment.getExternalStorageDirectory();
                        File dir = new File(sdCard.getAbsolutePath() + backupLocation);
                        File file = new File(dir, fileNames[item]);

                        FileInputStream fis = new FileInputStream(file.getAbsolutePath());
                        StringBuffer fileContent = new StringBuffer("");

                        byte[] buffer = new byte[1024];

                        int n = 0;
                        while ((n = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer, 0, n));
                        }
                        fis.close();

                        final ArrayList<Aspect> backupAspects = cloneAspectList(graphAspects);
                        graphAspects = new Gson().fromJson(fileContent.toString(), new TypeToken<ArrayList<Aspect>>() {}.getType());

                        Snackbar.make(getWindow().getDecorView(), "Imported aspects", Snackbar.LENGTH_LONG)
                                .setAction("Undo", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        synchronized (graphAspects) {
                                            graphAspects = backupAspects;
                                        }
                                    }
                                })
                                .setCallback(getUndoSnackbarCallback())
                                .show();
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "Import failed", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            Toast.makeText(MainActivity.this, "No files found", Toast.LENGTH_SHORT).show();

        }
    }

    private void showAspectListDialog(int type, AspectListOnClickListener onClickListener) {
        String title = "";
        ArrayList<Aspect> aspects = new ArrayList<>();

        if(type == ASPECT_LIST_DIALOG_TYPE_ADD) {
            title = "Add Aspect";
            aspects = cloneAspectList(getAspectList());
            aspects.removeAll(graphAspects);
        } else if(type == ASPECT_LIST_DIALOG_TYPE_LINK) {
            title = "Link to Aspect";
            if(selectedAspect != null) {
                aspects = cloneAspectList(getAspectList());
                aspects.remove(selectedAspect);
            }
        } else if(type == ASPECT_LIST_DIALOG_TYPE_MAP) {
            title = "Chart to Aspect";
            if(selectedAspect != null) {
                aspects = cloneAspectList(getAspectList());
                aspects.remove(selectedAspect);
            }
        }

        Collections.sort(aspects, new Comparator<Aspect>() {
            @Override
            public int compare(Aspect aspect1, Aspect aspect2)
            {
                return aspect1.getName().compareTo(aspect2.getName());
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);

        LinearLayout container = (LinearLayout) getLayoutInflater().inflate(R.layout.aspect_list, null);

        builder.setView(container);
        final Dialog dialog = builder.create();

        RecyclerView recyclerView = (RecyclerView) container.findViewById(R.id.aspect_list_recyclerview);
        AspectListAdapter aspectListAdapter = new AspectListAdapter(onClickListener, dialog, type);
        aspectListAdapter.setItems(aspects);
        recyclerView.setAdapter(aspectListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        dialog.show();
    }

    private void showAspectLinkDialog(LinkedList<Aspect> path) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(path.get(0).getName() + " to " + path.getLast().getName());

        View container = getLayoutInflater().inflate(R.layout.aspect_link, null);
        LinearLayout list = (LinearLayout) container.findViewById(R.id.aspect_link_list);

        for (int i = 0; i < path.size(); i++) {
            ImageView aspect = new ImageView(MainActivity.this);
            aspect.setImageResource(path.get(i).getImageResourceId());

            list.addView(aspect);

            if(i < path.size() - 1) {
                ImageView arrow = new ImageView(MainActivity.this);
                arrow.setImageResource(R.drawable.ic_arrow_forward_black_36dp);

                list.addView(arrow);
            }
        }

        builder.setView(container);

        final Dialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }

    public void toggleAspectMenu(final Aspect selectedAspect, boolean show) {
        if(show) {
            fab.setVisibility(View.GONE);
            searchFam.close(true);
            fam.setVisibility(View.VISIBLE);

            fam.removeAllMenuButtons();

            ContextThemeWrapper context = new ContextThemeWrapper(MainActivity.this, R.style.MenuButtonsStyle);

            final FloatingActionButton linkFAB = new FloatingActionButton(context);
            linkFAB.setButtonSize(FloatingActionButton.SIZE_MINI);
            linkFAB.setLabelText("Link to Aspect");
            linkFAB.setImageResource(R.drawable.ic_link_white_24dp);
            linkFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    modifyAspectLink(selectedAspect);
                }
            });
            fam.addMenuButton(linkFAB);

            final FloatingActionButton mapFAB = new FloatingActionButton(context);
            mapFAB.setButtonSize(FloatingActionButton.SIZE_MINI);
            mapFAB.setLabelText("Map to Aspect");
            mapFAB.setImageResource(R.drawable.ic_linear_scale_white_24dp);
            mapFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mapToAspect(selectedAspect);
                }
            });
            fam.addMenuButton(mapFAB);

            final FloatingActionButton removeFAB = new FloatingActionButton(context);
            removeFAB.setButtonSize(FloatingActionButton.SIZE_MINI);
            removeFAB.setLabelText("Remove Aspect");
            removeFAB.setImageResource(R.drawable.ic_clear_white_24dp);
            removeFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAspect(selectedAspect);
                }
            });
            fam.addMenuButton(removeFAB);

            fam.open(true);
        } else {
            fam.close(true);
        }
    }

    private void modifyAspectLink(final Aspect selectedAspect) {
        showAspectListDialog(ASPECT_LIST_DIALOG_TYPE_LINK, new AspectListOnClickListener() {
            @Override
            public void onClick(Aspect aspect, boolean add) {
                if(add) {
                    createAspectLink(selectedAspect, aspect);
                } else {
                    removeAspectLink(selectedAspect, aspect);
                }
            }
        });
    }

    private void mapToAspect(final Aspect selectedAspect) {
        showAspectListDialog(ASPECT_LIST_DIALOG_TYPE_MAP, new AspectListOnClickListener() {
            @Override
            public void onClick(Aspect aspect, boolean ignoreMe) {
                Dijkstra dijkstra = new Dijkstra();
                dijkstra.execute(selectedAspect);
                LinkedList<Aspect> path = dijkstra.getPath(aspect);

                if(path != null) {
                    showAspectLinkDialog(path);
                } else {
                    Toast.makeText(MainActivity.this, "No path found", Toast.LENGTH_LONG).show();
                }

                fam.close(true);
            }
        });
    }

    private void removeAspect(final Aspect selectedAspect) {
        final ArrayList<Aspect> aspectsThatLinkedToSelectedAspect = new ArrayList<>();

        synchronized (MainActivity.graphAspects) {
            for (Aspect graphAspect : MainActivity.graphAspects) {
                if(graphAspect.getLinkedAspectIds().contains(new Integer(selectedAspect.getId()))) {
                    graphAspect.getLinkedAspectIds().remove(new Integer(selectedAspect.getId()));
                    selectedAspect.getLinkedAspectIds().remove(new Integer(graphAspect.getId()));

                    aspectsThatLinkedToSelectedAspect.add(graphAspect);
                }
            }

            MainActivity.graphAspects.remove(selectedAspect);
        }

        fam.close(true);

        Snackbar.make(getWindow().getDecorView(), "Removed " + selectedAspect.getName(), Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (Aspect aspect : aspectsThatLinkedToSelectedAspect) {
                            aspect.getLinkedAspectIds().add(new Integer(selectedAspect.getId()));
                            selectedAspect.getLinkedAspectIds().add(new Integer(aspect.getId()));
                        }

                        MainActivity.graphAspects.add(selectedAspect);

                        MainActivity.selectedAspect = selectedAspect;
                        if(MainActivity.selectedAspect != null) {
                            toggleAspectMenu(MainActivity.selectedAspect, true);
                        }
                    }

                })
                .setCallback(getUndoSnackbarCallback())
                .show();
    }

    public void createAspectLink(final Aspect originAspect, final Aspect destinationAspect) {
        if(!(originAspect.getLinkedAspectIds().contains(destinationAspect.getId()) || destinationAspect.getLinkedAspectIds().contains(originAspect.getId()))) {
            boolean added = false;
            synchronized (MainActivity.graphAspects) {
                if (!MainActivity.graphAspects.contains(destinationAspect)) {
                    MainActivity.graphAspects.add(destinationAspect);
                    added = true;
                }
            }

            originAspect.getLinkedAspectIds().add(new Integer(destinationAspect.getId()));
            destinationAspect.getLinkedAspectIds().add(new Integer(originAspect.getId()));

            final boolean finalAdded = added;
            Snackbar.make(getWindow().getDecorView(), "Linked " + originAspect.getName() + " to " + destinationAspect.getName(), Snackbar.LENGTH_LONG)
                    .setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(finalAdded) {
                                synchronized (MainActivity.graphAspects) {
                                    MainActivity.graphAspects.remove(destinationAspect);
                                }
                            }

                            synchronized (originAspect.getLinkedAspectIds()) {
                                originAspect.getLinkedAspectIds().remove(new Integer(destinationAspect.getId()));
                            }

                            synchronized (destinationAspect.getLinkedAspectIds()) {
                                destinationAspect.getLinkedAspectIds().remove(new Integer(originAspect.getId()));
                            }
                        }

                    })
                    .setCallback(getUndoSnackbarCallback())
                    .show();
        }
    }

    @NonNull
    private Snackbar.Callback getUndoSnackbarCallback() {
        return new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);

                if(event == Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    saveAspects(false);
                }
            }
        };
    }

    public void removeAspectLink(final Aspect originAspect, final Aspect destinationAspect) {
        originAspect.getLinkedAspectIds().remove(new Integer(destinationAspect.getId()));
        destinationAspect.getLinkedAspectIds().remove(new Integer(originAspect.getId()));

        Snackbar.make(getWindow().getDecorView(), "Unlinked " + originAspect.getName() + " from " + destinationAspect.getName(), Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        originAspect.getLinkedAspectIds().add(new Integer(destinationAspect.getId()));
                        destinationAspect.getLinkedAspectIds().add(new Integer(originAspect.getId()));
                    }

                })
                .setCallback(getUndoSnackbarCallback())
                .show();

//        fam.close(true);
    }

    public static ArrayList<Aspect> getAspectList() {
        if(aspectList == null) {
            aspectList = generateAspectList();
        }

        return aspectList;
    }

    public static Aspect getAspectById(int id) {
        for (Aspect aspect : aspectList) {
            if(aspect.getId() == id) {
                return aspect;
            }
        }

        return null;
    }

    private static ArrayList<Aspect> generateAspectList() {
        ArrayList<Aspect> aspects = new ArrayList<>();

        aspects.add(new Aspect(1, R.drawable.aer, 1, 1, "Aer"));
        aspects.add(new Aspect(2, R.drawable.alienis, 1, 4, "Alienis"));
        aspects.add(new Aspect(3, R.drawable.aqua, 7, 1, "Aqua"));
        aspects.add(new Aspect(4, R.drawable.arbor, 10, 4, "Arbor"));
        aspects.add(new Aspect(5, R.drawable.auram, 12, 4, "Auram"));
        aspects.add(new Aspect(6, R.drawable.bestia, 4, 3, "Bestia"));
        aspects.add(new Aspect(7, R.drawable.cognitio, 8, 5, "Cognitio"));
        aspects.add(new Aspect(8, R.drawable.corpus, 4, 4, "Corpus"));
        aspects.add(new Aspect(9, R.drawable.exanimis, 6, 4, "Exanimis"));
        aspects.add(new Aspect(10, R.drawable.fabrico, 5, 8, "Fabrico"));
        aspects.add(new Aspect(11, R.drawable.fames, 14, 3, "Fames"));
        aspects.add(new Aspect(12, R.drawable.gelum, 13, 2, "Gelum"));
        aspects.add(new Aspect(13, R.drawable.herba, 10, 3, "Herba"));
        aspects.add(new Aspect(14, R.drawable.humanus, 8, 6, "Humanus"));
        aspects.add(new Aspect(15, R.drawable.ignis, 12, 1, "Ignis"));
        aspects.add(new Aspect(16, R.drawable.instrumentum, 7, 7, "Instrumentum"));
        aspects.add(new Aspect(17, R.drawable.iter, 5, 3, "Iter"));
        aspects.add(new Aspect(18, R.drawable.limus, 8, 3, "Limus"));
        aspects.add(new Aspect(19, R.drawable.lucrum, 6, 7, "Lucrum"));
        aspects.add(new Aspect(20, R.drawable.lux, 1, 2, "Lux"));
        aspects.add(new Aspect(21, R.drawable.machina, 6, 8, "Machina"));
        aspects.add(new Aspect(22, R.drawable.messis, 10, 7, "Messis"));
        aspects.add(new Aspect(23, R.drawable.metallum, 11, 3, "Metallum"));
        aspects.add(new Aspect(24, R.drawable.meto, 10, 8, "Meto"));
        aspects.add(new Aspect(25, R.drawable.mortuus, 7, 3, "Mortuus"));
        aspects.add(new Aspect(26, R.drawable.motus, 4, 2, "Motus"));
        aspects.add(new Aspect(27, R.drawable.ordo, 4, 1, "Ordo"));
        aspects.add(new Aspect(28, R.drawable.pannus, 7, 8, "Pannus"));
        aspects.add(new Aspect(29, R.drawable.perditio, 14, 1, "Perditio"));
        aspects.add(new Aspect(30, R.drawable.perfodio, 8, 7, "Perfodio"));
        aspects.add(new Aspect(31, R.drawable.permutatio, 3, 2, "Permutatio"));
        aspects.add(new Aspect(32, R.drawable.potentia, 12, 2, "Potentia"));
        aspects.add(new Aspect(33, R.drawable.praecantatio, 12, 3, "Praecantatio"));
        aspects.add(new Aspect(34, R.drawable.sano, 9, 3, "Sano"));
        aspects.add(new Aspect(35, R.drawable.sensus, 7, 5, "Sensus"));
        aspects.add(new Aspect(36, R.drawable.spiritus, 7, 4, "Spiritus"));
        aspects.add(new Aspect(37, R.drawable.telum, 8, 8, "Telum"));
        aspects.add(new Aspect(38, R.drawable.tempestas, 6, 2, "Tempestas"));
        aspects.add(new Aspect(39, R.drawable.tenebrae, 1, 3, "Tenebrae"));
        aspects.add(new Aspect(40, R.drawable.terra, 10, 1, "Terra"));
        aspects.add(new Aspect(41, R.drawable.tutamen, 9, 8, "Tutamen"));
        aspects.add(new Aspect(42, R.drawable.vacuos, 14, 2, "Vacuos"));
        aspects.add(new Aspect(43, R.drawable.venenum, 7, 2, "Venenum"));
        aspects.add(new Aspect(44, R.drawable.victus, 9, 2, "Victus"));
        aspects.add(new Aspect(45, R.drawable.vinculum, 3, 3, "Vinculum"));
        aspects.add(new Aspect(46, R.drawable.vitium, 13, 4, "Vitium"));
        aspects.add(new Aspect(47, R.drawable.vitreus, 11, 2, "Vitreus"));
        aspects.add(new Aspect(48, R.drawable.volatus, 2, 3, "Volatus"));

        return aspects;
    }

    private ArrayList<Aspect> cloneAspectList(ArrayList<Aspect> aspectList) {
        String cloneJson = new Gson().toJson(aspectList);
        return new Gson().fromJson(cloneJson, new TypeToken<ArrayList<Aspect>>() {}.getType());
    }
}