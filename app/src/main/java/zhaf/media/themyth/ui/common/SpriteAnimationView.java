package zhaf.media.themyth.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import zhaf.media.themyth.R;

public final class SpriteAnimationView extends View {

    private static final long FRAME_DELAY_MS = 110L;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            frame = (frame + 1) % SpriteArt.IDLE_FRAMES;
            invalidate();
            schedule();
        }
    };

    private Bitmap sheet;
    private String requestedSprite;
    private String label = "";
    private int frame;

    public SpriteAnimationView(Context context) {
        this(context, null);
    }

    public SpriteAnimationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpriteAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(context.getColor(R.color.text_primary));
    }

    public void setCharacter(String sprite, String initial) {
        label = initial == null ? "" : initial;
        if (sprite == null || sprite.isEmpty()) {
            requestedSprite = null;
            sheet = null;
            invalidate();
            return;
        }
        if (sprite.equals(requestedSprite)) {
            return;
        }
        requestedSprite = sprite;
        sheet = null;
        frame = 0;
        invalidate();
        SpriteArt.loadIdle(getContext(), sprite, loaded -> {
            if (sprite.equals(requestedSprite)) {
                sheet = loaded;
                frame = 0;
                invalidate();
                schedule();
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        schedule();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void schedule() {
        handler.removeCallbacks(tick);
        if (sheet != null && isAttachedToWindow()) {
            handler.postDelayed(tick, FRAME_DELAY_MS);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sheet == null || sheet.isRecycled() || getWidth() == 0 || getHeight() == 0) {
            drawLabel(canvas);
            return;
        }

        float cellWidth = sheet.getWidth() / (float) SpriteArt.IDLE_COLUMNS;
        float cellHeight = sheet.getHeight() / (float) SpriteArt.IDLE_ROWS;
        int column = frame % SpriteArt.IDLE_COLUMNS;
        int row = frame / SpriteArt.IDLE_COLUMNS;

        srcRect.set(
                Math.round(column * cellWidth),
                Math.round(row * cellHeight),
                Math.round((column + 1) * cellWidth),
                Math.round((row + 1) * cellHeight));

        float scale = Math.min(getWidth() / cellWidth, getHeight() / cellHeight);
        float width = cellWidth * scale;
        float height = cellHeight * scale;
        float left = (getWidth() - width) / 2f;
        float top = (getHeight() - height) / 2f;
        dstRect.set(left, top, left + width, top + height);

        canvas.drawBitmap(sheet, srcRect, dstRect, bitmapPaint);
    }

    private void drawLabel(Canvas canvas) {
        if (label.isEmpty()) {
            return;
        }
        textPaint.setTextSize(Math.min(getWidth(), getHeight()) * 0.3f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, getWidth() / 2f, baseline, textPaint);
    }
}
