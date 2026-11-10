package zhaf.media.themyth.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import zhaf.media.themyth.R;

public final class CharacterPortraitView extends View {

    private static final float BUST_LEFT = 0.15f;
    private static final float BUST_TOP = 0.02f;
    private static final float BUST_WIDTH = 0.70f;
    private static final float BUST_HEIGHT = 0.58f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF clipBounds = new RectF();
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();

    private Bitmap bitmap;
    private String label = "";
    private String requestedSprite;

    public CharacterPortraitView(Context context) {
        this(context, null);
    }

    public CharacterPortraitView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CharacterPortraitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(context.getColor(R.color.text_primary));
    }

    public void setCharacter(String sprite, String initial) {
        label = initial == null ? "" : initial;
        if (sprite == null || sprite.isEmpty()) {
            requestedSprite = null;
            bitmap = null;
            invalidate();
            return;
        }
        if (sprite.equals(requestedSprite)) {
            return;
        }
        requestedSprite = sprite;
        bitmap = null;
        invalidate();
        SpriteArt.loadPortrait(getContext(), sprite, loaded -> {
            if (sprite.equals(requestedSprite)) {
                bitmap = loaded;
                invalidate();
            }
        });
    }

    public void showPlaceholder(String text) {
        requestedSprite = null;
        bitmap = null;
        label = text == null ? "" : text;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || bitmap.isRecycled() || getWidth() == 0 || getHeight() == 0) {
            drawLabel(canvas);
            return;
        }

        float radius = dp(12);
        clipBounds.set(0, 0, getWidth(), getHeight());
        clip.reset();
        clip.addRoundRect(clipBounds, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        cropBust();
        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint);
        canvas.restore();
    }

    private void cropBust() {
        float sourceWidth = bitmap.getWidth() * BUST_WIDTH;
        float sourceHeight = bitmap.getHeight() * BUST_HEIGHT;
        float left = bitmap.getWidth() * BUST_LEFT;
        float top = bitmap.getHeight() * BUST_TOP;

        float scale = Math.max(getWidth() / sourceWidth, getHeight() / sourceHeight);
        float cropWidth = getWidth() / scale;
        float cropHeight = getHeight() / scale;
        float cropLeft = left + (sourceWidth - cropWidth) / 2f;

        srcRect.set(
                Math.round(cropLeft),
                Math.round(top),
                Math.round(cropLeft + cropWidth),
                Math.round(top + cropHeight));
        dstRect.set(0, 0, getWidth(), getHeight());
    }

    private void drawLabel(Canvas canvas) {
        if (label.isEmpty()) {
            return;
        }
        textPaint.setTextSize(Math.min(getWidth(), getHeight()) * 0.42f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, getWidth() / 2f, baseline, textPaint);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
