package com.coordinate.measuring.dreams;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
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

import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(18, 16, 23);
    private final int surface = Color.rgb(29, 23, 36);
    private final int surface2 = Color.rgb(42, 32, 51);
    private final int pink = Color.rgb(214, 107, 160);
    private final int text = Color.rgb(247, 236, 243);
    private final int muted = Color.rgb(188, 170, 188);

    private LinearLayout root;
    private VisualiserView visualiser;
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

        TextView more = label("More function buttons can be added here as the toolbox grows.");
        more.setPadding(0, dp(18), 0, 0);
        root.addView(more);
    }

    private void showIJK() {
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

    @Override
    public void onBackPressed() {
        if (visualiser != null) {
            visualiser = null;
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

            // Build the arrow head in screen-space. The old 3D-offset method collapsed
            // for vertical vectors such as the Z/K axis, so the Z arrowhead disappeared.
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
                // Drag right to orbit right, drag left to orbit left.
                yaw -= (event.getX() - lastX) * 0.45f;
                // Drag up to look from above the origin, drag down to look from below.
                // Allow near-full orbit instead of stopping at the vertical axis.
                pitch -= (event.getY() - lastY) * 0.45f;
                pitch = Math.max(-170f, Math.min(170f, pitch));
                lastX = event.getX(); lastY = event.getY(); invalidate(); return true;
            }
            return true;
        }
    }
}
