package uri.app.kickflip;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class BoardView extends View {

    // ---- Modes ----
    public static final int MODE_DRAW    = 0;
    public static final int MODE_STICKER = 1;

    // ---- Shapes ----
    public static final int SHAPE_POPSICLE  = 0;
    public static final int SHAPE_OLDSCHOOL = 1;
    public static final int SHAPE_CRUISER   = 2;
    public static final int SHAPE_FISH      = 3;

    // ---- Grip ----
    public static final int GRIP_NONE    = 0;
    public static final int GRIP_BLACK   = 1;
    public static final int GRIP_COLORED = 2;

    // ---- Stickers ----
    public static final int STICKER_STAR      = 0;
    public static final int STICKER_SKULL     = 1;
    public static final int STICKER_LIGHTNING = 2;
    public static final int STICKER_FIRE      = 3;
    public static final int STICKER_CROWN     = 4;
    public static final int STICKER_HEART     = 5;
    public static final int STICKER_DIAMOND   = 6;
    public static final int STICKER_CROSS     = 7;

    // ---- State ----
    private int   mode        = MODE_DRAW;
    private int   boardShape  = SHAPE_POPSICLE;
    private int   deckColor   = Color.parseColor("#3B82F6");
    private int   truckColor  = Color.parseColor("#888888");
    private int   wheelColor  = Color.parseColor("#EEEEEE");
    private int   gripStyle   = GRIP_BLACK;
    private int   gripColor   = Color.parseColor("#222222");
    private float brushSize   = 6f;
    private int   drawColor   = Color.WHITE;
    private int   stickerType = STICKER_STAR;
    private float stickerSize = 30f;

    // ---- Draw strokes ----
    private static class Stroke {
        Path  path;
        int   color;
        float size;
        Stroke(Path p, int c, float s) { path = p; color = c; size = s; }
    }
    private final List<Stroke> strokes = new ArrayList<>();
    private Path currentPath;

    // ---- Stickers ----
    private static class Sticker {
        int   type;
        float x, y, size;
        Sticker(int t, float x, float y, float s) { type=t; this.x=x; this.y=y; size=s; }
    }
    private final List<Sticker> stickers = new ArrayList<>();
    private Sticker dragged;
    private float   dragDx, dragDy;

    // ---- Paints ----
    private final Paint deckPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gripPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint truckPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BoardView(Context c)                        { super(c); init(); }
    public BoardView(Context c, AttributeSet a)        { super(c,a); init(); }
    public BoardView(Context c, AttributeSet a, int s) { super(c,a,s); init(); }

    private void init() {
        deckPaint.setStyle(Paint.Style.FILL);

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(Color.parseColor("#222222"));
        outlinePaint.setStrokeWidth(3.5f);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setAntiAlias(true);

        stickerPaint.setStyle(Paint.Style.FILL);
        stickerPaint.setAntiAlias(true);

        gripPaint.setStyle(Paint.Style.STROKE);
        gripPaint.setStrokeWidth(0.8f);
        gripPaint.setAntiAlias(true);

        truckPaint.setStyle(Paint.Style.FILL);
        truckPaint.setAntiAlias(true);

        wheelPaint.setStyle(Paint.Style.FILL);
        wheelPaint.setAntiAlias(true);

        wheelRingPaint.setStyle(Paint.Style.STROKE);
        wheelRingPaint.setStrokeWidth(2f);
        wheelRingPaint.setAntiAlias(true);
    }

    // ---- Public API ----
    public void setMode(int m)          { mode = m; }
    public void setShape(int s)         { boardShape = s; invalidate(); }
    public void setFillColor(int c)     { deckColor = c; invalidate(); }
    public void setTruckColor(int c)    { truckColor = c; invalidate(); }
    public void setWheelColor(int c)    { wheelColor = c; invalidate(); }
    public void setGripStyle(int s)     { gripStyle = s; invalidate(); }
    public void setGripColor(int c)     { gripColor = c; invalidate(); }
    public void setDrawColor(int c)     { drawColor = c; }
    public void setBrushSize(float s)   { brushSize = s; }
    public void setStickerType(int t)   { stickerType = t; }
    public void setStickerSize(float s) { stickerSize = s; }

    public int   getFillColor()   { return deckColor; }
    public int   getTruckColor()  { return truckColor; }
    public int   getWheelColor()  { return wheelColor; }
    public int   getGripStyle()   { return gripStyle; }
    public int   getGripColor()   { return gripColor; }
    public int   getBoardShape()  { return boardShape; }

    public void undo() {
        if (mode == MODE_STICKER && !stickers.isEmpty()) {
            stickers.remove(stickers.size() - 1);
        } else if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
        }
        invalidate();
    }

    public void clear() {
        strokes.clear();
        stickers.clear();
        currentPath = null;
        invalidate();
    }

    // ---- Board shapes ----
    private Path buildDeck(float cx, float cy, float w, float h) {
        switch (boardShape) {
            case SHAPE_OLDSCHOOL: return buildOldSchool(cx, cy, w, h);
            case SHAPE_CRUISER:   return buildCruiser(cx, cy, w, h);
            case SHAPE_FISH:      return buildFish(cx, cy, w, h);
            default:              return buildPopsicle(cx, cy, w, h);
        }
    }

    private Path buildPopsicle(float cx, float cy, float w, float h) {
        Path p = new Path();
        float hw = w/2f, hh = h/2f, taper = w*0.14f, waist = w*0.04f;
        p.moveTo(cx, cy-hh);
        p.cubicTo(cx+hw*0.5f, cy-hh, cx+hw-taper+waist, cy-hh*0.3f, cx+hw, cy);
        p.cubicTo(cx+hw-waist, cy+hh*0.3f, cx+hw*0.5f, cy+hh, cx, cy+hh);
        p.cubicTo(cx-hw*0.5f, cy+hh, cx-hw+waist, cy+hh*0.3f, cx-hw, cy);
        p.cubicTo(cx-hw+taper-waist, cy-hh*0.3f, cx-hw*0.5f, cy-hh, cx, cy-hh);
        p.close();
        return p;
    }

    private Path buildOldSchool(float cx, float cy, float w, float h) {
        // Wide flat nose, narrower rounded tail
        Path p = new Path();
        float hw = w*0.54f, hh = h/2f;
        p.moveTo(cx-hw*0.85f, cy-hh);
        p.lineTo(cx+hw*0.85f, cy-hh);
        p.cubicTo(cx+hw, cy-hh, cx+hw, cy-hh*0.5f, cx+hw, cy-hh*0.1f);
        p.cubicTo(cx+hw, cy+hh*0.5f, cx+hw*0.5f, cy+hh, cx, cy+hh);
        p.cubicTo(cx-hw*0.5f, cy+hh, cx-hw, cy+hh*0.5f, cx-hw, cy-hh*0.1f);
        p.cubicTo(cx-hw, cy-hh*0.5f, cx-hw, cy-hh, cx-hw*0.85f, cy-hh);
        p.close();
        return p;
    }

    private Path buildCruiser(float cx, float cy, float w, float h) {
        // Wide oval, relaxed curves
        Path p = new Path();
        float hw = w*0.56f, hh = h/2f;
        p.moveTo(cx, cy-hh);
        p.cubicTo(cx+hw*0.65f, cy-hh, cx+hw, cy-hh*0.55f, cx+hw, cy);
        p.cubicTo(cx+hw, cy+hh*0.55f, cx+hw*0.65f, cy+hh, cx, cy+hh);
        p.cubicTo(cx-hw*0.65f, cy+hh, cx-hw, cy+hh*0.55f, cx-hw, cy);
        p.cubicTo(cx-hw, cy-hh*0.55f, cx-hw*0.65f, cy-hh, cx, cy-hh);
        p.close();
        return p;
    }

    private Path buildFish(float cx, float cy, float w, float h) {
        // Pointed nose, wide fishtail with V cutout
        Path p = new Path();
        float hw = w*0.57f, hh = h/2f, vd = hh*0.26f;
        p.moveTo(cx, cy-hh);                             // pointed nose
        p.cubicTo(cx+hw*0.4f, cy-hh*0.8f, cx+hw, cy-hh*0.2f, cx+hw, cy+hh*0.15f);
        p.cubicTo(cx+hw, cy+hh*0.65f, cx+hw*0.72f, cy+hh, cx+hw*0.26f, cy+hh);
        p.cubicTo(cx+hw*0.08f, cy+hh, cx+hw*0.02f, cy+hh-vd*0.5f, cx, cy+hh-vd);
        p.cubicTo(cx-hw*0.02f, cy+hh-vd*0.5f, cx-hw*0.08f, cy+hh, cx-hw*0.26f, cy+hh);
        p.cubicTo(cx-hw*0.72f, cy+hh, cx-hw, cy+hh*0.65f, cx-hw, cy+hh*0.15f);
        p.cubicTo(cx-hw, cy-hh*0.2f, cx-hw*0.4f, cy-hh*0.8f, cx, cy-hh);
        p.close();
        return p;
    }

    // ---- Draw ----
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float W = getWidth(), H = getHeight();
        float dw = W * 0.54f, dh = H * 0.82f;
        float cx = W/2f, cy = H/2f;

        Path deck = buildDeck(cx, cy, dw, dh);

        // Deck fill
        deckPaint.setColor(deckColor);
        canvas.drawPath(deck, deckPaint);

        // Grip tape
        drawGrip(canvas, deck, cx, cy, dw, dh);

        // Trucks & wheels (drawn OUTSIDE clip so wheels peek out)
        drawHardware(canvas, cx, cy, dw, dh);

        // Board outline
        canvas.drawPath(deck, outlinePaint);

        // User art — clipped to deck
        canvas.save();
        canvas.clipPath(deck);

        // Completed strokes
        for (Stroke s : strokes) {
            strokePaint.setColor(s.color);
            strokePaint.setStrokeWidth(s.size);
            canvas.drawPath(s.path, strokePaint);
        }
        // In-progress stroke
        if (currentPath != null) {
            strokePaint.setColor(drawColor);
            strokePaint.setStrokeWidth(brushSize);
            canvas.drawPath(currentPath, strokePaint);
        }

        // Stickers
        for (Sticker s : stickers) drawSticker(canvas, s);

        canvas.restore();
    }

    private void drawGrip(Canvas canvas, Path deck, float cx, float cy, float dw, float dh) {
        if (gripStyle == GRIP_NONE) return;
        canvas.save();
        canvas.clipPath(deck);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        int baseColor = gripStyle == GRIP_COLORED ? gripColor : 0xFF000000;
        fill.setColor((baseColor & 0x00FFFFFF) | 0x36000000); // ~21% alpha overlay
        fill.setStyle(Paint.Style.FILL);
        canvas.drawPaint(fill);

        // Fine dot grid to simulate grip texture
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor((baseColor & 0x00FFFFFF) | 0x28000000);
        dot.setStyle(Paint.Style.FILL);
        float spacing = 9f;
        for (float y = cy - dh/2f; y < cy + dh/2f; y += spacing) {
            for (float x = cx - dw/2f; x < cx + dw/2f; x += spacing) {
                canvas.drawCircle(x, y, 1.2f, dot);
            }
        }
        canvas.restore();
    }

    private void drawHardware(Canvas canvas, float cx, float cy, float dw, float dh) {
        float axleW = dw * 0.72f, axleH = 9f;
        float ax = cx - axleW/2f;
        float truckOffset = (boardShape == SHAPE_OLDSCHOOL) ? 0.32f : 0.37f;
        float noseTY = cy - dh * truckOffset;
        float tailTY = cy + dh * truckOffset;

        // Axles
        truckPaint.setColor(truckColor);
        RectF noseAxle = new RectF(ax, noseTY-axleH/2f, ax+axleW, noseTY+axleH/2f);
        RectF tailAxle = new RectF(ax, tailTY-axleH/2f, ax+axleW, tailTY+axleH/2f);
        canvas.drawRoundRect(noseAxle, 5, 5, truckPaint);
        canvas.drawRoundRect(tailAxle, 5, 5, truckPaint);

        // Baseplate bolt (centre)
        truckPaint.setColor(darken(truckColor, 0.75f));
        canvas.drawCircle(cx, noseTY, 4f, truckPaint);
        canvas.drawCircle(cx, tailTY, 4f, truckPaint);

        // Wheels
        float wr = 13.5f;
        float wx1 = ax - wr * 0.55f, wx2 = ax + axleW + wr * 0.55f;
        wheelPaint.setColor(wheelColor);
        canvas.drawCircle(wx1, noseTY, wr, wheelPaint);
        canvas.drawCircle(wx2, noseTY, wr, wheelPaint);
        canvas.drawCircle(wx1, tailTY, wr, wheelPaint);
        canvas.drawCircle(wx2, tailTY, wr, wheelPaint);

        // Wheel rings
        wheelRingPaint.setColor(darken(wheelColor, 0.72f));
        float rr = wr * 0.58f;
        canvas.drawCircle(wx1, noseTY, rr, wheelRingPaint);
        canvas.drawCircle(wx2, noseTY, rr, wheelRingPaint);
        canvas.drawCircle(wx1, tailTY, rr, wheelRingPaint);
        canvas.drawCircle(wx2, tailTY, rr, wheelRingPaint);

        // Wheel hub dot
        wheelRingPaint.setStyle(Paint.Style.FILL);
        wheelRingPaint.setColor(darken(wheelColor, 0.6f));
        canvas.drawCircle(wx1, noseTY, 3.5f, wheelRingPaint);
        canvas.drawCircle(wx2, noseTY, 3.5f, wheelRingPaint);
        canvas.drawCircle(wx1, tailTY, 3.5f, wheelRingPaint);
        canvas.drawCircle(wx2, tailTY, 3.5f, wheelRingPaint);
        wheelRingPaint.setStyle(Paint.Style.STROKE);
    }

    private int darken(int color, float f) {
        return Color.rgb(
            Math.max(0, (int)(Color.red(color)   * f)),
            Math.max(0, (int)(Color.green(color) * f)),
            Math.max(0, (int)(Color.blue(color)  * f))
        );
    }

    // ---- Sticker rendering ----
    private void drawSticker(Canvas canvas, Sticker s) {
        switch (s.type) {
            case STICKER_STAR:      drawStar(canvas, s.x, s.y, s.size); break;
            case STICKER_SKULL:     drawSkull(canvas, s.x, s.y, s.size); break;
            case STICKER_LIGHTNING: drawLightning(canvas, s.x, s.y, s.size); break;
            case STICKER_FIRE:      drawFire(canvas, s.x, s.y, s.size); break;
            case STICKER_CROWN:     drawCrown(canvas, s.x, s.y, s.size); break;
            case STICKER_HEART:     drawHeart(canvas, s.x, s.y, s.size); break;
            case STICKER_DIAMOND:   drawDiamond(canvas, s.x, s.y, s.size); break;
            case STICKER_CROSS:     drawCross(canvas, s.x, s.y, s.size); break;
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFFF59E0B);
        Path p = new Path();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI/5*i - Math.PI/2;
            float rad = (i%2==0) ? r : r*0.42f;
            float px = cx + (float)(Math.cos(a)*rad), py = cy + (float)(Math.sin(a)*rad);
            if (i==0) p.moveTo(px,py); else p.lineTo(px,py);
        }
        p.close();
        canvas.drawPath(p, stickerPaint);
    }

    private void drawSkull(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy-r*0.1f, r*0.7f, stickerPaint);
        canvas.drawRoundRect(new RectF(cx-r*.44f,cy+r*.35f,cx+r*.44f,cy+r*.72f), r*.14f, r*.14f, stickerPaint);
        stickerPaint.setColor(Color.BLACK);
        canvas.drawCircle(cx-r*.23f, cy-r*.15f, r*.17f, stickerPaint);
        canvas.drawCircle(cx+r*.23f, cy-r*.15f, r*.17f, stickerPaint);
        canvas.drawCircle(cx, cy+r*.12f, r*.09f, stickerPaint);
    }

    private void drawLightning(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFFF59E0B);
        Path p = new Path();
        p.moveTo(cx+r*.2f, cy-r); p.lineTo(cx-r*.2f, cy); p.lineTo(cx+r*.15f, cy);
        p.lineTo(cx-r*.2f, cy+r); p.lineTo(cx+r*.2f, cy); p.lineTo(cx-r*.15f, cy);
        p.close();
        canvas.drawPath(p, stickerPaint);
    }

    private void drawFire(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFFEF4444);
        Path p = new Path();
        p.moveTo(cx, cy+r);
        p.cubicTo(cx-r, cy+r*.3f, cx-r*.5f, cy-r*.2f, cx, cy-r);
        p.cubicTo(cx+r*.5f, cy-r*.2f, cx+r, cy+r*.3f, cx, cy+r);
        p.close();
        canvas.drawPath(p, stickerPaint);
        stickerPaint.setColor(0xFFF59E0B);
        Path i2 = new Path();
        i2.moveTo(cx, cy+r*.7f);
        i2.cubicTo(cx-r*.4f, cy+r*.2f, cx-r*.2f, cy-r*.2f, cx, cy-r*.4f);
        i2.cubicTo(cx+r*.2f, cy-r*.2f, cx+r*.4f, cy+r*.2f, cx, cy+r*.7f);
        i2.close();
        canvas.drawPath(i2, stickerPaint);
    }

    private void drawCrown(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFFF59E0B);
        Path p = new Path();
        p.moveTo(cx-r, cy+r*.3f); p.lineTo(cx+r, cy+r*.3f); p.lineTo(cx+r, cy);
        p.lineTo(cx+r*.6f, cy-r*.6f); p.lineTo(cx+r*.3f, cy);
        p.lineTo(cx, cy-r); p.lineTo(cx-r*.3f, cy);
        p.lineTo(cx-r*.6f, cy-r*.6f); p.lineTo(cx-r, cy);
        p.close();
        canvas.drawPath(p, stickerPaint);
    }

    private void drawHeart(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFFEF4444);
        Path p = new Path();
        p.moveTo(cx, cy+r*.85f);
        p.cubicTo(cx-r*1.4f, cy+r*.3f, cx-r*1.4f, cy-r*.6f, cx, cy-r*.15f);
        p.cubicTo(cx+r*1.4f, cy-r*.6f, cx+r*1.4f, cy+r*.3f, cx, cy+r*.85f);
        p.close();
        canvas.drawPath(p, stickerPaint);
    }

    private void drawDiamond(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(0xFF3B82F6);
        Path p = new Path();
        p.moveTo(cx, cy-r); p.lineTo(cx+r*.72f, cy); p.lineTo(cx, cy+r); p.lineTo(cx-r*.72f, cy);
        p.close();
        canvas.drawPath(p, stickerPaint);
        stickerPaint.setColor(0xFF7BB4F8);
        Path hi = new Path();
        hi.moveTo(cx, cy-r*.52f); hi.lineTo(cx+r*.35f, cy-r*.1f);
        hi.lineTo(cx, cy+r*.52f); hi.lineTo(cx-r*.35f, cy-r*.1f);
        hi.close();
        canvas.drawPath(hi, stickerPaint);
    }

    private void drawCross(Canvas canvas, float cx, float cy, float r) {
        stickerPaint.setColor(Color.WHITE);
        float a = r*.36f;
        Path p = new Path();
        p.moveTo(cx-a, cy-r); p.lineTo(cx+a, cy-r); p.lineTo(cx+a, cy-a);
        p.lineTo(cx+r, cy-a); p.lineTo(cx+r, cy+a); p.lineTo(cx+a, cy+a);
        p.lineTo(cx+a, cy+r); p.lineTo(cx-a, cy+r); p.lineTo(cx-a, cy+a);
        p.lineTo(cx-r, cy+a); p.lineTo(cx-r, cy-a); p.lineTo(cx-a, cy-a);
        p.close();
        canvas.drawPath(p, stickerPaint);
    }

    // ---- Touch ----
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        float tx = event.getX(), ty = event.getY();
        if (mode == MODE_DRAW) handleDraw(event, tx, ty);
        else                   handleSticker(event, tx, ty);
        return true;
    }

    private void handleDraw(MotionEvent ev, float tx, float ty) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(tx, ty);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) { currentPath.lineTo(tx, ty); invalidate(); }
                break;
            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    currentPath.lineTo(tx, ty);
                    strokes.add(new Stroke(currentPath, drawColor, brushSize));
                    currentPath = null;
                    invalidate();
                }
                break;
        }
    }

    private void handleSticker(MotionEvent ev, float tx, float ty) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragged = findSticker(tx, ty);
                if (dragged != null) { dragDx = tx-dragged.x; dragDy = ty-dragged.y; }
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragged != null) { dragged.x = tx-dragDx; dragged.y = ty-dragDy; invalidate(); }
                break;
            case MotionEvent.ACTION_UP:
                if (dragged == null) { stickers.add(new Sticker(stickerType, tx, ty, stickerSize)); invalidate(); }
                dragged = null;
                break;
        }
    }

    private Sticker findSticker(float x, float y) {
        for (int i = stickers.size()-1; i >= 0; i--) {
            Sticker s = stickers.get(i);
            float dx = x-s.x, dy = y-s.y;
            if (dx*dx+dy*dy <= (s.size+14)*(s.size+14)) return s;
        }
        return null;
    }
}
