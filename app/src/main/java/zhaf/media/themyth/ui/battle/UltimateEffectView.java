package zhaf.media.themyth.ui.battle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import zhaf.media.themyth.ui.common.SpriteArt;

public final class UltimateEffectView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private Bitmap bitmap;
    private int frameIndex;
    private boolean mirrored;

    public UltimateEffectView(Context context) {
        this(context, null);
    }

    public UltimateEffectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UltimateEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setFrame(Bitmap bitmap, int frameIndex, boolean mirrored) {
        if (bitmap == null || bitmap.isRecycled() || frameIndex < 0
                || frameIndex >= SpriteArt.EFFECT_FRAMES) {
            clear();
            return;
        }
        if (this.bitmap == bitmap && this.frameIndex == frameIndex && this.mirrored == mirrored) {
            return;
        }
        this.bitmap = bitmap;
        this.frameIndex = frameIndex;
        this.mirrored = mirrored;
        postInvalidateOnAnimation();
    }

    public void clear() {
        if (bitmap != null) {
            bitmap = null;
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap sheet = bitmap;
        if (sheet == null || sheet.isRecycled() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int cellWidth = sheet.getWidth() / SpriteArt.EFFECT_COLUMNS;
        int cellHeight = sheet.getHeight() / SpriteArt.EFFECT_ROWS;
        int column = frameIndex % SpriteArt.EFFECT_COLUMNS;
        int row = frameIndex / SpriteArt.EFFECT_COLUMNS;
        source.set(column * cellWidth, row * cellHeight,
                (column + 1) * cellWidth, (row + 1) * cellHeight);
        destination.set(0f, 0f, getWidth(), getHeight());
        int save = canvas.save();
        if (mirrored) {
            canvas.scale(-1f, 1f, getWidth() / 2f, 0f);
        }
        canvas.drawBitmap(sheet, source, destination, paint);
        canvas.restoreToCount(save);
    }

    @Override
    protected void onDetachedFromWindow() {
        clear();
        super.onDetachedFromWindow();
    }
}
