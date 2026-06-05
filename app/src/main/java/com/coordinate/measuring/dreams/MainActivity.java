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
    private static final int REQUEST_OPEN_MODEL = 42;

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

        TextView more = label("Import a simple OBJ/STL, orbit the wireframe, and tap two vertices to measure between them.");
        more.setPadding(0, dp(18), 0, 0);
        root.addView(more);
    }

    private void showIJK() {
        inModelMeasure = false;
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

    private void showModelMeasure() {
        visualiser = null;
        inModelMeasure = true;
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

        TextView hint = label("Drag to orbit. Pinch-like zoom is not needed yet: use two-finger apps later; for now the model auto-fits. Tap two visible vertices to measure. Units are model units because OBJ/STL files do not declare mm/inch reliably.");
        hint.setPadding(0, dp(8), 0, 0);
        controls.addView(hint);

        modelView.setModel(makeDemoBlock("Demo 100 × 60 × 25 block"));
        updateModelStatus();
    }

    private void openModelPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/sla", "application/octet-stream", "text/plain", "model/obj", "text/*"
        });
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
                runOnUiThread(() -> {
                    if (modelStatus != null) modelStatus.setText("Could not import model: " + ex.getMessage());
                });
            }
        }).start();
    }

    private ModelData parseModel(Uri uri, String name) throws Exception {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("no readable stream");
            bytes = readAll(in, 6 * 1024 * 1024);
        }
        String lower = name.toLowerCase(Locale.UK);
        if (lower.endsWith(".obj")) return parseObj(new String(bytes, StandardCharsets.UTF_8), name);
        if (looksBinaryStl(bytes)) return parseBinaryStl(bytes, name);
        ModelData stl = parseAsciiStl(new String(bytes, StandardCharsets.UTF_8), name);
        if (stl.vertices.size() > 0) return stl;
        return parseObj(new String(bytes, StandardCharsets.UTF_8), name);
    }

    private byte[] readAll(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) throw new Exception("file is over 6 MB; simplify it first");
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
        return 84L + triCount * 50L == bytes.length;
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
        if (visualiser != null || inModelMeasure) {
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

    public class ModelMeasureView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ModelData model;
        private float yaw = -35f;
        private float pitch = 25f;
        private float lastX, lastY, downX, downY;
        private int selectedA = -1, selectedB = -1;
        private float[] screenX = new float[0];
        private float[] screenY = new float[0];

        public ModelMeasureView(Activity context) { super(context); }

        public void setModel(ModelData model) {
            this.model = model;
            selectedA = -1;
            selectedB = -1;
            screenX = new float[model.vertices.size()];
            screenY = new float[model.vertices.size()];
            invalidate();
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
            float cx = w / 2f, cy = h / 2f + dp(10);
            float scale = (float)(Math.min(w, h) * 0.72 / Math.max(1.0, model.maxDim()));
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
            canvas.drawText("Drag to orbit. Tap vertices for A/B measurement.", dp(14), dp(44), paint);
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
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastX = downX = event.getX();
                lastY = downY = event.getY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                yaw -= (event.getX() - lastX) * 0.45f;
                pitch -= (event.getY() - lastY) * 0.45f;
                pitch = Math.max(-170f, Math.min(170f, pitch));
                lastX = event.getX(); lastY = event.getY(); invalidate(); return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.sqrt(dx * dx + dy * dy) < dp(8)) pickVertex(event.getX(), event.getY());
                return true;
            }
            return true;
        }

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
