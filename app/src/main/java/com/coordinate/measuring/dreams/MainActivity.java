package com.coordinate.measuring.dreams;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "CMD";
    private static final int REQUEST_OPEN_MODEL = 42;
    private static final int MODEL_MAX_BYTES = 24 * 1024 * 1024;

    private final int bg = Color.rgb(18, 16, 23);
    private final int surface = Color.rgb(29, 23, 36);
    private final int surface2 = Color.rgb(42, 32, 51);
    private final int pink = Color.rgb(214, 107, 160);
    private final int text = Color.rgb(247, 236, 243);
    private final int muted = Color.rgb(188, 170, 188);

    private LinearLayout root;
    private VisualiserView visualiser;
    private ModelMeasureView modelView;
    private TextView modelStatus;
    private SeekBar xySeek;
    private SeekBar zSeek;
    private TextView xyValue;
    private TextView zValue;
    private EditText xyInput;
    private EditText zInput;
    private EditText iInput;
    private EditText jInput;
    private EditText kInput;
    private boolean editing = false;
    private boolean inModelMeasure = false;
    private boolean inIsoTolerance = false;
    private boolean inTrigCalculator = false;
    private boolean inProbeAngle = false;

    private double xyDeg = 45.0;
    private double zDeg = 20.0;
    private double i = 0, j = 0, k = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void setBaseRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(bg);
        setContentView(root);
    }

    private void showHome() {
        visualiser = null;
        modelView = null;
        inModelMeasure = false;
        inIsoTolerance = false;
        inTrigCalculator = false;
        inProbeAngle = false;
        setBaseRoot();
        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("ic_cmd_logo", "drawable", getPackageName()));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(92), dp(92));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logo, logoLp);

        TextView title = title("CMD");
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView subtitle = label("Coordinate Measuring Dreams\nDaily CMM programming tools");
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        Button ijk = button("IJK Angle Visualiser");
        ijk.setOnClickListener(v -> showIJK());
        root.addView(ijk, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        Button modelMeasure = button("Model Measure");
        modelMeasure.setOnClickListener(v -> showModelMeasure());
        LinearLayout.LayoutParams modelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        modelLp.setMargins(0, dp(10), 0, 0);
        root.addView(modelMeasure, modelLp);

        Button tolerances = button("ISO Tolerance Tables");
        tolerances.setOnClickListener(v -> showIsoTolerances());
        LinearLayout.LayoutParams tolLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        tolLp.setMargins(0, dp(10), 0, 0);
        root.addView(tolerances, tolLp);

        Button trig = button("Trigonometry Calculator");
        trig.setOnClickListener(v -> showTrigonometry());
        LinearLayout.LayoutParams trigLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        trigLp.setMargins(0, dp(10), 0, 0);
        root.addView(trig, trigLp);

        Button probe = button("Probe Angle Calculator");
        probe.setOnClickListener(v -> showProbeAngle());
        LinearLayout.LayoutParams probeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        probeLp.setMargins(0, dp(10), 0, 0);
        root.addView(probe, probeLp);

        TextView more = label("Import OBJ/STL models for quick measurements, visualise IJK angles, check ISO limits and fits, solve shop trigonometry, or check probe/stylus clearance angles.");
        more.setPadding(0, dp(18), 0, 0);
        root.addView(more);
    }

    private void showIJK() {
        inModelMeasure = false;
        inIsoTolerance = false;
        inTrigCalculator = false;
        inProbeAngle = false;
        setBaseRoot();
        Button back = button("← Back to tools");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView title = title("IJK Angle Visualiser");
        root.addView(title);

        visualiser = new VisualiserView(this);
        visualiser.setBackgroundColor(surface);
        root.addView(visualiser, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        ScrollView scroll = new ScrollView(this);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, dp(10), 0, 0);
        scroll.addView(controls);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(290)));

        xyValue = label("");
        controls.addView(xyValue);
        xySeek = new SeekBar(this);
        xySeek.setMax(3600);
        controls.addView(xySeek);

        zValue = label("");
        controls.addView(zValue);
        zSeek = new SeekBar(this);
        zSeek.setMax(1800);
        controls.addView(zSeek);

        LinearLayout rowAngles = row();
        xyInput = numeric("XY°");
        zInput = numeric("Z°");
        rowAngles.addView(wrap("XY angle", xyInput), weight());
        rowAngles.addView(wrap("Z/elevation", zInput), weight());
        controls.addView(rowAngles);

        LinearLayout rowIjk = row();
        iInput = numeric("I");
        jInput = numeric("J");
        kInput = numeric("K");
        rowIjk.addView(wrap("I", iInput), weight());
        rowIjk.addView(wrap("J", jInput), weight());
        rowIjk.addView(wrap("K", kInput), weight());
        controls.addView(rowIjk);

        TextView hint = label("Drag the 3D panel to orbit around the red/green/blue origin triad. Type existing IJK values to normalise and visualise them.");
        hint.setPadding(0, dp(8), 0, 0);
        controls.addView(hint);

        xySeek.setOnSeekBarChangeListener(seekListener(true));
        zSeek.setOnSeekBarChangeListener(seekListener(false));
        addAngleWatcher(xyInput, true);
        addAngleWatcher(zInput, false);
        addIjkWatcher(iInput);
        addIjkWatcher(jInput);
        addIjkWatcher(kInput);

        updateFromAngles(false);
    }

    private void showIsoTolerances() {
        visualiser = null;
        modelView = null;
        inModelMeasure = false;
        inIsoTolerance = true;
        setBaseRoot();

        Button back = button("← Back to tools");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView title = title("ISO Tolerance Tables");
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(controls);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = label("Pick whether you are checking a hole or shaft, enter the nominal size in mm, then enter a tolerance such as H7, g6, JS11 or za12. Covers ISO lookup-table positions A–ZC/a–zc where ISO 286 defines the size/grade combination.");
        hint.setPadding(0, 0, 0, dp(10));
        controls.addView(hint);

        final boolean[] holeMode = new boolean[] { true };
        LinearLayout modeRow = row();
        Button holeButton = button("Hole");
        Button shaftButton = button("Shaft");
        modeRow.addView(holeButton, weight());
        modeRow.addView(shaftButton, weight());
        controls.addView(modeRow);

        LinearLayout inputRow = row();
        EditText sizeInput = numeric("e.g. 25");
        EditText fitInput = textInput("e.g. H7");
        inputRow.addView(wrap("Nominal size mm", sizeInput), weight());
        inputRow.addView(wrap("Tolerance", fitInput), weight());
        controls.addView(inputRow);

        TextView result = label("");
        result.setTextSize(16);
        result.setPadding(0, dp(12), 0, dp(8));
        controls.addView(result);

        Button calculate = button("Calculate tolerance");
        LinearLayout.LayoutParams calcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        calcLp.setMargins(0, dp(8), 0, 0);
        controls.addView(calculate, calcLp);

        TextView notes = label("Table note: uses embedded ISO 286 IT grade and fundamental-deviation lookup tables, including IT01, IT0 and IT1–IT18. If a size/grade/position is not defined by the table, CMD warns instead of inventing a value.");
        notes.setPadding(0, dp(14), 0, 0);
        controls.addView(notes);

        Runnable updateMode = () -> {
            holeButton.setText(holeMode[0] ? "✓ Hole" : "Hole");
            shaftButton.setText(holeMode[0] ? "Shaft" : "✓ Shaft");
        };
        holeButton.setOnClickListener(v -> { holeMode[0] = true; updateMode.run(); });
        shaftButton.setOnClickListener(v -> { holeMode[0] = false; updateMode.run(); });
        updateMode.run();

        calculate.setOnClickListener(v -> result.setText(calculateTolerance(sizeInput.getText().toString(), fitInput.getText().toString(), holeMode[0])));
        sizeInput.setText("25");
        fitInput.setText("H7");
        result.setText(calculateTolerance("25", "H7", true));
    }

    private String calculateTolerance(String sizeText, String designationText, boolean holeMode) {
        Double nominal = parse(sizeText);
        if (nominal == null || nominal <= 0 || nominal > 3150) {
            return "Enter a nominal size from 0 to 3150 mm.";
        }
        String raw = designationText == null ? "" : designationText.trim();
        if (raw.length() < 2) return "Enter a tolerance such as H7, g6, JS11 or za12.";

        int split = 0;
        while (split < raw.length() && Character.isLetter(raw.charAt(split))) split++;
        if (split == 0 || split >= raw.length()) return "Use letters plus grade, for example H7, g6 or JS11.";
        String letters = raw.substring(0, split);
        String gradeText = raw.substring(split).toUpperCase(Locale.UK);
        int itIndex = toleranceGradeIndex(gradeText);
        if (itIndex < 0) return "Grade must be IT01, IT0, or IT1 to IT18, for example H7 or js11.";

        boolean typedHole = Character.isUpperCase(letters.charAt(0));
        if (!letters.equalsIgnoreCase("js") && typedHole != holeMode) {
            String corrected = holeMode ? letters.toUpperCase(Locale.UK) : letters.toLowerCase(Locale.UK);
            return "Hole/Shaft selection does not match " + raw + ". Use " + corrected + gradeDisplay(itIndex) + " or switch to " + (holeMode ? "Shaft" : "Hole") + ".";
        }

        String pos = letters.equalsIgnoreCase("js") ? (holeMode ? "JS" : "js") : (holeMode ? letters.toUpperCase(Locale.UK) : letters.toLowerCase(Locale.UK));
        double itMicrons = lookupIt(nominal, itIndex);
        if (Double.isNaN(itMicrons)) return "No IT grade table row found for this size.";

        double lowerMicrons;
        double upperMicrons;
        if (holeMode) {
            String dbKey = holeGradeDb(pos, itIndex);
            if (!isSupportedHolePosition(pos)) return "Unsupported hole position " + pos + ".";
            if (isExceptionalHole(dbKey, itIndex, nominal)) return pos + gradeDisplay(itIndex) + " is outside the ISO table range covered for this size/grade.";
            if (pos.equals("JS")) {
                lowerMicrons = -itMicrons / 2.0;
                upperMicrons = itMicrons / 2.0;
            } else {
                double dev = getHoleDeviation(dbKey, itIndex, nominal);
                if (Double.isNaN(dev)) return "No ISO deviation table value for " + pos + gradeDisplay(itIndex) + " at this size.";
                if (isAH(pos)) { lowerMicrons = dev; upperMicrons = dev + itMicrons; }
                else { upperMicrons = dev; lowerMicrons = dev - itMicrons; }
            }
        } else {
            String dbKey = shaftGradeDb(pos, itIndex);
            if (!isSupportedShaftPosition(pos)) return "Unsupported shaft position " + pos + ".";
            if (isExceptionalShaft(dbKey, itIndex, nominal)) return pos + gradeDisplay(itIndex) + " is outside the ISO table range covered for this size/grade.";
            if (pos.equals("js")) {
                lowerMicrons = -itMicrons / 2.0;
                upperMicrons = itMicrons / 2.0;
            } else {
                double dev = getShaftDeviation(dbKey, nominal);
                if (Double.isNaN(dev)) return "No ISO deviation table value for " + pos + gradeDisplay(itIndex) + " at this size.";
                if (pos.compareTo("h") <= 0) { upperMicrons = dev; lowerMicrons = dev - itMicrons; }
                else { lowerMicrons = dev; upperMicrons = dev + itMicrons; }
            }
        }

        double min = nominal + lowerMicrons / 1000.0;
        double max = nominal + upperMicrons / 1000.0;
        return String.format(Locale.UK,
                "%s%s at %.3f mm\nTolerance width: %.1f µm (%.4f mm)\nLower deviation: %+.1f µm (%+.4f mm)\nUpper deviation: %+.1f µm (%+.4f mm)\nMinimum size: %.5f mm\nMaximum size: %.5f mm",
                pos, gradeDisplay(itIndex), nominal, itMicrons, itMicrons / 1000.0,
                lowerMicrons, lowerMicrons / 1000.0, upperMicrons, upperMicrons / 1000.0, min, max);
    }

    private static final String[] IT_KEYS = new String[] {"IT01", "IT0", "IT1", "IT2", "IT3", "IT4", "IT5", "IT6", "IT7", "IT8", "IT9", "IT10", "IT11", "IT12", "IT13", "IT14", "IT15", "IT16", "IT17", "IT18"};
    private static final double[][] ITG_TABLE = new double[][] {
            {0.0, 3.0, 0.2999999999999999, 0.5, 0.8, 1.2, 2.0, 3.0, 4.0, 6.0, 10.0, 14.0, 25.0, 40.0, 60.0, 100.0, 140.0, 250.0, 400.0, 600.0, 1000.0, 1400.0},
            {3.0, 6.0, 0.4, 0.5999999999999999, 1.0, 1.5, 2.5, 4.0, 5.0, 8.0, 12.0, 18.0, 30.0, 48.0, 75.0, 120.0, 180.0, 300.0, 480.0, 750.0, 1200.0, 1800.0},
            {6.0, 10.0, 0.4, 0.5999999999999999, 1.0, 1.5, 2.5, 4.0, 6.0, 9.0, 15.0, 22.0, 36.0, 58.0, 90.0, 150.0, 220.0, 360.0, 580.0, 900.0, 1500.0, 2200.0},
            {10.0, 18.0, 0.5, 0.8, 1.2, 2.0, 3.0, 5.0, 8.0, 11.0, 18.0, 27.0, 43.0, 70.0, 110.0, 180.0, 270.0, 430.0, 700.0, 1100.0, 1800.0, 2700.0},
            {18.0, 30.0, 0.5999999999999999, 1.0, 1.5, 2.5, 4.0, 6.0, 9.0, 13.0, 21.0, 33.0, 52.0, 84.0, 130.0, 210.0, 330.0, 520.0, 840.0, 1300.0, 2100.0, 3300.0},
            {30.0, 50.0, 0.5999999999999999, 1.0, 1.5, 2.5, 4.0, 7.0, 11.0, 16.0, 25.0, 39.0, 62.0, 100.0, 160.0, 250.0, 390.0, 620.0, 1000.0, 1600.0, 2500.0, 3900.0},
            {50.0, 80.0, 0.8, 1.2, 2.0, 3.0, 5.0, 8.0, 13.0, 19.0, 30.0, 46.0, 74.0, 120.0, 190.0, 300.0, 460.0, 740.0, 1200.0, 1900.0, 3000.0, 4600.0},
            {80.0, 120.0, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 15.0, 22.0, 35.0, 54.0, 87.0, 140.0, 220.0, 350.0, 540.0, 870.0, 1400.0, 2200.0, 3500.0, 5400.0},
            {120.0, 180.0, 1.2, 2.0, 3.5, 5.0, 8.0, 12.0, 18.0, 25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0},
            {180.0, 250.0, 2.0, 3.0, 4.5, 7.0, 10.0, 14.0, 20.0, 29.0, 46.0, 72.0, 115.0, 185.0, 290.0, 460.0, 720.0, 1150.0, 1850.0, 2900.0, 4600.0, 7200.0},
            {250.0, 315.0, 2.5, 4.0, 6.0, 8.0, 12.0, 16.0, 23.0, 32.0, 52.0, 81.0, 130.0, 210.0, 320.0, 520.0, 810.0, 1300.0, 2100.0, 3200.0, 5200.0, 8100.0},
            {315.0, 400.0, 3.0, 5.0, 7.0, 9.0, 13.0, 18.0, 25.0, 36.0, 57.0, 89.0, 140.0, 230.0, 360.0, 570.0, 890.0, 1400.0, 2300.0, 3600.0, 5700.0, 8900.0},
            {400.0, 500.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0, 27.0, 40.0, 63.0, 97.0, 155.0, 250.0, 400.0, 630.0, 970.0, 1550.0, 2500.0, 4000.0, 6300.0, 9700.0},
            {500.0, 630.0, Double.NaN, Double.NaN, 9.0, 11.0, 16.0, 22.0, 32.0, 44.0, 70.0, 110.0, 175.0, 280.0, 440.0, 700.0, 1100.0, 1750.0, 2800.0, 4400.0, 7000.0, 11000.0},
            {630.0, 800.0, Double.NaN, Double.NaN, 10.0, 13.0, 18.0, 25.0, 36.0, 50.0, 80.0, 125.0, 200.0, 320.0, 500.0, 800.0, 1250.0, 2000.0, 3200.0, 5000.0, 8000.0, 12500.0},
            {800.0, 1000.0, Double.NaN, Double.NaN, 11.0, 15.0, 21.0, 28.0, 40.0, 56.0, 90.0, 140.0, 230.0, 360.0, 560.0, 900.0, 1400.0, 2300.0, 3600.0, 5600.0, 9000.0, 14000.0},
            {1000.0, 1250.0, Double.NaN, Double.NaN, 13.0, 18.0, 24.0, 33.0, 47.0, 66.0, 105.0, 165.0, 260.0, 420.0, 660.0, 1050.0, 1650.0, 2600.0, 4200.0, 6600.0, 10500.0, 16500.0},
            {1250.0, 1600.0, Double.NaN, Double.NaN, 15.0, 21.0, 29.0, 39.0, 55.0, 78.0, 125.0, 195.0, 310.0, 500.0, 780.0, 1250.0, 1950.0, 3100.0, 5000.0, 7800.0, 12500.0, 19500.0},
            {1600.0, 2000.0, Double.NaN, Double.NaN, 18.0, 25.0, 35.0, 46.0, 65.0, 92.0, 150.0, 230.0, 370.0, 600.0, 920.0, 1500.0, 2300.0, 3700.0, 6000.0, 9200.0, 15000.0, 23000.0},
            {2000.0, 2500.0, Double.NaN, Double.NaN, 22.0, 30.0, 41.0, 55.0, 78.0, 110.0, 175.0, 280.0, 440.0, 700.0, 1100.0, 1750.0, 2800.0, 4400.0, 7000.0, 11000.0, 17500.0, 28000.0},
            {2500.0, 3150.0, Double.NaN, Double.NaN, 26.0, 36.0, 50.0, 68.0, 96.0, 135.0, 210.0, 330.0, 540.0, 860.0, 1350.0, 2100.0, 3300.0, 5400.0, 8600.0, 13500.0, 21000.0, 33000.0}
    };
    private static final String[] HOLE_KEYS = new String[] {"A", "B", "C", "CD", "D", "E", "EF", "F", "FG", "G", "H", "J6", "J7", "J8", "K_IT1_IT8", "K_IT9_IT18", "M_IT1_IT8", "M_IT9_IT18", "N_IT1_IT8", "N_IT9_IT18", "P", "R", "S", "T", "U", "V", "X", "Y", "Z", "ZA", "ZB", "ZC"};
    private static final double[][] HOLE_DEV_TABLE = new double[][] {
            {18.0, 24.0, 300.0, 160.0, 110.0, 85.0, 65.0, 40.0, 28.0, 20.0, 12.0, 7.0, 0.0, 8.0, 12.0, 20.0, -2.0, Double.NaN, -8.0, -8.0, -15.0, 0.0, -22.0, -28.0, -35.0, Double.NaN, -41.0, -47.0, -54.0, -63.0, -73.0, -98.0, -136.0, -188.0},
            {0.0, 3.0, 270.0, 140.0, 60.0, 34.0, 20.0, 14.0, 10.0, 6.0, 4.0, 2.0, 0.0, 2.0, 4.0, 6.0, 0.0, 0.0, -2.0, -2.0, -4.0, -4.0, -6.0, -10.0, -14.0, Double.NaN, -18.0, Double.NaN, -20.0, Double.NaN, -26.0, -32.0, -40.0, -60.0},
            {3.0, 6.0, 270.0, 140.0, 70.0, 46.0, 30.0, 20.0, 14.0, 10.0, 6.0, 4.0, 0.0, 5.0, 6.0, 10.0, -1.0, Double.NaN, -4.0, -4.0, -8.0, 0.0, -12.0, -15.0, -19.0, Double.NaN, -23.0, Double.NaN, -28.0, Double.NaN, -35.0, -42.0, -50.0, -80.0},
            {6.0, 10.0, 280.0, 150.0, 80.0, 56.0, 40.0, 25.0, 18.0, 13.0, 8.0, 5.0, 0.0, 5.0, 8.0, 12.0, -1.0, Double.NaN, -6.0, -6.0, -10.0, 0.0, -15.0, -19.0, -23.0, Double.NaN, -28.0, Double.NaN, -34.0, Double.NaN, -42.0, -52.0, -67.0, -97.0},
            {10.0, 14.0, 290.0, 150.0, 95.0, 70.0, 50.0, 32.0, 23.0, 16.0, 10.0, 6.0, 0.0, 6.0, 10.0, 15.0, -1.0, Double.NaN, -7.0, -7.0, -12.0, 0.0, -18.0, -23.0, -28.0, Double.NaN, -33.0, Double.NaN, -40.0, Double.NaN, -50.0, -64.0, -90.0, -130.0},
            {14.0, 18.0, 290.0, 150.0, 95.0, 70.0, 50.0, 32.0, 23.0, 16.0, 10.0, 6.0, 0.0, 6.0, 10.0, 15.0, -1.0, Double.NaN, -7.0, -7.0, -12.0, 0.0, -18.0, -23.0, -28.0, Double.NaN, -33.0, -39.0, -45.0, Double.NaN, -60.0, -77.0, -108.0, -150.0},
            {24.0, 30.0, 300.0, 160.0, 110.0, 85.0, 65.0, 40.0, 28.0, 20.0, 12.0, 7.0, 0.0, 8.0, 12.0, 20.0, -2.0, Double.NaN, -8.0, -8.0, -15.0, 0.0, -22.0, -28.0, -35.0, -41.0, -48.0, -55.0, -64.0, -75.0, -88.0, -118.0, -160.0, -218.0},
            {30.0, 40.0, 310.0, 170.0, 120.0, 100.0, 80.0, 50.0, 35.0, 25.0, 15.0, 9.0, 0.0, 10.0, 14.0, 24.0, -2.0, Double.NaN, -9.0, -9.0, -17.0, 0.0, -26.0, -34.0, -43.0, -48.0, -60.0, -68.0, -80.0, -94.0, -112.0, -148.0, -200.0, -274.0},
            {40.0, 50.0, 320.0, 180.0, 130.0, 100.0, 80.0, 50.0, 35.0, 25.0, 15.0, 9.0, 0.0, 10.0, 14.0, 24.0, -2.0, Double.NaN, -9.0, -9.0, -17.0, 0.0, -26.0, -34.0, -43.0, -54.0, -70.0, -81.0, -97.0, -114.0, -136.0, -180.0, -242.0, -325.0},
            {50.0, 65.0, 340.0, 190.0, 140.0, Double.NaN, 100.0, 60.0, Double.NaN, 30.0, Double.NaN, 10.0, 0.0, 13.0, 18.0, 28.0, -2.0, Double.NaN, -11.0, -11.0, -20.0, 0.0, -32.0, -41.0, -53.0, -66.0, -87.0, -102.0, -122.0, -144.0, -172.0, -226.0, -300.0, -405.0},
            {65.0, 80.0, 360.0, 200.0, 150.0, Double.NaN, 100.0, 60.0, Double.NaN, 30.0, Double.NaN, 10.0, 0.0, 13.0, 18.0, 28.0, -2.0, Double.NaN, -11.0, -11.0, -20.0, 0.0, -32.0, -43.0, -59.0, -75.0, -102.0, -120.0, -146.0, -174.0, -210.0, -274.0, -360.0, -480.0},
            {80.0, 100.0, 380.0, 220.0, 170.0, Double.NaN, 120.0, 72.0, Double.NaN, 36.0, Double.NaN, 12.0, 0.0, 16.0, 22.0, 34.0, -3.0, Double.NaN, -13.0, -13.0, -23.0, 0.0, -37.0, -51.0, -71.0, -91.0, -124.0, -146.0, -178.0, -214.0, -258.0, -335.0, -445.0, -585.0},
            {100.0, 120.0, 410.0, 240.0, 180.0, Double.NaN, 120.0, 72.0, Double.NaN, 36.0, Double.NaN, 12.0, 0.0, 16.0, 22.0, 34.0, -3.0, Double.NaN, -13.0, -13.0, -23.0, 0.0, -37.0, -54.0, -79.0, -104.0, -144.0, -172.0, -210.0, -254.0, -310.0, -400.0, -525.0, -690.0},
            {120.0, 140.0, 460.0, 260.0, 200.0, Double.NaN, 145.0, 85.0, Double.NaN, 43.0, Double.NaN, 14.0, 0.0, 18.0, 26.0, 41.0, -3.0, Double.NaN, -15.0, -15.0, -27.0, 0.0, -43.0, -63.0, -92.0, -122.0, -170.0, -202.0, -248.0, -300.0, -365.0, -470.0, -620.0, -800.0},
            {140.0, 160.0, 520.0, 280.0, 210.0, Double.NaN, 145.0, 85.0, Double.NaN, 43.0, Double.NaN, 14.0, 0.0, 18.0, 26.0, 41.0, -3.0, Double.NaN, -15.0, -15.0, -27.0, 0.0, -43.0, -65.0, -100.0, -134.0, -190.0, -228.0, -280.0, -340.0, -415.0, -535.0, -700.0, -900.0},
            {160.0, 180.0, 580.0, 310.0, 230.0, Double.NaN, 145.0, 85.0, Double.NaN, 43.0, Double.NaN, 14.0, 0.0, 18.0, 26.0, 41.0, -3.0, Double.NaN, -15.0, -15.0, -27.0, 0.0, -43.0, -68.0, -108.0, -146.0, -210.0, -252.0, -310.0, -380.0, -465.0, -600.0, -780.0, -1000.0},
            {180.0, 200.0, 660.0, 340.0, 240.0, Double.NaN, 170.0, 100.0, Double.NaN, 50.0, Double.NaN, 15.0, 0.0, 22.0, 30.0, 47.0, -4.0, Double.NaN, -17.0, -17.0, -31.0, 0.0, -50.0, -77.0, -122.0, -166.0, -236.0, -284.0, -350.0, -425.0, -520.0, -670.0, -880.0, -1150.0},
            {200.0, 225.0, 740.0, 380.0, 260.0, Double.NaN, 170.0, 100.0, Double.NaN, 50.0, Double.NaN, 15.0, 0.0, 22.0, 30.0, 47.0, -4.0, Double.NaN, -17.0, -17.0, -31.0, 0.0, -50.0, -80.0, -130.0, -180.0, -258.0, -310.0, -385.0, -470.0, -575.0, -740.0, -960.0, -1250.0},
            {225.0, 250.0, 820.0, 420.0, 280.0, Double.NaN, 170.0, 100.0, Double.NaN, 50.0, Double.NaN, 15.0, 0.0, 22.0, 30.0, 47.0, -4.0, Double.NaN, -17.0, -17.0, -31.0, 0.0, -50.0, -84.0, -140.0, -196.0, -284.0, -340.0, -425.0, -520.0, -640.0, -820.0, -1050.0, -1350.0},
            {250.0, 280.0, 920.0, 480.0, 300.0, Double.NaN, 190.0, 110.0, Double.NaN, 56.0, Double.NaN, 17.0, 0.0, 25.0, 36.0, 55.0, -4.0, Double.NaN, -20.0, -20.0, -34.0, 0.0, -56.0, -94.0, -158.0, -218.0, -315.0, -385.0, -475.0, -580.0, -710.0, -920.0, -1200.0, -1550.0},
            {280.0, 315.0, 1050.0, 540.0, 330.0, Double.NaN, 190.0, 110.0, Double.NaN, 56.0, Double.NaN, 17.0, 0.0, 25.0, 36.0, 55.0, -4.0, Double.NaN, -20.0, -20.0, -34.0, 0.0, -56.0, -98.0, -170.0, -240.0, -350.0, -425.0, -525.0, -650.0, -790.0, -1000.0, -1300.0, -1700.0},
            {315.0, 355.0, 1200.0, 600.0, 360.0, Double.NaN, 210.0, 125.0, Double.NaN, 62.0, Double.NaN, 18.0, 0.0, 29.0, 39.0, 60.0, -4.0, Double.NaN, -21.0, -21.0, -37.0, 0.0, -62.0, -108.0, -190.0, -268.0, -390.0, -475.0, -590.0, -730.0, -900.0, -1150.0, -1500.0, -1900.0},
            {355.0, 400.0, 1350.0, 680.0, 400.0, Double.NaN, 210.0, 125.0, Double.NaN, 62.0, Double.NaN, 18.0, 0.0, 29.0, 39.0, 60.0, -4.0, Double.NaN, -21.0, -21.0, -37.0, 0.0, -62.0, -114.0, -208.0, -294.0, -435.0, -530.0, -660.0, -820.0, -1000.0, -1300.0, -1650.0, -2100.0},
            {400.0, 450.0, 1500.0, 760.0, 440.0, Double.NaN, 230.0, 135.0, Double.NaN, 68.0, Double.NaN, 20.0, 0.0, 33.0, 43.0, 66.0, -5.0, Double.NaN, -23.0, -23.0, -40.0, 0.0, -68.0, -126.0, -232.0, -330.0, -490.0, -595.0, -740.0, -920.0, -1100.0, -1450.0, -1850.0, -2400.0},
            {450.0, 500.0, 1650.0, 840.0, 480.0, Double.NaN, 230.0, 135.0, Double.NaN, 68.0, Double.NaN, 20.0, 0.0, 33.0, 43.0, 66.0, -5.0, Double.NaN, -23.0, -23.0, -40.0, 0.0, -68.0, -132.0, -252.0, -360.0, -540.0, -660.0, -820.0, -1000.0, -1250.0, -1600.0, -2100.0, -2600.0},
            {500.0, 560.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 260.0, 145.0, Double.NaN, 76.0, Double.NaN, 22.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -26.0, -26.0, -44.0, -44.0, -78.0, -150.0, -280.0, -400.0, -600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {560.0, 630.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 260.0, 145.0, Double.NaN, 76.0, Double.NaN, 22.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -26.0, -26.0, -44.0, -44.0, -78.0, -155.0, -310.0, -450.0, -660.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {630.0, 710.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 290.0, 160.0, Double.NaN, 80.0, Double.NaN, 24.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -30.0, -30.0, -50.0, -50.0, -88.0, -175.0, -340.0, -500.0, -740.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {710.0, 800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 290.0, 160.0, Double.NaN, 80.0, Double.NaN, 24.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -30.0, -30.0, -50.0, -50.0, -88.0, -185.0, -380.0, -560.0, -840.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {800.0, 900.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 320.0, 170.0, Double.NaN, 86.0, Double.NaN, 26.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -34.0, -34.0, -56.0, -56.0, -100.0, -210.0, -430.0, -620.0, -940.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {900.0, 1000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 320.0, 170.0, Double.NaN, 86.0, Double.NaN, 26.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -34.0, -34.0, -56.0, -56.0, -100.0, -220.0, -470.0, -680.0, -1050.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1000.0, 1120.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 350.0, 195.0, Double.NaN, 98.0, Double.NaN, 28.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -40.0, -40.0, -66.0, -66.0, -120.0, -250.0, -520.0, -780.0, -1150.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1120.0, 1250.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 350.0, 195.0, Double.NaN, 98.0, Double.NaN, 28.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -40.0, -40.0, -66.0, -66.0, -120.0, -260.0, -580.0, -840.0, -1300.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1250.0, 1400.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 390.0, 220.0, Double.NaN, 110.0, Double.NaN, 30.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -48.0, -48.0, -78.0, -78.0, -140.0, -300.0, -640.0, -960.0, -1450.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1400.0, 1600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 390.0, 220.0, Double.NaN, 110.0, Double.NaN, 30.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -48.0, -48.0, -78.0, -78.0, -140.0, -330.0, -720.0, -1050.0, -1600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1600.0, 1800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 430.0, 240.0, Double.NaN, 120.0, Double.NaN, 32.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -58.0, -58.0, -92.0, -92.0, -170.0, -370.0, -820.0, -1200.0, -1850.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1800.0, 2000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 430.0, 240.0, Double.NaN, 120.0, Double.NaN, 32.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -58.0, -58.0, -92.0, -92.0, -170.0, -400.0, -920.0, -1350.0, -2000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2000.0, 2240.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 480.0, 260.0, Double.NaN, 130.0, Double.NaN, 34.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -68.0, -68.0, -110.0, -110.0, -195.0, -440.0, -1000.0, -1500.0, -2300.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2240.0, 2500.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 480.0, 260.0, Double.NaN, 130.0, Double.NaN, 34.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -68.0, -68.0, -110.0, -110.0, -195.0, -460.0, -1100.0, -1650.0, -2500.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2500.0, 2800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 520.0, 290.0, Double.NaN, 145.0, Double.NaN, 38.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -76.0, -76.0, -135.0, -135.0, -240.0, -550.0, -1250.0, -1900.0, -2900.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2800.0, 3150.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 520.0, 290.0, Double.NaN, 145.0, Double.NaN, 38.0, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0, Double.NaN, -76.0, -76.0, -135.0, -135.0, -240.0, -580.0, -1400.0, -2100.0, -3200.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN}
    };
    private static final String[] SHAFT_KEYS = new String[] {"a", "b", "c", "cd", "d", "e", "ef", "f", "fg", "g", "h", "j5", "j6", "j7", "j8", "k4_7", "k1_3_8_18", "m", "n", "p", "r", "s", "t", "u", "v", "x", "y", "z", "za", "zb", "zc"};
    private static final double[][] SHAFT_DEV_TABLE = new double[][] {
            {0.0, 3.0, -270.0, -140.0, -60.0, -34.0, -20.0, -14.0, -10.0, -6.0, -4.0, -2.0, 0.0, -2.0, -2.0, -4.0, -6.0, 0.0, 0.0, 2.0, 4.0, 6.0, 10.0, 14.0, Double.NaN, 18.0, Double.NaN, 20.0, Double.NaN, 26.0, 32.0, 40.0, 60.0},
            {3.0, 6.0, -270.0, -140.0, -70.0, -46.0, -30.0, -20.0, -14.0, -10.0, -6.0, -4.0, 0.0, -2.0, -2.0, -4.0, Double.NaN, 1.0, 0.0, 4.0, 8.0, 12.0, 15.0, 19.0, Double.NaN, 23.0, Double.NaN, 28.0, Double.NaN, 35.0, 42.0, 50.0, 80.0},
            {6.0, 10.0, -280.0, -150.0, -80.0, -56.0, -40.0, -25.0, -18.0, -13.0, -8.0, -5.0, 0.0, -2.0, -2.0, -5.0, Double.NaN, 1.0, 0.0, 6.0, 10.0, 15.0, 19.0, 23.0, Double.NaN, 28.0, Double.NaN, 34.0, Double.NaN, 42.0, 52.0, 67.0, 97.0},
            {10.0, 14.0, -290.0, -150.0, -95.0, -70.0, -50.0, -32.0, -23.0, -16.0, -10.0, -6.0, 0.0, -3.0, -3.0, -6.0, Double.NaN, 1.0, 0.0, 7.0, 12.0, 18.0, 23.0, 28.0, Double.NaN, 33.0, Double.NaN, 40.0, Double.NaN, 50.0, 64.0, 90.0, 130.0},
            {14.0, 18.0, -290.0, -150.0, -95.0, -70.0, -50.0, -32.0, -23.0, -16.0, -10.0, -6.0, 0.0, -3.0, -3.0, -6.0, Double.NaN, 1.0, 0.0, 7.0, 12.0, 18.0, 23.0, 28.0, Double.NaN, 33.0, 39.0, 45.0, Double.NaN, 60.0, 77.0, 108.0, 150.0},
            {18.0, 24.0, -300.0, -160.0, -110.0, -85.0, -65.0, -40.0, -25.0, -20.0, -12.0, -7.0, 0.0, -4.0, -4.0, -8.0, Double.NaN, 2.0, 0.0, 8.0, 15.0, 22.0, 28.0, 35.0, Double.NaN, 41.0, 47.0, 54.0, 63.0, 73.0, 98.0, 136.0, 188.0},
            {24.0, 30.0, -300.0, -160.0, -110.0, -85.0, -65.0, -40.0, -25.0, -20.0, -12.0, -7.0, 0.0, -4.0, -4.0, -8.0, Double.NaN, 2.0, 0.0, 8.0, 15.0, 22.0, 28.0, 35.0, 41.0, 48.0, 55.0, 64.0, 75.0, 88.0, 118.0, 160.0, 218.0},
            {30.0, 40.0, -310.0, -170.0, -120.0, -100.0, -80.0, -50.0, -35.0, -25.0, -15.0, -9.0, 0.0, -5.0, -5.0, -10.0, Double.NaN, 2.0, 0.0, 9.0, 17.0, 26.0, 34.0, 43.0, 48.0, 60.0, 68.0, 80.0, 94.0, 112.0, 148.0, 200.0, 274.0},
            {40.0, 50.0, -320.0, -180.0, -130.0, -100.0, -80.0, -50.0, -35.0, -25.0, -15.0, -9.0, 0.0, -5.0, -5.0, -10.0, Double.NaN, 2.0, 0.0, 9.0, 17.0, 26.0, 34.0, 43.0, 54.0, 70.0, 81.0, 97.0, 114.0, 136.0, 180.0, 242.0, 325.0},
            {50.0, 65.0, -340.0, -190.0, -140.0, Double.NaN, -100.0, -60.0, Double.NaN, -30.0, Double.NaN, -10.0, 0.0, -7.0, -7.0, -12.0, Double.NaN, 2.0, 0.0, 11.0, 20.0, 32.0, 41.0, 53.0, 66.0, 87.0, 102.0, 122.0, 144.0, 172.0, 226.0, 300.0, 405.0},
            {65.0, 80.0, -360.0, -200.0, -150.0, Double.NaN, -100.0, -60.0, Double.NaN, -30.0, Double.NaN, -10.0, 0.0, -7.0, -7.0, -12.0, Double.NaN, 2.0, 0.0, 11.0, 20.0, 32.0, 43.0, 59.0, 75.0, 102.0, 120.0, 146.0, 174.0, 210.0, 274.0, 360.0, 480.0},
            {80.0, 100.0, -380.0, -220.0, -170.0, Double.NaN, -120.0, -72.0, Double.NaN, -36.0, Double.NaN, -12.0, 0.0, -9.0, -9.0, -15.0, Double.NaN, 3.0, 0.0, 13.0, 23.0, 37.0, 51.0, 71.0, 91.0, 124.0, 146.0, 178.0, 214.0, 258.0, 335.0, 445.0, 585.0},
            {100.0, 120.0, -410.0, -240.0, -180.0, Double.NaN, -120.0, -72.0, Double.NaN, -36.0, Double.NaN, -12.0, 0.0, -9.0, -9.0, -15.0, Double.NaN, 3.0, 0.0, 13.0, 23.0, 37.0, 54.0, 79.0, 104.0, 144.0, 172.0, 210.0, 254.0, 310.0, 400.0, 525.0, 690.0},
            {120.0, 140.0, -460.0, -260.0, -200.0, Double.NaN, -145.0, -85.0, Double.NaN, -43.0, Double.NaN, -14.0, 0.0, -11.0, -11.0, -18.0, Double.NaN, 3.0, 0.0, 15.0, 27.0, 43.0, 63.0, 92.0, 122.0, 170.0, 202.0, 248.0, 300.0, 365.0, 470.0, 620.0, 800.0},
            {140.0, 160.0, -520.0, -280.0, -210.0, Double.NaN, -145.0, -85.0, Double.NaN, -43.0, Double.NaN, -14.0, 0.0, -11.0, -11.0, -18.0, Double.NaN, 3.0, 0.0, 15.0, 27.0, 43.0, 65.0, 100.0, 134.0, 190.0, 228.0, 280.0, 340.0, 415.0, 535.0, 700.0, 900.0},
            {160.0, 180.0, -580.0, -310.0, -230.0, Double.NaN, -145.0, -85.0, Double.NaN, -43.0, Double.NaN, -14.0, 0.0, -11.0, -11.0, -18.0, Double.NaN, 3.0, 0.0, 15.0, 27.0, 43.0, 68.0, 108.0, 146.0, 210.0, 252.0, 310.0, 380.0, 465.0, 600.0, 780.0, 1000.0},
            {180.0, 200.0, -660.0, -340.0, -240.0, Double.NaN, -170.0, -100.0, Double.NaN, -50.0, Double.NaN, -15.0, 0.0, -13.0, -13.0, -21.0, Double.NaN, 4.0, 0.0, 17.0, 31.0, 50.0, 77.0, 122.0, 166.0, 236.0, 284.0, 350.0, 425.0, 520.0, 670.0, 880.0, 1150.0},
            {200.0, 225.0, -740.0, -380.0, -260.0, Double.NaN, -170.0, -100.0, Double.NaN, -50.0, Double.NaN, -15.0, 0.0, -13.0, -13.0, -21.0, Double.NaN, 4.0, 0.0, 17.0, 31.0, 50.0, 80.0, 130.0, 180.0, 258.0, 310.0, 385.0, 470.0, 575.0, 740.0, 960.0, 1250.0},
            {225.0, 250.0, -820.0, -420.0, -280.0, Double.NaN, -170.0, -100.0, Double.NaN, -50.0, Double.NaN, -15.0, 0.0, -13.0, -13.0, -21.0, Double.NaN, 4.0, 0.0, 17.0, 31.0, 50.0, 84.0, 140.0, 196.0, 284.0, 340.0, 425.0, 520.0, 640.0, 820.0, 1050.0, 1350.0},
            {250.0, 280.0, -920.0, -480.0, -300.0, Double.NaN, -190.0, -110.0, Double.NaN, -56.0, Double.NaN, -17.0, 0.0, -16.0, -16.0, -26.0, Double.NaN, 4.0, 0.0, 20.0, 34.0, 56.0, 94.0, 158.0, 218.0, 315.0, 385.0, 475.0, 580.0, 710.0, 920.0, 1200.0, 1550.0},
            {280.0, 315.0, -1050.0, -540.0, -330.0, Double.NaN, -190.0, -110.0, Double.NaN, -56.0, Double.NaN, -17.0, 0.0, -16.0, -16.0, -26.0, Double.NaN, 4.0, 0.0, 20.0, 34.0, 56.0, 98.0, 170.0, 240.0, 350.0, 425.0, 525.0, 650.0, 790.0, 1000.0, 1300.0, 1700.0},
            {315.0, 355.0, -1200.0, -600.0, -360.0, Double.NaN, -210.0, -125.0, Double.NaN, -62.0, Double.NaN, -18.0, 0.0, -18.0, -18.0, -28.0, Double.NaN, 4.0, 0.0, 21.0, 37.0, 62.0, 108.0, 190.0, 268.0, 390.0, 475.0, 590.0, 730.0, 900.0, 1150.0, 1500.0, 1900.0},
            {355.0, 400.0, -1350.0, -680.0, -400.0, Double.NaN, -210.0, -125.0, Double.NaN, -62.0, Double.NaN, -18.0, 0.0, -18.0, -18.0, -28.0, Double.NaN, 4.0, 0.0, 21.0, 37.0, 62.0, 114.0, 208.0, 294.0, 435.0, 530.0, 660.0, 820.0, 1000.0, 1300.0, 1650.0, 2100.0},
            {400.0, 450.0, -1500.0, -760.0, -440.0, Double.NaN, -230.0, -135.0, Double.NaN, -68.0, Double.NaN, -20.0, 0.0, -20.0, -20.0, -32.0, Double.NaN, 5.0, 0.0, 23.0, 40.0, 68.0, 126.0, 232.0, 330.0, 490.0, 595.0, 740.0, 920.0, 1100.0, 1450.0, 1850.0, 2400.0},
            {450.0, 500.0, -1650.0, -840.0, -480.0, Double.NaN, -230.0, -135.0, Double.NaN, -68.0, Double.NaN, -20.0, 0.0, -20.0, -20.0, -32.0, Double.NaN, 5.0, 0.0, 23.0, 40.0, 68.0, 132.0, 252.0, 360.0, 540.0, 660.0, 820.0, 1000.0, 1250.0, 1600.0, 2100.0, 2600.0},
            {500.0, 560.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -260.0, -145.0, Double.NaN, -76.0, Double.NaN, -22.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 26.0, 44.0, 78.0, 150.0, 280.0, 400.0, 600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {560.0, 630.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -260.0, -145.0, Double.NaN, -76.0, Double.NaN, -22.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 26.0, 44.0, 78.0, 155.0, 310.0, 450.0, 660.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {630.0, 710.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -290.0, -160.0, Double.NaN, -80.0, Double.NaN, -24.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 30.0, 50.0, 88.0, 175.0, 340.0, 500.0, 740.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {710.0, 800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -290.0, -160.0, Double.NaN, -80.0, Double.NaN, -24.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 30.0, 50.0, 88.0, 185.0, 380.0, 560.0, 840.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {800.0, 900.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -320.0, -170.0, Double.NaN, -86.0, Double.NaN, -26.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 34.0, 56.0, 100.0, 210.0, 430.0, 620.0, 940.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {900.0, 1000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -320.0, -170.0, Double.NaN, -86.0, Double.NaN, -26.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 34.0, 56.0, 100.0, 220.0, 470.0, 680.0, 1050.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1000.0, 1120.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -350.0, -195.0, Double.NaN, -98.0, Double.NaN, -28.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 40.0, 66.0, 120.0, 250.0, 520.0, 780.0, 1150.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1120.0, 1250.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -350.0, -195.0, Double.NaN, -98.0, Double.NaN, -28.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 40.0, 66.0, 120.0, 260.0, 580.0, 840.0, 1300.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1250.0, 1400.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -390.0, -220.0, Double.NaN, -110.0, Double.NaN, -30.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 48.0, 78.0, 140.0, 300.0, 640.0, 960.0, 1450.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1400.0, 1600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -390.0, -220.0, Double.NaN, -110.0, Double.NaN, -30.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 48.0, 78.0, 140.0, 330.0, 720.0, 1050.0, 1600.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1600.0, 1800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -430.0, -240.0, Double.NaN, -120.0, Double.NaN, -32.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 58.0, 92.0, 170.0, 370.0, 820.0, 1200.0, 1850.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {1800.0, 2000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -430.0, -240.0, Double.NaN, -120.0, Double.NaN, -32.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 58.0, 92.0, 170.0, 400.0, 920.0, 1350.0, 2000.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2000.0, 2240.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -480.0, -260.0, Double.NaN, -130.0, Double.NaN, -34.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 68.0, 110.0, 195.0, 440.0, 1000.0, 1500.0, 2300.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2240.0, 2500.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -480.0, -260.0, Double.NaN, -130.0, Double.NaN, -34.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 68.0, 110.0, 195.0, 460.0, 1100.0, 1650.0, 2500.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2500.0, 2800.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -520.0, -290.0, Double.NaN, -145.0, Double.NaN, -38.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 76.0, 135.0, 240.0, 550.0, 1250.0, 1900.0, 2900.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
            {2800.0, 3150.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, -520.0, -290.0, Double.NaN, -145.0, Double.NaN, -38.0, 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, 76.0, 135.0, 240.0, 580.0, 1400.0, 2100.0, 3200.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN}
    };
    private static final String[] HOLE_DELTA_KEYS = new String[] {"IT3", "IT4", "IT5", "IT6", "IT7", "IT8"};
    private static final double[][] HOLE_DELTA_TABLE = new double[][] {
            {0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            {3.0, 6.0, 1.0, 1.5, 1.0, 3.0, 4.0, 6.0},
            {6.0, 10.0, 1.0, 1.5, 2.0, 3.0, 6.0, 7.0},
            {10.0, 14.0, 1.0, 2.0, 3.0, 3.0, 7.0, 9.0},
            {14.0, 18.0, 1.0, 2.0, 3.0, 3.0, 7.0, 9.0},
            {18.0, 24.0, 1.5, 2.0, 3.0, 4.0, 8.0, 12.0},
            {24.0, 30.0, 1.5, 2.0, 3.0, 4.0, 8.0, 12.0},
            {30.0, 40.0, 1.5, 3.0, 4.0, 5.0, 9.0, 14.0},
            {40.0, 50.0, 1.5, 3.0, 4.0, 5.0, 9.0, 14.0},
            {50.0, 65.0, 2.0, 3.0, 5.0, 6.0, 11.0, 16.0},
            {65.0, 80.0, 2.0, 3.0, 5.0, 6.0, 11.0, 16.0},
            {80.0, 100.0, 2.0, 4.0, 5.0, 7.0, 13.0, 19.0},
            {100.0, 120.0, 2.0, 4.0, 5.0, 7.0, 13.0, 19.0},
            {120.0, 140.0, 3.0, 4.0, 6.0, 7.0, 15.0, 23.0},
            {140.0, 160.0, 3.0, 4.0, 6.0, 7.0, 15.0, 23.0},
            {160.0, 180.0, 3.0, 4.0, 6.0, 7.0, 15.0, 23.0},
            {180.0, 200.0, 3.0, 4.0, 6.0, 9.0, 17.0, 26.0},
            {200.0, 225.0, 3.0, 4.0, 6.0, 9.0, 17.0, 26.0},
            {225.0, 250.0, 3.0, 4.0, 6.0, 9.0, 17.0, 26.0},
            {250.0, 280.0, 4.0, 4.0, 7.0, 9.0, 20.0, 29.0},
            {280.0, 315.0, 4.0, 4.0, 7.0, 9.0, 20.0, 29.0},
            {315.0, 355.0, 4.0, 5.0, 7.0, 11.0, 21.0, 32.0},
            {355.0, 400.0, 4.0, 5.0, 7.0, 11.0, 21.0, 32.0},
            {400.0, 450.0, 5.0, 5.0, 7.0, 13.0, 23.0, 34.0},
            {450.0, 500.0, 5.0, 5.0, 7.0, 13.0, 23.0, 34.0}
    };

    private int toleranceGradeIndex(String gradeText) {
        String g = gradeText.startsWith("IT") ? gradeText : "IT" + gradeText;
        for (int n = 0; n < IT_KEYS.length; n++) if (IT_KEYS[n].equals(g)) return n;
        return -1;
    }
    private String gradeDisplay(int itIndex) { return IT_KEYS[itIndex].substring(2); }
    private double lookupIt(double size, int itIndex) { double[] row = findRangeRow(ITG_TABLE, size); return row == null ? Double.NaN : row[2 + itIndex]; }
    private double[] findRangeRow(double[][] table, double size) {
        for (double[] row : table) if (size > row[0] && size <= row[1]) return row;
        return null;
    }
    private int keyIndex(String[] keys, String key) { for (int n = 0; n < keys.length; n++) if (keys[n].equals(key)) return n; return -1; }
    private boolean hasKey(String[] keys, String key) { return keyIndex(keys, key) >= 0; }
    private boolean isSupportedHolePosition(String pos) { return pos.equals("JS") || hasKey(HOLE_KEYS, holeGradeDb(pos, 8)); }
    private boolean isSupportedShaftPosition(String pos) { return pos.equals("js") || hasKey(SHAFT_KEYS, shaftGradeDb(pos, 8)); }
    private boolean isAH(String pos) { return pos.length() == 1 && pos.charAt(0) >= 'A' && pos.charAt(0) <= 'H'; }
    private String holeGradeDb(String uiGrade, int itIndex) {
        int it = gradeNumber(itIndex);
        if (uiGrade.equals("J")) { if (it == 6 || it == 7 || it == 8) return "J" + it; return "J"; }
        if (uiGrade.equals("K")) return (it >= 1 && it <= 8) ? "K_IT1_IT8" : "K_IT9_IT18";
        if (uiGrade.equals("M")) return (it >= 1 && it <= 8) ? "M_IT1_IT8" : "M_IT9_IT18";
        if (uiGrade.equals("N")) return (it >= 1 && it <= 8) ? "N_IT1_IT8" : "N_IT9_IT18";
        return uiGrade;
    }
    private String shaftGradeDb(String uiGrade, int itIndex) {
        int it = gradeNumber(itIndex);
        if (uiGrade.equals("j")) { if (it == 5 || it == 6 || it == 7 || it == 8) return "j" + it; return "j"; }
        if (uiGrade.equals("k")) { if (it == 4 || it == 5 || it == 6 || it == 7) return "k4_7"; return "k1_3_8_18"; }
        return uiGrade;
    }
    private int gradeNumber(int itIndex) { if (itIndex == 0) return -1; if (itIndex == 1) return 0; return itIndex - 1; }
    private double getHoleDeviation(String dbKey, int itIndex, double size) {
        double[] row = findRangeRow(HOLE_DEV_TABLE, size); if (row == null) return Double.NaN;
        int idx = keyIndex(HOLE_KEYS, dbKey); if (idx < 0) return Double.NaN;
        double base = row[2 + idx]; if (Double.isNaN(base)) return Double.NaN;
        int it = gradeNumber(itIndex); boolean deltaCondition = false;
        if (dbKey.equals("K_IT1_IT8") || dbKey.equals("M_IT1_IT8") || dbKey.equals("N_IT1_IT8")) deltaCondition = size > 3 && size <= 500;
        else if (dbKey.equals("P") || dbKey.equals("R") || dbKey.equals("S") || dbKey.equals("T") || dbKey.equals("U") || dbKey.equals("V") || dbKey.equals("X") || dbKey.equals("Y") || dbKey.equals("Z") || dbKey.equals("ZA") || dbKey.equals("ZB") || dbKey.equals("ZC")) deltaCondition = it > 2 && it < 8;
        if (deltaCondition) { double[] drow = findRangeRow(HOLE_DELTA_TABLE, size); int didx = keyIndex(HOLE_DELTA_KEYS, "IT" + it); if (drow != null && didx >= 0) base += drow[2 + didx]; }
        return base;
    }
    private double getShaftDeviation(String dbKey, double size) { double[] row = findRangeRow(SHAFT_DEV_TABLE, size); if (row == null) return Double.NaN; int idx = keyIndex(SHAFT_KEYS, dbKey); return idx < 0 ? Double.NaN : row[2 + idx]; }
    private boolean isExceptionalHole(String gradeDb, int itIndex, double size) {
        int it = gradeNumber(itIndex);
        if (size > 3150) return true; if ((itIndex == 0 || itIndex == 1) && size > 500) return true;
        if (gradeDb.equals("A") || gradeDb.equals("B")) return size <= 1 || size > 500;
        if (gradeDb.equals("C") || gradeDb.equals("J6") || gradeDb.equals("J7") || gradeDb.equals("J8")) return size > 500;
        if (gradeDb.equals("CD") || gradeDb.equals("EF") || gradeDb.equals("FG")) return size > 50;
        if (gradeDb.equals("J")) return true;
        if (gradeDb.equals("K_IT1_IT8") || gradeDb.equals("M_IT1_IT8") || gradeDb.equals("N_IT1_IT8")) return itIndex < 4;
        if (gradeDb.equals("K_IT9_IT18") || gradeDb.equals("M_IT9_IT18") || gradeDb.equals("N_IT9_IT18")) {
            if (it < 1) return true;
        }
        if (gradeDb.equals("K_IT9_IT18")) return size > 3;
        if (gradeDb.equals("P") || gradeDb.equals("R") || gradeDb.equals("S") || gradeDb.equals("U")) return itIndex < 4;
        if (gradeDb.equals("T")) return itIndex < 4 || size <= 24;
        if (gradeDb.equals("V")) return itIndex < 4 || size <= 14 || size > 500;
        if (gradeDb.equals("Y")) return itIndex < 4 || size <= 18 || size > 500;
        if (gradeDb.equals("X") || gradeDb.equals("Z") || gradeDb.equals("ZA") || gradeDb.equals("ZB") || gradeDb.equals("ZC")) return itIndex < 4 || size > 500;
        return false;
    }
    private boolean isExceptionalShaft(String gradeDb, int itIndex, double size) {
        if (size > 3150) return true; if ((itIndex == 0 || itIndex == 1) && size > 500) return true;
        if (gradeDb.equals("a") || gradeDb.equals("b")) return size <= 1 || size > 500;
        if (gradeDb.equals("c") || gradeDb.equals("j5") || gradeDb.equals("j6") || gradeDb.equals("j7") || gradeDb.equals("x") || gradeDb.equals("za") || gradeDb.equals("zb") || gradeDb.equals("zc")) return size > 500;
        if (gradeDb.equals("cd") || gradeDb.equals("ef") || gradeDb.equals("fg")) return size > 50;
        if (gradeDb.equals("k4_7") || gradeDb.equals("k1_3_8_18")) return itIndex < 2;
        if (gradeDb.equals("j8")) return size > 3;
        if (gradeDb.equals("t")) return size <= 24;
        if (gradeDb.equals("v")) return size <= 14 || size > 500;
        if (gradeDb.equals("y")) return size <= 18 || size > 500;
        if (gradeDb.equals("j")) return true;
        return false;
    }

    private void showTrigonometry() {
        visualiser = null;
        modelView = null;
        inModelMeasure = false;
        inIsoTolerance = false;
        inTrigCalculator = true;
        inProbeAngle = false;
        setBaseRoot();

        Button back = button("← Back to tools");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        root.addView(title("Trigonometry Calculator"));

        TrigDiagramView diagram = new TrigDiagramView(this);
        diagram.setBackgroundColor(surface);
        root.addView(diagram, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        ScrollView scroll = new ScrollView(this);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(controls);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(330)));

        TextView hint = label("Right-triangle solver. Enter any two values where possible: opposite, adjacent, hypotenuse, or angle A. Blank unknowns are calculated.");
        controls.addView(hint);
        LinearLayout r1 = row();
        EditText opp = numeric("opposite");
        EditText adj = numeric("adjacent");
        r1.addView(wrap("Opposite", opp), weight());
        r1.addView(wrap("Adjacent", adj), weight());
        controls.addView(r1);
        LinearLayout r2 = row();
        EditText hyp = numeric("hypotenuse");
        EditText angle = numeric("degrees");
        r2.addView(wrap("Hypotenuse", hyp), weight());
        r2.addView(wrap("Angle A °", angle), weight());
        controls.addView(r2);
        TextView result = label("");
        result.setTextSize(16);
        result.setPadding(0, dp(10), 0, dp(8));
        controls.addView(result);
        Button calc = button("Solve triangle");
        controls.addView(calc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        Runnable solve = () -> {
            TriangleResult tr = solveTriangle(parse(opp.getText().toString()), parse(adj.getText().toString()), parse(hyp.getText().toString()), parse(angle.getText().toString()));
            result.setText(tr.message);
            if (tr.valid) diagram.setTriangle(tr.opposite, tr.adjacent, tr.hypotenuse, tr.angleDeg);
        };
        calc.setOnClickListener(v -> solve.run());
        adj.setText("40");
        angle.setText("30");
        solve.run();
    }

    private TriangleResult solveTriangle(Double oppIn, Double adjIn, Double hypIn, Double angleIn) {
        Double opp = positiveOrNull(oppIn), adj = positiveOrNull(adjIn), hyp = positiveOrNull(hypIn), ang = positiveOrNull(angleIn);
        try {
            if (opp != null && adj != null) { hyp = Math.hypot(opp, adj); ang = Math.toDegrees(Math.atan2(opp, adj)); }
            else if (opp != null && hyp != null) { if (opp >= hyp) return TriangleResult.error("Opposite must be less than hypotenuse."); adj = Math.sqrt(hyp * hyp - opp * opp); ang = Math.toDegrees(Math.asin(opp / hyp)); }
            else if (adj != null && hyp != null) { if (adj >= hyp) return TriangleResult.error("Adjacent must be less than hypotenuse."); opp = Math.sqrt(hyp * hyp - adj * adj); ang = Math.toDegrees(Math.acos(adj / hyp)); }
            else if (ang != null && hyp != null) { if (ang <= 0 || ang >= 90) return TriangleResult.error("Angle A must be between 0° and 90°."); opp = hyp * Math.sin(Math.toRadians(ang)); adj = hyp * Math.cos(Math.toRadians(ang)); }
            else if (ang != null && adj != null) { if (ang <= 0 || ang >= 90) return TriangleResult.error("Angle A must be between 0° and 90°."); opp = adj * Math.tan(Math.toRadians(ang)); hyp = adj / Math.cos(Math.toRadians(ang)); }
            else if (ang != null && opp != null) { if (ang <= 0 || ang >= 90) return TriangleResult.error("Angle A must be between 0° and 90°."); adj = opp / Math.tan(Math.toRadians(ang)); hyp = opp / Math.sin(Math.toRadians(ang)); }
            else return TriangleResult.error("Enter at least two usable values.");
        } catch (Exception ex) { return TriangleResult.error("Could not solve that triangle."); }
        double area = 0.5 * opp * adj;
        double other = 90.0 - ang;
        return new TriangleResult(true, opp, adj, hyp, ang,
                String.format(Locale.UK, "Opposite: %.4f\nAdjacent: %.4f\nHypotenuse: %.4f\nAngle A: %.4f°\nAngle B: %.4f°\nArea: %.4f", opp, adj, hyp, ang, other, area));
    }

    private Double positiveOrNull(Double v) { return v == null || v <= 0 ? null : v; }

    private void showProbeAngle() {
        visualiser = null;
        modelView = null;
        inModelMeasure = false;
        inIsoTolerance = false;
        inTrigCalculator = false;
        inProbeAngle = true;
        setBaseRoot();

        Button back = button("← Back to tools");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        root.addView(title("Probe Angle Calculator"));

        ProbeAngleView diagram = new ProbeAngleView(this);
        diagram.setBackgroundColor(surface);
        root.addView(diagram, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, dp(8), 0, dp(12));
        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(310)));
        TextView hint = label("Checks when the stylus stem/shaft would become the limiting contact instead of the probe ball. Uses angle from the surface normal: sin(angle) = (probe radius - shaft radius) / usable length.");
        controls.addView(hint);
        LinearLayout r1 = row();
        EditText probeDia = numeric("e.g. 4");
        EditText shaftDia = numeric("e.g. 2");
        r1.addView(wrap("Probe ball dia", probeDia), weight());
        r1.addView(wrap("Shaft/stem dia", shaftDia), weight());
        controls.addView(r1);
        EditText length = numeric("e.g. 20");
        controls.addView(wrap("Usable length from ball centre", length));
        TextView result = label("");
        result.setTextSize(16);
        result.setPadding(0, dp(10), 0, dp(8));
        controls.addView(result);
        Button calc = button("Calculate max angle");
        controls.addView(calc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        Runnable solve = () -> {
            String msg = calculateProbeAngle(probeDia.getText().toString(), shaftDia.getText().toString(), length.getText().toString(), diagram);
            result.setText(msg);
        };
        calc.setOnClickListener(v -> solve.run());
        probeDia.setText("4");
        shaftDia.setText("2");
        length.setText("20");
        solve.run();
    }

    private String calculateProbeAngle(String probeText, String shaftText, String lengthText, ProbeAngleView diagram) {
        Double probeDia = parse(probeText), shaftDia = parse(shaftText), length = parse(lengthText);
        if (probeDia == null || shaftDia == null || length == null || probeDia <= 0 || shaftDia <= 0 || length <= 0) return "Enter positive probe diameter, shaft diameter, and usable length.";
        double clearance = (probeDia - shaftDia) / 2.0;
        if (clearance <= 0) return "Shaft/stem diameter must be smaller than the probe ball diameter.";
        double ratio = clearance / length;
        if (ratio >= 1) ratio = 1;
        double angle = Math.toDegrees(Math.asin(ratio));
        double fromSurface = 90.0 - angle;
        diagram.setValues(probeDia, shaftDia, length, angle);
        return String.format(Locale.UK,
                "Max angle from surface normal: %.3f°\nEquivalent from surface: %.3f°\nRadial clearance: %.4f\nFormula: asin((probe dia - shaft dia) / 2 / length)\nAssumption: length is from probe-ball centre to the point where the stem becomes exposed/limiting.",
                angle, fromSurface, clearance);
    }

    private void showModelMeasure() {
        visualiser = null;
        inModelMeasure = true;
        inIsoTolerance = false;
        inTrigCalculator = false;
        inProbeAngle = false;
        setBaseRoot();

        Button back = button("← Back to tools");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView title = title("Model Measure");
        root.addView(title);

        modelView = new ModelMeasureView(this);
        modelView.setBackgroundColor(surface);
        root.addView(modelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, dp(10), 0, 0);
        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(215)));

        LinearLayout row = row();
        Button importButton = button("Import OBJ/STL");
        importButton.setOnClickListener(v -> openModelPicker());
        Button demoButton = button("Load demo block");
        demoButton.setOnClickListener(v -> {
            modelView.setModel(makeDemoBlock("Demo 100 × 60 × 25 block"));
            updateModelStatus();
        });
        row.addView(importButton, weight());
        row.addView(demoButton, weight());
        controls.addView(row);

        Button clear = button("Clear selection");
        clear.setOnClickListener(v -> {
            modelView.clearSelection();
            updateModelStatus();
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        clearLp.setMargins(0, dp(8), 0, 0);
        controls.addView(clear, clearLp);

        modelStatus = label("");
        modelStatus.setPadding(0, dp(8), 0, 0);
        controls.addView(modelStatus);

        TextView hint = label("Drag right to orbit right. Pinch to zoom, two-finger drag to pan, and tap two visible vertices to measure. Units are model units because OBJ/STL files do not declare mm/inch reliably.");
        hint.setPadding(0, dp(8), 0, 0);
        controls.addView(hint);

        modelView.setModel(makeDemoBlock("Demo 100 × 60 × 25 block"));
        updateModelStatus();
    }


    private void openModelPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN_MODEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_MODEL && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            loadModelUri(uri);
        }
    }

    private void loadModelUri(Uri uri) {
        String name = displayName(uri);
        if (modelStatus != null) modelStatus.setText("Loading " + name + "…");
        new Thread(() -> {
            try {
                ModelData model = parseModel(uri, name);
                runOnUiThread(() -> {
                    if (modelView != null) {
                        modelView.setModel(model);
                        updateModelStatus();
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "Could not import model from " + uri, ex);
                runOnUiThread(() -> {
                    if (modelView != null) modelView.clearModel();
                    if (modelStatus != null) modelStatus.setText("Could not import " + name + ": " + ex.getMessage() + "\nTry an OBJ or STL export under 24 MB.");
                });
            }
        }).start();
    }

    private ModelData parseModel(Uri uri, String name) throws Exception {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("no readable stream");
            bytes = readAll(in, MODEL_MAX_BYTES);
        }
        String lower = name.toLowerCase(Locale.UK);
        if (lower.endsWith(".obj")) return parseObj(new String(bytes, StandardCharsets.UTF_8), name);
        if (looksBinaryStl(bytes)) return parseBinaryStl(bytes, name);
        String textData = new String(bytes, StandardCharsets.UTF_8);
        if (lower.endsWith(".stl")) {
            ModelData stl = parseAsciiStl(textData, name);
            if (stl.vertices.size() > 0) return stl;
            throw new Exception("no ASCII STL vertices found");
        }
        ModelData stl = parseAsciiStl(textData, name);
        if (stl.vertices.size() > 0) return stl;
        return parseObj(textData, name);
    }

    private byte[] readAll(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) throw new Exception("file is over " + (maxBytes / (1024 * 1024)) + " MB; simplify it first");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private String displayName(Uri uri) {
        String out = "model";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) out = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return out == null ? "model" : out;
    }

    private boolean looksBinaryStl(byte[] bytes) {
        if (bytes.length < 84) return false;
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        long triCount = bb.getInt(80) & 0xffffffffL;
        long expected = 84L + triCount * 50L;
        return triCount > 0 && expected <= bytes.length && expected > 84L;
    }

    private ModelData parseObj(String textData, String name) throws Exception {
        ModelData model = new ModelData(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(textData.getBytes(StandardCharsets.UTF_8))));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            if (parts[0].equals("v") && parts.length >= 4) {
                model.vertices.add(new Vec3(parseDouble(parts[1]), parseDouble(parts[2]), parseDouble(parts[3])));
            } else if (parts[0].equals("f") && parts.length >= 4) {
                ArrayList<Integer> idxs = new ArrayList<>();
                for (int n = 1; n < parts.length; n++) idxs.add(objIndex(parts[n], model.vertices.size()));
                for (int n = 1; n + 1 < idxs.size(); n++) model.triangles.add(new Triangle(idxs.get(0), idxs.get(n), idxs.get(n + 1)));
            }
        }
        if (model.vertices.size() == 0) throw new Exception("no OBJ vertices found");
        model.finish();
        return model;
    }

    private int objIndex(String token, int size) throws Exception {
        String first = token.split("/")[0];
        int raw = Integer.parseInt(first);
        int idx = raw < 0 ? size + raw : raw - 1;
        if (idx < 0 || idx >= size) throw new Exception("OBJ face index out of range");
        return idx;
    }

    private ModelData parseAsciiStl(String textData, String name) throws Exception {
        ModelData model = new ModelData(name);
        ArrayList<Integer> tri = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(textData.getBytes(StandardCharsets.UTF_8))));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim().toLowerCase(Locale.UK);
            if (!line.startsWith("vertex")) continue;
            String[] p = line.split("\\s+");
            if (p.length < 4) continue;
            model.vertices.add(new Vec3(parseDouble(p[1]), parseDouble(p[2]), parseDouble(p[3])));
            tri.add(model.vertices.size() - 1);
            if (tri.size() == 3) {
                model.triangles.add(new Triangle(tri.get(0), tri.get(1), tri.get(2)));
                tri.clear();
            }
        }
        if (model.vertices.size() > 0) model.finish();
        return model;
    }

    private ModelData parseBinaryStl(byte[] bytes, String name) throws Exception {
        ModelData model = new ModelData(name);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int triCount = bb.getInt(80);
        long expected = 84L + ((long)triCount) * 50L;
        if (triCount <= 0 || expected > bytes.length) throw new Exception("invalid binary STL triangle count");
        int pos = 84;
        for (int t = 0; t < triCount; t++) {
            pos += 12; // normal
            int a = addBinaryVertex(model, bytes, pos); pos += 12;
            int b = addBinaryVertex(model, bytes, pos); pos += 12;
            int c = addBinaryVertex(model, bytes, pos); pos += 12;
            model.triangles.add(new Triangle(a, b, c));
            pos += 2; // attribute byte count
        }
        if (model.vertices.size() == 0) throw new Exception("empty binary STL");
        model.finish();
        return model;
    }

    private int addBinaryVertex(ModelData model, byte[] bytes, int pos) {
        ByteBuffer bb = ByteBuffer.wrap(bytes, pos, 12).order(ByteOrder.LITTLE_ENDIAN);
        model.vertices.add(new Vec3(bb.getFloat(), bb.getFloat(), bb.getFloat()));
        return model.vertices.size() - 1;
    }

    private double parseDouble(String s) throws Exception {
        return Double.parseDouble(s.trim());
    }

    private ModelData makeDemoBlock(String name) {
        ModelData m = new ModelData(name);
        double x = 100, y = 60, z = 25;
        m.vertices.add(new Vec3(0, 0, 0));
        m.vertices.add(new Vec3(x, 0, 0));
        m.vertices.add(new Vec3(x, y, 0));
        m.vertices.add(new Vec3(0, y, 0));
        m.vertices.add(new Vec3(0, 0, z));
        m.vertices.add(new Vec3(x, 0, z));
        m.vertices.add(new Vec3(x, y, z));
        m.vertices.add(new Vec3(0, y, z));
        int[][] faces = {{0,1,2},{0,2,3},{4,6,5},{4,7,6},{0,4,5},{0,5,1},{1,5,6},{1,6,2},{2,6,7},{2,7,3},{3,7,4},{3,4,0}};
        for (int[] f : faces) m.triangles.add(new Triangle(f[0], f[1], f[2]));
        m.finish();
        return m;
    }

    private void updateModelStatus() {
        if (modelStatus == null || modelView == null || modelView.model == null) return;
        ModelData m = modelView.model;
        String selection = modelView.selectionText();
        modelStatus.setText(String.format(Locale.UK,
                "%s\nVertices: %d  Triangles: %d\nX %.3f  Y %.3f  Z %.3f%s",
                m.name, m.vertices.size(), m.triangles.size(), m.width(), m.depth(), m.height(), selection));
    }

    @Override
    public void onBackPressed() {
        if (visualiser != null || inModelMeasure || inIsoTolerance || inTrigCalculator || inProbeAngle) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    private SeekBar.OnSeekBarChangeListener seekListener(boolean xy) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || editing) return;
                if (xy) xyDeg = progress / 10.0; else zDeg = progress / 10.0 - 90.0;
                updateFromAngles(false);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private void addAngleWatcher(EditText edit, boolean xy) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                if (editing || !edit.hasFocus()) return;
                Double val = parse(e.toString());
                if (val == null) return;
                if (xy) xyDeg = val; else zDeg = Math.max(-90, Math.min(90, val));
                updateFromAngles(false);
            }
        });
    }

    private void addIjkWatcher(EditText edit) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                if (editing || !edit.hasFocus()) return;
                Double ii = parse(iInput.getText().toString());
                Double jj = parse(jInput.getText().toString());
                Double kk = parse(kInput.getText().toString());
                if (ii == null || jj == null || kk == null) return;
                setVector(ii, jj, kk, false);
            }
        });
    }

    private void updateFromAngles(boolean keepTextFocus) {
        double xyRad = Math.toRadians(xyDeg);
        double zRad = Math.toRadians(zDeg);
        i = Math.cos(zRad) * Math.cos(xyRad);
        j = Math.cos(zRad) * Math.sin(xyRad);
        k = Math.sin(zRad);
        refreshUi(keepTextFocus);
    }

    private void setVector(double ii, double jj, double kk, boolean keepTextFocus) {
        double len = Math.sqrt(ii * ii + jj * jj + kk * kk);
        if (len < 0.000001) return;
        i = ii / len;
        j = jj / len;
        k = kk / len;
        xyDeg = positiveDeg(Math.toDegrees(Math.atan2(j, i)));
        zDeg = Math.toDegrees(Math.asin(k));
        refreshUi(keepTextFocus);
    }

    private void refreshUi(boolean keepTextFocus) {
        editing = true;
        xySeek.setProgress((int)Math.round(positiveDeg(xyDeg) * 10));
        zSeek.setProgress((int)Math.round((zDeg + 90.0) * 10));
        xyValue.setText(String.format(Locale.UK, "XY angle: %.1f°", positiveDeg(xyDeg)));
        zValue.setText(String.format(Locale.UK, "Z/elevation: %.1f°", zDeg));
        xyInput.setText(String.format(Locale.UK, "%.3f", positiveDeg(xyDeg)));
        zInput.setText(String.format(Locale.UK, "%.3f", zDeg));
        iInput.setText(String.format(Locale.UK, "%.6f", i));
        jInput.setText(String.format(Locale.UK, "%.6f", j));
        kInput.setText(String.format(Locale.UK, "%.6f", k));
        if (!keepTextFocus) {
            xyInput.clearFocus(); zInput.clearFocus(); iInput.clearFocus(); jInput.clearFocus(); kInput.clearFocus();
        }
        editing = false;
        visualiser.setVector(i, j, k, xyDeg, zDeg);
    }

    private double positiveDeg(double deg) {
        double out = deg % 360.0;
        return out < 0 ? out + 360.0 : out;
    }

    private Double parse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception ignored) { return null; }
    }

    private TextView title(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(text);
        v.setTextSize(26);
        v.setGravity(Gravity.START);
        v.setPadding(0, dp(8), 0, dp(10));
        return v;
    }

    private TextView label(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(muted);
        v.setTextSize(15);
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setBackgroundColor(surface2);
        return b;
    }

    private EditText numeric(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(text);
        e.setHintTextColor(muted);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setBackgroundColor(surface2);
        e.setPadding(dp(8), 0, dp(8), 0);
        return e;
    }

    private EditText textInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(text);
        e.setHintTextColor(muted);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setBackgroundColor(surface2);
        e.setPadding(dp(8), 0, dp(8), 0);
        return e;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(6), 0, 0);
        return l;
    }

    private LinearLayout wrap(String label, EditText input) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(3), 0, dp(3), 0);
        TextView t = label(label);
        l.addView(t);
        l.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return l;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class Vec3 {
        double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        double distance(Vec3 o) {
            double dx = x - o.x, dy = y - o.y, dz = z - o.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    static class Triangle {
        int a, b, c;
        Triangle(int a, int b, int c) { this.a = a; this.b = b; this.c = c; }
    }

    static class ModelData {
        String name;
        ArrayList<Vec3> vertices = new ArrayList<>();
        ArrayList<Triangle> triangles = new ArrayList<>();
        double minX, minY, minZ, maxX, maxY, maxZ;
        ModelData(String name) { this.name = name; }
        void finish() {
            if (vertices.size() == 0) return;
            minX = maxX = vertices.get(0).x; minY = maxY = vertices.get(0).y; minZ = maxZ = vertices.get(0).z;
            for (Vec3 v : vertices) {
                minX = Math.min(minX, v.x); minY = Math.min(minY, v.y); minZ = Math.min(minZ, v.z);
                maxX = Math.max(maxX, v.x); maxY = Math.max(maxY, v.y); maxZ = Math.max(maxZ, v.z);
            }
        }
        double width() { return maxX - minX; }
        double depth() { return maxY - minY; }
        double height() { return maxZ - minZ; }
        double maxDim() { return Math.max(width(), Math.max(depth(), height())); }
        Vec3 center() { return new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0); }
    }

    static class TriangleResult {
        boolean valid;
        double opposite, adjacent, hypotenuse, angleDeg;
        String message;
        TriangleResult(boolean valid, double opposite, double adjacent, double hypotenuse, double angleDeg, String message) {
            this.valid = valid; this.opposite = opposite; this.adjacent = adjacent; this.hypotenuse = hypotenuse; this.angleDeg = angleDeg; this.message = message;
        }
        static TriangleResult error(String message) { return new TriangleResult(false, 0, 0, 0, 0, message); }
    }

    public class TrigDiagramView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private double opp = 23.094, adj = 40, hyp = 46.188, angle = 30;
        public TrigDiagramView(Activity context) { super(context); }
        public void setTriangle(double opposite, double adjacent, double hypotenuse, double angleDeg) { opp = opposite; adj = adjacent; hyp = hypotenuse; angle = angleDeg; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(surface);
            int w = getWidth(), h = getHeight();
            float margin = dp(36);
            float maxW = Math.max(1, w - margin * 2);
            float maxH = Math.max(1, h - margin * 2 - dp(30));
            float scale = (float)Math.min(maxW / Math.max(adj, 1), maxH / Math.max(opp, 1));
            float x0 = margin;
            float y0 = h - margin;
            float x1 = x0 + (float)(adj * scale);
            float y1 = y0;
            float x2 = x1;
            float y2 = y0 - (float)(opp * scale);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(4));
            paint.setColor(pink);
            canvas.drawLine(x0, y0, x1, y1, paint);
            paint.setColor(Color.rgb(110, 220, 255));
            canvas.drawLine(x1, y1, x2, y2, paint);
            paint.setColor(Color.rgb(255, 220, 120));
            canvas.drawLine(x0, y0, x2, y2, paint);
            paint.setStrokeWidth(dp(2));
            paint.setColor(muted);
            canvas.drawLine(x1 - dp(18), y1, x1 - dp(18), y1 - dp(18), paint);
            canvas.drawLine(x1 - dp(18), y1 - dp(18), x1, y1 - dp(18), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(15));
            paint.setColor(text);
            canvas.drawText("Right triangle", dp(14), dp(24), paint);
            paint.setColor(muted);
            canvas.drawText(String.format(Locale.UK, "A %.2f°", angle), x0 + dp(8), y0 - dp(10), paint);
            canvas.drawText(String.format(Locale.UK, "adj %.3f", adj), (x0 + x1) / 2 - dp(25), y0 + dp(24), paint);
            canvas.drawText(String.format(Locale.UK, "opp %.3f", opp), x1 + dp(8), (y1 + y2) / 2, paint);
            canvas.drawText(String.format(Locale.UK, "hyp %.3f", hyp), (x0 + x2) / 2 - dp(20), (y0 + y2) / 2 - dp(10), paint);
        }
    }

    public class ProbeAngleView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private double probeDia = 4, shaftDia = 2, length = 20, angle = 2.866;
        public ProbeAngleView(Activity context) { super(context); }
        public void setValues(double probeDia, double shaftDia, double length, double angle) { this.probeDia = probeDia; this.shaftDia = shaftDia; this.length = length; this.angle = angle; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(surface);
            int w = getWidth(), h = getHeight();
            float cx = w / 2f;
            float by = h - dp(44);
            float ballR = Math.max(dp(20), Math.min(w, h) * 0.12f);
            float stemR = (float)(ballR * Math.max(0.15, shaftDia / Math.max(probeDia, 0.001)));
            float stemLen = Math.min(h * 0.55f, Math.max(dp(70), (float)(length / Math.max(probeDia, 0.001) * ballR * 0.8)));
            double a = Math.toRadians(angle);
            float dx = (float)(Math.sin(a) * stemLen);
            float dy = (float)(Math.cos(a) * stemLen);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(50, 42, 62));
            canvas.drawRect(0, by + ballR, w, h, paint);
            paint.setColor(Color.rgb(110, 220, 255));
            paint.setStrokeWidth(stemR * 2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(cx, by, cx + dx, by - dy, paint);
            paint.setColor(pink);
            canvas.drawCircle(cx, by, ballR, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(255, 220, 120));
            canvas.drawLine(cx, by, cx, by - stemLen * 0.55f, paint);
            canvas.drawLine(cx, by, cx + dx, by - dy, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(15));
            paint.setColor(text);
            canvas.drawText("Probe/stylus clearance", dp(14), dp(24), paint);
            paint.setColor(muted);
            canvas.drawText(String.format(Locale.UK, "Max from normal %.3f°", angle), dp(14), dp(45), paint);
            canvas.drawText(String.format(Locale.UK, "Ball Ø%.3f  Stem Ø%.3f  Length %.3f", probeDia, shaftDia, length), dp(14), dp(66), paint);
        }
    }

    public class ModelMeasureView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ModelData model;
        private float yaw = -35f;
        private float pitch = 25f;
        private float zoom = 1f;
        private float panX = 0f, panY = 0f;
        private float lastX, lastY, downX, downY;
        private float lastSpan = 0f, lastMidX = 0f, lastMidY = 0f;
        private boolean twoFingerGesture = false;
        private int selectedA = -1, selectedB = -1;
        private float[] screenX = new float[0];
        private float[] screenY = new float[0];

        public ModelMeasureView(Activity context) { super(context); }

        public void setModel(ModelData model) {
            this.model = model;
            selectedA = -1;
            selectedB = -1;
            zoom = 1f;
            panX = 0f;
            panY = 0f;
            int count = model == null ? 0 : model.vertices.size();
            screenX = new float[count];
            screenY = new float[count];
            invalidate();
        }

        public void clearModel() {
            setModel(null);
        }

        public void clearSelection() {
            selectedA = -1;
            selectedB = -1;
            invalidate();
        }

        public String selectionText() {
            if (model == null || selectedA < 0) return "\nTap two vertices to measure.";
            if (selectedB < 0) return String.format(Locale.UK, "\nA: vertex %d selected. Tap B.", selectedA + 1);
            Vec3 a = model.vertices.get(selectedA);
            Vec3 b = model.vertices.get(selectedB);
            return String.format(Locale.UK, "\nA v%d → B v%d: %.4f units  ΔX %.4f  ΔY %.4f  ΔZ %.4f",
                    selectedA + 1, selectedB + 1, a.distance(b), b.x - a.x, b.y - a.y, b.z - a.z);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(surface);
            if (model == null || model.vertices.size() == 0) return;

            int w = getWidth(), h = getHeight();
            float cx = w / 2f + panX, cy = h / 2f + dp(10) + panY;
            float scale = (float)(Math.min(w, h) * 0.72 * zoom / Math.max(1.0, model.maxDim()));
            Vec3 center = model.center();

            for (int n = 0; n < model.vertices.size(); n++) {
                Vec3 v = model.vertices.get(n);
                float[] p = project(v.x - center.x, v.y - center.y, v.z - center.z, cx, cy, scale);
                screenX[n] = p[0];
                screenY[n] = p[1];
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(90, 75, 105));
            for (Triangle t : model.triangles) {
                drawEdge(canvas, t.a, t.b);
                drawEdge(canvas, t.b, t.c);
                drawEdge(canvas, t.c, t.a);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(245, 180, 215));
            int pointStep = model.vertices.size() > 1500 ? Math.max(1, model.vertices.size() / 1500) : 1;
            for (int n = 0; n < model.vertices.size(); n += pointStep) canvas.drawCircle(screenX[n], screenY[n], dp(2), paint);

            drawSelected(canvas, selectedA, Color.rgb(255, 220, 120), "A");
            drawSelected(canvas, selectedB, Color.rgb(110, 220, 255), "B");
            if (selectedA >= 0 && selectedB >= 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(pink);
                canvas.drawLine(screenX[selectedA], screenY[selectedA], screenX[selectedB], screenY[selectedB], paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(text);
            paint.setTextSize(dp(15));
            canvas.drawText("Wireframe model viewer", dp(14), dp(24), paint);
            paint.setColor(muted);
            paint.setTextSize(dp(13));
            canvas.drawText("Drag to orbit. Pinch zoom, two-finger pan. Tap vertices for A/B.", dp(14), dp(44), paint);
        }

        private void drawEdge(Canvas c, int a, int b) {
            if (a < 0 || b < 0 || a >= screenX.length || b >= screenX.length) return;
            c.drawLine(screenX[a], screenY[a], screenX[b], screenY[b], paint);
        }

        private void drawSelected(Canvas c, int idx, int colour, String label) {
            if (idx < 0 || idx >= screenX.length) return;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colour);
            c.drawCircle(screenX[idx], screenY[idx], dp(7), paint);
            paint.setTextSize(dp(15));
            c.drawText(label, screenX[idx] + dp(8), screenY[idx] - dp(8), paint);
        }

        private float[] project(double x, double y, double z, float cx, float cy, float scale) {
            double yr = Math.toRadians(yaw);
            double pr = Math.toRadians(pitch);
            double x1 = x * Math.cos(yr) - y * Math.sin(yr);
            double y1 = x * Math.sin(yr) + y * Math.cos(yr);
            double z1 = z;
            double y2 = y1 * Math.cos(pr) - z1 * Math.sin(pr);
            double z2 = y1 * Math.sin(pr) + z1 * Math.cos(pr);
            double persp = 1.0 / (1.0 + z2 * 0.0015);
            return new float[] { (float)(cx + x1 * scale * persp), (float)(cy - y2 * scale * persp) };
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                twoFingerGesture = false;
                lastX = downX = event.getX();
                lastY = downY = event.getY();
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
                twoFingerGesture = true;
                lastSpan = pointerSpan(event);
                lastMidX = pointerMidX(event);
                lastMidY = pointerMidY(event);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (event.getPointerCount() >= 2) {
                    float span = pointerSpan(event);
                    float midX = pointerMidX(event);
                    float midY = pointerMidY(event);
                    if (lastSpan > 1f) {
                        zoom *= span / lastSpan;
                        zoom = Math.max(0.2f, Math.min(8f, zoom));
                    }
                    panX += midX - lastMidX;
                    panY += midY - lastMidY;
                    lastSpan = span;
                    lastMidX = midX;
                    lastMidY = midY;
                    invalidate();
                    return true;
                }
                if (!twoFingerGesture) {
                    yaw += (event.getX() - lastX) * 0.45f;
                    pitch -= (event.getY() - lastY) * 0.45f;
                    pitch = Math.max(-170f, Math.min(170f, pitch));
                    lastX = event.getX(); lastY = event.getY(); invalidate();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP) {
                if (event.getPointerCount() <= 2) {
                    twoFingerGesture = true;
                    int keep = event.getActionIndex() == 0 ? 1 : 0;
                    lastX = event.getX(keep);
                    lastY = event.getY(keep);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!twoFingerGesture && Math.sqrt(dx * dx + dy * dy) < dp(8)) pickVertex(event.getX(), event.getY());
                return true;
            }
            return true;
        }

        private float pointerSpan(MotionEvent event) {
            if (event.getPointerCount() < 2) return 0f;
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            return (float)Math.sqrt(dx * dx + dy * dy);
        }

        private float pointerMidX(MotionEvent event) { return (event.getX(0) + event.getX(1)) / 2f; }
        private float pointerMidY(MotionEvent event) { return (event.getY(0) + event.getY(1)) / 2f; }

        private void pickVertex(float x, float y) {
            if (model == null || screenX.length == 0) return;
            int nearest = -1;
            double best = dp(28) * dp(28);
            for (int n = 0; n < screenX.length; n++) {
                double dx = screenX[n] - x;
                double dy = screenY[n] - y;
                double d = dx * dx + dy * dy;
                if (d < best) { best = d; nearest = n; }
            }
            if (nearest >= 0) {
                if (selectedA < 0 || (selectedA >= 0 && selectedB >= 0)) {
                    selectedA = nearest;
                    selectedB = -1;
                } else {
                    selectedB = nearest;
                }
                updateModelStatus();
                invalidate();
            }
        }
    }


    public class VisualiserView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private double vi = 0.66, vj = 0.66, vk = 0.34;
        private double displayXy = 45, displayZ = 20;
        private float yaw = -35f;
        private float pitch = 22f;
        private float lastX, lastY;

        public VisualiserView(Activity context) { super(context); }

        public void setVector(double i, double j, double k, double xy, double z) {
            this.vi = i; this.vj = j; this.vk = k; this.displayXy = xy; this.displayZ = z;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            canvas.drawColor(surface);
            float cx = w / 2f, cy = h / 2f + dp(16);
            float scale = Math.min(w, h) * 0.34f;

            drawGrid(canvas, cx, cy, scale);
            drawAxis(canvas, cx, cy, scale, 1, 0, 0, Color.rgb(255, 107, 107), "X/I");
            drawAxis(canvas, cx, cy, scale, 0, 1, 0, Color.rgb(112, 212, 139), "Y/J");
            drawAxis(canvas, cx, cy, scale, 0, 0, 1, Color.rgb(110, 168, 255), "Z/K");
            drawArrow(canvas, cx, cy, scale * 1.18f, vi, vj, vk, pink, "IJK");

            paint.setColor(text);
            paint.setTextSize(dp(15));
            canvas.drawText(String.format(Locale.UK, "I %.5f   J %.5f   K %.5f", vi, vj, vk), dp(14), dp(24), paint);
            paint.setColor(muted);
            paint.setTextSize(dp(13));
            canvas.drawText(String.format(Locale.UK, "XY %.1f°   Z %.1f°", positiveDeg(displayXy), displayZ), dp(14), dp(44), paint);
        }

        private void drawGrid(Canvas c, float cx, float cy, float scale) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.rgb(55, 45, 65));
            for (int n = -2; n <= 2; n++) {
                float[] a = project(-1, n * 0.25, 0, cx, cy, scale);
                float[] b = project(1, n * 0.25, 0, cx, cy, scale);
                c.drawLine(a[0], a[1], b[0], b[1], paint);
                a = project(n * 0.25, -1, 0, cx, cy, scale);
                b = project(n * 0.25, 1, 0, cx, cy, scale);
                c.drawLine(a[0], a[1], b[0], b[1], paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawAxis(Canvas c, float cx, float cy, float scale, double x, double y, double z, int colour, String label) {
            drawArrow(c, cx, cy, scale, x, y, z, colour, label);
        }

        private void drawArrow(Canvas c, float cx, float cy, float scale, double x, double y, double z, int colour, String label) {
            float[] o = project(0, 0, 0, cx, cy, scale);
            float[] p = project(x, y, z, cx, cy, scale);
            paint.setColor(colour);
            paint.setStrokeWidth(label.equals("IJK") ? dp(5) : dp(3));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            c.drawLine(o[0], o[1], p[0], p[1], paint);

            float dx = p[0] - o[0];
            float dy = p[1] - o[1];
            float len2 = (float)Math.sqrt(dx * dx + dy * dy);
            if (len2 > 0.001f) {
                float ux = dx / len2;
                float uy = dy / len2;
                float px = -uy;
                float py = ux;
                float headBack = label.equals("IJK") ? dp(18) : dp(14);
                float headSide = label.equals("IJK") ? dp(9) : dp(7);
                Path head = new Path();
                head.moveTo(p[0], p[1]);
                head.lineTo(p[0] - ux * headBack + px * headSide, p[1] - uy * headBack + py * headSide);
                head.lineTo(p[0] - ux * headBack - px * headSide, p[1] - uy * headBack - py * headSide);
                head.close();
                paint.setStyle(Paint.Style.FILL);
                c.drawPath(head, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(13));
            c.drawText(label, p[0] + dp(6), p[1] - dp(6), paint);
        }

        private float[] project(double x, double y, double z, float cx, float cy, float scale) {
            double yr = Math.toRadians(yaw);
            double pr = Math.toRadians(pitch);
            double x1 = x * Math.cos(yr) - y * Math.sin(yr);
            double y1 = x * Math.sin(yr) + y * Math.cos(yr);
            double z1 = z;
            double y2 = y1 * Math.cos(pr) - z1 * Math.sin(pr);
            double z2 = y1 * Math.sin(pr) + z1 * Math.cos(pr);
            double persp = 1.0 / (1.0 + z2 * 0.22);
            return new float[] { (float)(cx + x1 * scale * persp), (float)(cy - y2 * scale * persp) };
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastX = event.getX(); lastY = event.getY(); return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                yaw -= (event.getX() - lastX) * 0.45f;
                pitch -= (event.getY() - lastY) * 0.45f;
                pitch = Math.max(-170f, Math.min(170f, pitch));
                lastX = event.getX(); lastY = event.getY(); invalidate(); return true;
            }
            return true;
        }
    }
}
