package zhaf.media.themyth.ui.battle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import zhaf.media.themyth.R;
import zhaf.media.themyth.battle.engine.BattleEvent;
import zhaf.media.themyth.battle.engine.BattleResult;
import zhaf.media.themyth.battle.engine.BattleUnit;
import zhaf.media.themyth.battle.engine.StatusType;
import zhaf.media.themyth.ui.common.SpriteAnimation;
import zhaf.media.themyth.ui.common.SpriteArt;

public final class BattleFieldView extends View {

    public interface Listener {
        void onReady();

        void onLog(BattleEvent event);

        void onPlaybackEnd();
    }

    private static final long FRAME_INTERVAL_MS = 30L;
    private static final long FLOAT_DURATION_MS = 900L;
    private static final long BANNER_DURATION_MS = 1100L;
    private static final long STATUS_DURATION_MS = 5000L;
    private static final long RESULT_DELAY_MS = 1600L;
    private static final long LUNGE_APPROACH_MAX_MS = 220L;
    private static final long LUNGE_RETURN_MS = 180L;
    private static final long ACTION_PADDING_MS = 120L;

    private static final SpriteAnimation[] PRELOAD = {
            SpriteAnimation.IDLE,
            SpriteAnimation.ATTACK,
            SpriteAnimation.HURT,
            SpriteAnimation.DEAD,
            SpriteAnimation.VICTORY,
            SpriteAnimation.SKILL1,
            SpriteAnimation.SKILL2,
            SpriteAnimation.ULTIMA1
    };

    private static final float[] SLOT_X = {0.33f, 0.215f, 0.10f};
    private static final float[] SLOT_Y = {0.84f, 0.80f, 0.765f};
    private static final float[] SLOT_H = {0.46f, 0.43f, 0.40f};

    private final Paint spritePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floatPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<UnitState> states = new ArrayList<>();
    private final List<UnitState> drawOrder = new ArrayList<>();
    private final Map<String, UnitState> byUid = new HashMap<>();
    private final Map<String, Sheet> sheets = new HashMap<>();
    private final List<FloatText> floats = new ArrayList<>();

    private List<BattleEvent> events = new ArrayList<>();
    private int nextEvent;
    private long clockMs;
    private long lastFrameMs;
    private long actionReadyAt;
    private long bannerUntil;
    private long playbackEndsAt;
    private String bannerText;
    private int bannerColor;
    private float speed = 2f;
    private boolean playing;
    private boolean paused;
    private boolean ended;
    private boolean ready;
    private int pendingSheets;
    private final float density;
    private final float scaledDensity;
    private Listener listener;

    private final Runnable frameTick = new Runnable() {
        @Override
        public void run() {
            if (!playing) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            long delta = Math.max(0L, Math.min(now - lastFrameMs, 120L));
            lastFrameMs = now;
            if (!paused) {
                clockMs += Math.round(delta * speed);
                drainEvents();
                updateDisplays();
            }
            invalidate();
            if (playing) {
                handler.postDelayed(this, FRAME_INTERVAL_MS);
            }
        }
    };

    public BattleFieldView(Context context) {
        this(context, null);
    }

    public BattleFieldView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BattleFieldView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(context.getColor(R.color.text_primary));

        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        namePaint.setColor(context.getColor(R.color.text_primary));
        namePaint.setTextSize(11f * scaledDensity);
        namePaint.setShadowLayer(3f * density, 0f, 1f * density, Color.BLACK);

        floatPaint.setTextAlign(Paint.Align.CENTER);
        floatPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        bannerPaint.setTextAlign(Paint.Align.CENTER);
        bannerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        bannerPaint.setTextSize(18f * scaledDensity);
        bannerPaint.setShadowLayer(6f * density, 0f, 2f * density, Color.BLACK);

        shadowPaint.setColor(Color.parseColor("#66000000"));
        glowPaint.setStyle(Paint.Style.FILL);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setBattle(BattleResult result) {
        handler.removeCallbacks(frameTick);
        states.clear();
        drawOrder.clear();
        byUid.clear();
        sheets.clear();
        floats.clear();
        events = result == null ? new ArrayList<>() : result.events;
        nextEvent = 0;
        clockMs = 0L;
        actionReadyAt = 0L;
        playbackEndsAt = 0L;
        bannerText = null;
        bannerUntil = 0L;
        playing = false;
        paused = false;
        ended = false;
        ready = false;
        pendingSheets = 0;

        if (result != null) {
            for (BattleUnit unit : result.units) {
                UnitState state = new UnitState(unit);
                states.add(state);
                byUid.put(state.uid, state);
            }
        }
        layoutUnits();
        drawOrder.addAll(states);
        drawOrder.sort((left, right) -> Float.compare(left.baselineY, right.baselineY));
        preloadSheets();
    }

    public void play() {
        if (events.isEmpty()) {
            return;
        }
        if (playing) {
            return;
        }
        playing = true;
        paused = false;
        lastFrameMs = SystemClock.uptimeMillis();
        handler.removeCallbacks(frameTick);
        handler.post(frameTick);
    }

    public void pause() {
        paused = true;
    }

    public void resumePlayback() {
        if (playing) {
            paused = false;
            lastFrameMs = SystemClock.uptimeMillis();
        }
    }

    public void restart() {
        handler.removeCallbacks(frameTick);
        clockMs = 0L;
        nextEvent = 0;
        actionReadyAt = 0L;
        playbackEndsAt = 0L;
        bannerText = null;
        bannerUntil = 0L;
        ended = false;
        playing = false;
        paused = false;
        floats.clear();
        for (UnitState state : states) {
            state.reset(clockMs);
        }
        invalidate();
        play();
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.5f, Math.min(3f, speed));
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isEnded() {
        return ended;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (playing && !paused) {
            lastFrameMs = SystemClock.uptimeMillis();
            handler.removeCallbacks(frameTick);
            handler.post(frameTick);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(frameTick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutUnits();
    }

    private void layoutUnits() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        for (UnitState state : states) {
            int slot = Math.max(0, Math.min(2, state.slot));
            float fraction = SLOT_X[slot];
            state.centerX = width * (state.player ? fraction : 1f - fraction);
            state.baselineY = height * SLOT_Y[slot];
            state.height = height * SLOT_H[slot];
            state.barWidth = Math.min(width * 0.076f, state.height * 0.60f);
            state.spriteWidth = state.height * 0.62f;
            state.name = TextUtils.ellipsize(state.rawName, namePaint,
                    state.barWidth * 1.5f, TextUtils.TruncateAt.END).toString();
        }
        if (!drawOrder.isEmpty()) {
            drawOrder.sort((left, right) -> Float.compare(left.baselineY, right.baselineY));
        }
    }

    private void preloadSheets() {
        Set<String> sprites = new HashSet<>();
        for (UnitState state : states) {
            if (state.sprite != null && !state.sprite.isEmpty()) {
                sprites.add(state.sprite);
            }
        }
        pendingSheets = sprites.size() * PRELOAD.length;
        if (pendingSheets == 0) {
            markReady();
            return;
        }
        for (String sprite : sprites) {
            for (SpriteAnimation animation : PRELOAD) {
                SpriteArt.loadAnimation(getContext(), sprite, animation, bitmap -> {
                    if (bitmap != null) {
                        sheets.put(sheetKey(sprite, animation), new Sheet(
                                bitmap,
                                SpriteArt.columns(sprite, animation),
                                SpriteArt.rows(sprite, animation)));
                    }
                    pendingSheets--;
                    if (pendingSheets <= 0) {
                        markReady();
                    }
                });
            }
        }
    }

    private void markReady() {
        ready = true;
        invalidate();
        if (listener != null) {
            listener.onReady();
        }
    }

    private void drainEvents() {
        long limit = clockMs;
        while (nextEvent < events.size()) {
            BattleEvent event = events.get(nextEvent);
            if (event.tick * 100L > limit) {
                break;
            }
            if (event.type == BattleEvent.Type.ACTION && clockMs < actionReadyAt) {
                break;
            }
            nextEvent++;
            applyEvent(event);
        }
    }

    private void applyEvent(BattleEvent event) {
        UnitState source = event.srcUid == null ? null : byUid.get(event.srcUid);
        UnitState target = event.dstUid == null ? null : byUid.get(event.dstUid);

        switch (event.type) {
            case ACTION:
                if (source != null) {
                    SpriteAnimation animation = skillAnimation(event.skillType);
                    long animationMs = animationMs(source, animation);
                    source.play(animation, clockMs);
                    if (target != null && target.player != source.player) {
                        startLunge(source, target, animationMs);
                    }
                    actionReadyAt = clockMs + animationMs
                            + LUNGE_RETURN_MS + ACTION_PADDING_MS;
                    bannerText = event.skillName;
                    bannerColor = skillColor(event.skillType);
                    bannerUntil = clockMs + BANNER_DURATION_MS;
                }
                break;
            case HIT:
                if (target != null) {
                    target.targetHp = event.hpAfter;
                    target.energy = event.energyAfter;
                    target.play(SpriteAnimation.HURT, clockMs);
                    target.flashUntil = clockMs + 200L;
                    addFloat(target, "-" + event.value,
                            event.crit ? getContext().getColor(R.color.gold)
                                    : getContext().getColor(R.color.danger),
                            event.crit);
                }
                break;
            case DOT:
                if (target != null) {
                    target.targetHp = event.hpAfter;
                    target.energy = event.energyAfter;
                    target.play(SpriteAnimation.HURT, clockMs);
                    addFloat(target, "-" + event.value,
                            getContext().getColor(R.color.element_fire), false);
                }
                break;
            case MISS:
                if (target != null) {
                    target.energy = event.energyAfter;
                    addFloat(target, getContext().getString(R.string.battle_miss),
                            getContext().getColor(R.color.text_muted), false);
                }
                break;
            case HEAL:
                if (target != null) {
                    target.targetHp = event.hpAfter;
                    target.energy = event.energyAfter;
                    addFloat(target, "+" + event.value,
                            getContext().getColor(R.color.success), false);
                }
                break;
            case SHIELD:
                if (target != null) {
                    target.energy = event.energyAfter;
                    target.shield = event.value;
                    target.shieldUntil = clockMs + 3000L;
                    addFloat(target, "+" + event.value,
                            getContext().getColor(R.color.cyan), false);
                }
                break;
            case STATUS:
                if (target != null && event.success) {
                    StatusType type = parseStatus(event.detail);
                    if (type != null) {
                        target.statuses.put(type, clockMs + STATUS_DURATION_MS);
                        addFloatAt(target, statusLabel(type), statusColor(type), false,
                                target.barWidth * 0.85f);
                    }
                }
                break;
            case DEATH:
                if (target != null) {
                    target.alive = false;
                    target.targetHp = 0;
                    target.shield = 0;
                    target.statuses.clear();
                    target.lungeStart = -1L;
                    target.lungeOffset = 0f;
                    target.play(SpriteAnimation.DEAD, clockMs);
                    addFloat(target, getContext().getString(R.string.battle_ko),
                            getContext().getColor(R.color.danger), true);
                }
                break;
            case END:
                onBattleEnd(event);
                break;
            default:
                break;
        }

        if (listener != null) {
            listener.onLog(event);
        }
    }

    private long animationMs(UnitState state, SpriteAnimation animation) {
        Sheet sheet = sheets.get(sheetKey(state.sprite, animation));
        int frames = sheet != null
                ? Math.max(1, sheet.columns * sheet.rows)
                : Math.max(1, animation.frames());
        return frames * (long) animation.frameMs;
    }

    private void startLunge(UnitState source, UnitState target, long animationMs) {
        float dx = target.centerX - source.centerX;
        if (Math.abs(dx) < 1f) {
            return;
        }
        float contact = (source.spriteWidth + target.spriteWidth) * 0.44f;
        float distance = Math.abs(dx) - contact;
        if (distance <= 0f) {
            return;
        }
        source.startLunge(Math.signum(dx) * distance, clockMs, animationMs);
    }

    private void onBattleEnd(BattleEvent event) {
        int winner = event.value;
        for (UnitState state : states) {
            if (!state.alive) {
                continue;
            }
            boolean won = (winner == 0 && state.player) || (winner == 1 && !state.player);
            if (won) {
                state.play(SpriteAnimation.VICTORY, clockMs);
            }
        }
        playbackEndsAt = clockMs + RESULT_DELAY_MS;
    }

    private void updateDisplays() {
        for (UnitState state : states) {
            state.updateLunge(clockMs);
            if (!state.animation.loop && !state.animation.holdLast && state.lungeStart < 0L) {
                int frames = SpriteArt.frames(state.sprite, state.animation);
                if (clockMs - state.animationStart >= (long) frames * state.animation.frameMs) {
                    state.animation = SpriteAnimation.IDLE;
                    state.animationStart = clockMs;
                }
            }
            if (Math.abs(state.hp - state.targetHp) < state.maxHp * 0.002f) {
                state.hp = state.targetHp;
            } else {
                state.hp += (state.targetHp - state.hp) * 0.22f;
            }
            if (!state.alive && state.hp < 1f) {
                state.hp = 0f;
            }
            Iterator<Map.Entry<StatusType, Long>> iterator =
                    state.statuses.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue() <= clockMs) {
                    iterator.remove();
                }
            }
            if (state.shield > 0 && clockMs > state.shieldUntil) {
                state.shield = 0;
            }
        }
        Iterator<FloatText> iterator = floats.iterator();
        while (iterator.hasNext()) {
            if (clockMs - iterator.next().startMs > FLOAT_DURATION_MS) {
                iterator.remove();
            }
        }
        if (playbackEndsAt > 0 && clockMs >= playbackEndsAt && !ended) {
            ended = true;
            playing = false;
            if (listener != null) {
                listener.onPlaybackEnd();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        drawArena(canvas);
        for (UnitState state : drawOrder) {
            drawUnit(canvas, state);
        }
        drawFloats(canvas);
        drawBanner(canvas);
    }

    private void drawArena(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        barPaint.setColor(Color.parseColor("#1A22D3EE"));
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(width * 0.04f, height * 0.68f, width * 0.96f, height * 0.96f),
                barPaint);
        barPaint.setColor(Color.parseColor("#0D8B5CF6"));
        canvas.drawOval(new RectF(width * 0.14f, height * 0.72f, width * 0.86f, height * 0.90f),
                barPaint);
    }

    private void drawUnit(Canvas canvas, UnitState state) {
        float headY = state.baselineY - state.height;
        float drawX = state.centerX + state.lungeOffset;
        float shadowWidth = state.height * 0.34f;
        canvas.drawOval(new RectF(
                drawX - shadowWidth,
                state.baselineY - state.height * 0.035f,
                drawX + shadowWidth,
                state.baselineY + state.height * 0.035f), shadowPaint);

        int elementColor = elementColor(state.element);
        glowPaint.setColor(Color.argb(70, Color.red(elementColor),
                Color.green(elementColor), Color.blue(elementColor)));
        canvas.drawOval(new RectF(
                drawX - shadowWidth * 1.15f,
                state.baselineY - state.height * 0.05f,
                drawX + shadowWidth * 1.15f,
                state.baselineY + state.height * 0.05f), glowPaint);

        Sheet sheet = sheets.get(sheetKey(state.sprite, state.animation));
        if (sheet != null && !sheet.bitmap.isRecycled()) {
            int frame = state.frameIndex(clockMs, sheet.columns, sheet.rows);
            int column = frame % sheet.columns;
            int row = frame / sheet.columns;
            float cellWidth = sheet.bitmap.getWidth() / (float) sheet.columns;
            float cellHeight = sheet.bitmap.getHeight() / (float) sheet.rows;
            srcRect.set(
                    Math.round(column * cellWidth),
                    Math.round(row * cellHeight),
                    Math.round((column + 1) * cellWidth),
                    Math.round((row + 1) * cellHeight));
            float scale = state.height / cellHeight;
            float drawWidth = cellWidth * scale;
            float drawHeight = cellHeight * scale;
            state.spriteWidth = drawWidth;
            dstRect.set(
                    drawX - drawWidth / 2f,
                    state.baselineY - drawHeight,
                    drawX + drawWidth / 2f,
                    state.baselineY);

            int save = canvas.save();
            if (!state.player) {
                canvas.scale(-1f, 1f, drawX, 0f);
            }
            canvas.drawBitmap(sheet.bitmap, srcRect, dstRect, spritePaint);
            canvas.restoreToCount(save);

            if (state.flashUntil > clockMs) {
                spritePaint.setAlpha(150);
                save = canvas.save();
                if (!state.player) {
                    canvas.scale(-1f, 1f, drawX, 0f);
                }
                canvas.drawBitmap(sheet.bitmap, srcRect, dstRect, spritePaint);
                canvas.restoreToCount(save);
                spritePaint.setAlpha(255);
            }
        } else {
            state.spriteWidth = state.height * 0.62f;
            drawPlaceholder(canvas, state, drawX, headY);
        }

        drawBars(canvas, state, headY, drawX);
    }

    private void drawPlaceholder(Canvas canvas, UnitState state, float drawX, float headY) {
        float radius = state.height * 0.16f;
        barPaint.setColor(getContext().getColor(R.color.panel_elevated));
        canvas.drawCircle(drawX, state.baselineY - radius, radius, barPaint);
        textPaint.setTextSize(radius * 1.1f);
        textPaint.setColor(getContext().getColor(R.color.text_primary));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = state.baselineY - radius
                - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(state.initial, drawX, baseline, textPaint);
    }

    private void drawBars(Canvas canvas, UnitState state, float headY, float drawX) {
        float width = state.barWidth;
        float left = drawX - width / 2f;
        float hpTop = headY - 12f * density;
        float hpHeight = 5f * density;
        float radius = hpHeight / 2f;

        barPaint.setColor(Color.parseColor("#99101A33"));
        canvas.drawRoundRect(new RectF(left, hpTop, left + width, hpTop + hpHeight),
                radius, radius, barPaint);

        float hpRatio = state.maxHp <= 0 ? 0f : state.hp / state.maxHp;
        hpRatio = Math.max(0f, Math.min(1f, hpRatio));
        if (hpRatio > 0f) {
            barPaint.setColor(state.player
                    ? getContext().getColor(R.color.success)
                    : getContext().getColor(R.color.danger));
            canvas.drawRoundRect(new RectF(left, hpTop, left + width * hpRatio,
                    hpTop + hpHeight), radius, radius, barPaint);
        }

        float energyTop = hpTop + hpHeight + 3f * density;
        float energyHeight = 3f * density;
        barPaint.setColor(Color.parseColor("#99101A33"));
        canvas.drawRoundRect(new RectF(left, energyTop, left + width, energyTop + energyHeight),
                energyHeight / 2f, energyHeight / 2f, barPaint);
        float energyRatio = Math.max(0f, Math.min(1f, state.energy / 100f));
        if (energyRatio > 0f) {
            barPaint.setColor(energyRatio >= 1f
                    ? getContext().getColor(R.color.gold)
                    : getContext().getColor(R.color.cyan));
            canvas.drawRoundRect(new RectF(left, energyTop,
                    left + width * energyRatio, energyTop + energyHeight),
                    energyHeight / 2f, energyHeight / 2f, barPaint);
        }

        if (state.shield > 0) {
            float shieldTop = energyTop + energyHeight + 2f * density;
            barPaint.setColor(getContext().getColor(R.color.cyan));
            canvas.drawRoundRect(new RectF(left, shieldTop, left + width,
                    shieldTop + 2f * density), density, density, barPaint);
        }

        canvas.drawText(state.name, drawX, hpTop - 5f * density, namePaint);

        int dotIndex = 0;
        for (Map.Entry<StatusType, Long> entry : state.statuses.entrySet()) {
            if (dotIndex >= 4) {
                break;
            }
            float dotX = left + dotIndex * 9f * density;
            barPaint.setColor(statusColor(entry.getKey()));
            canvas.drawCircle(dotX, energyTop + 11f * density, 2.6f * density, barPaint);
            dotIndex++;
        }
    }

    private void drawFloats(Canvas canvas) {
        for (FloatText text : floats) {
            float progress = Math.min(1f, (clockMs - text.startMs) / (float) FLOAT_DURATION_MS);
            int alpha = (int) (255f * (1f - progress * progress));
            textPaint.setColor(text.color);
            textPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
            textPaint.setTextSize((text.big ? 22f : 15f) * scaledDensity);
            canvas.drawText(text.text, text.x, text.y - 44f * density * progress, textPaint);
        }
        textPaint.setAlpha(255);
    }

    private void drawBanner(Canvas canvas) {
        if (bannerText == null || clockMs > bannerUntil) {
            return;
        }
        long remaining = bannerUntil - clockMs;
        int alpha = (int) (255f * Math.min(1f, remaining / 350f));
        bannerPaint.setColor(bannerColor);
        bannerPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
        float y = 38f * density;
        canvas.drawText(bannerText, getWidth() / 2f, y, bannerPaint);
        bannerPaint.setAlpha(255);
    }

    private void addFloat(UnitState target, String text, int color, boolean big) {
        addFloatAt(target, text, color, big, 0f);
    }

    private void addFloatAt(UnitState target, String text, int color, boolean big,
                            float offsetX) {
        float x = target.centerX + offsetX + (floats.size() % 3 - 1) * 14f * density;
        float y = target.baselineY - target.height - 30f * density;
        floats.add(new FloatText(text, x, y, color, big, clockMs));
    }

    private SpriteAnimation skillAnimation(String skillType) {
        if ("SKILL1".equals(skillType)) {
            return SpriteAnimation.SKILL1;
        }
        if ("SKILL2".equals(skillType)) {
            return SpriteAnimation.SKILL2;
        }
        if ("ULTIMATE".equals(skillType)) {
            return SpriteAnimation.ULTIMA1;
        }
        return SpriteAnimation.ATTACK;
    }

    private int skillColor(String skillType) {
        if ("ULTIMATE".equals(skillType)) {
            return getContext().getColor(R.color.gold);
        }
        if ("SKILL2".equals(skillType)) {
            return getContext().getColor(R.color.purple_light);
        }
        if ("SKILL1".equals(skillType)) {
            return getContext().getColor(R.color.cyan);
        }
        return getContext().getColor(R.color.text_primary);
    }

    private String statusLabel(StatusType type) {
        switch (type) {
            case BURN:
                return getContext().getString(R.string.status_burn);
            case BLEED:
                return getContext().getString(R.string.status_bleed);
            case WET:
                return getContext().getString(R.string.status_wet);
            case SHOCK:
                return getContext().getString(R.string.status_shock);
            case STUN:
                return getContext().getString(R.string.status_stun);
            case ATK_UP:
                return getContext().getString(R.string.status_atk_up);
            case DEF_DOWN:
                return getContext().getString(R.string.status_def_down);
            default:
                return type.name();
        }
    }

    private int statusColor(StatusType type) {
        switch (type) {
            case BURN:
                return getContext().getColor(R.color.element_fire);
            case BLEED:
                return getContext().getColor(R.color.danger);
            case WET:
                return getContext().getColor(R.color.element_water);
            case SHOCK:
                return getContext().getColor(R.color.element_lightning);
            case STUN:
                return getContext().getColor(R.color.gold);
            case ATK_UP:
                return getContext().getColor(R.color.success);
            case DEF_DOWN:
                return getContext().getColor(R.color.purple_light);
            default:
                return getContext().getColor(R.color.text_muted);
        }
    }

    private int elementColor(String element) {
        if (element == null) {
            return getContext().getColor(R.color.cyan);
        }
        switch (element) {
            case "fire":
                return getContext().getColor(R.color.element_fire);
            case "water":
                return getContext().getColor(R.color.element_water);
            case "wind":
                return getContext().getColor(R.color.element_wind);
            case "earth":
                return getContext().getColor(R.color.element_earth);
            case "lightning":
                return getContext().getColor(R.color.element_lightning);
            default:
                return getContext().getColor(R.color.cyan);
        }
    }

    private static StatusType parseStatus(String detail) {
        if (detail == null) {
            return null;
        }
        try {
            return StatusType.valueOf(detail);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static String sheetKey(String sprite, SpriteAnimation animation) {
        return (sprite == null ? "" : sprite) + "|" + animation.name();
    }

    private static final class Sheet {
        final Bitmap bitmap;
        final int columns;
        final int rows;

        Sheet(Bitmap bitmap, int columns, int rows) {
            this.bitmap = bitmap;
            this.columns = columns;
            this.rows = rows;
        }
    }

    private static final class FloatText {
        final String text;
        final float x;
        final float y;
        final int color;
        final boolean big;
        final long startMs;

        FloatText(String text, float x, float y, int color, boolean big, long startMs) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.big = big;
            this.startMs = startMs;
        }
    }

    private static final class UnitState {
        final String uid;
        final String rawName;
        final String initial;
        final String sprite;
        final String element;
        final boolean player;
        final int slot;
        final int maxHp;
        final Map<StatusType, Long> statuses = new LinkedHashMap<>();

        String name;
        float centerX;
        float baselineY;
        float height;
        float barWidth;
        float spriteWidth;
        float lungeOffset;
        float lungeTo;
        long lungeStart = -1L;
        long lungeApproachMs;
        long lungeReturnAt;
        float hp;
        int targetHp;
        int energy;
        int shield;
        long shieldUntil;
        long flashUntil;
        boolean alive;
        SpriteAnimation animation = SpriteAnimation.IDLE;
        long animationStart;

        UnitState(BattleUnit unit) {
            uid = unit.uid;
            rawName = unit.name == null ? "" : unit.name;
            initial = rawName.isEmpty()
                    ? "?"
                    : rawName.substring(0, 1).toUpperCase(Locale.ROOT);
            name = rawName;
            sprite = unit.sprite;
            element = unit.element;
            player = unit.team == 0;
            slot = unit.slot;
            maxHp = unit.maxHp;
            hp = unit.maxHp;
            targetHp = unit.maxHp;
            energy = BattleUnit.START_ENERGY;
            alive = true;
        }

        void play(SpriteAnimation next, long now) {
            if (!alive && next != SpriteAnimation.DEAD) {
                return;
            }
            if (animation == SpriteAnimation.DEAD && next != SpriteAnimation.DEAD) {
                return;
            }
            animation = next;
            animationStart = now;
        }

        void startLunge(float distance, long now, long animationMs) {
            lungeTo = distance;
            lungeStart = now;
            lungeApproachMs = Math.max(1L, Math.min(LUNGE_APPROACH_MAX_MS, animationMs / 3L));
            lungeReturnAt = now + Math.max(animationMs, lungeApproachMs);
        }

        void updateLunge(long now) {
            if (lungeStart < 0L) {
                lungeOffset = 0f;
                return;
            }
            if (now < lungeStart + lungeApproachMs) {
                float k = (now - lungeStart) / (float) lungeApproachMs;
                float eased = 1f - (1f - k) * (1f - k) * (1f - k);
                lungeOffset = lungeTo * eased;
                return;
            }
            if (now < lungeReturnAt) {
                lungeOffset = lungeTo;
                return;
            }
            float k = (now - lungeReturnAt) / (float) LUNGE_RETURN_MS;
            if (k >= 1f) {
                lungeStart = -1L;
                lungeOffset = 0f;
                return;
            }
            float eased = k * k * (3f - 2f * k);
            lungeOffset = lungeTo * (1f - eased);
        }

        int frameIndex(long now, int columns, int rows) {
            int frames = Math.max(1, columns * rows);
            long elapsed = Math.max(0L, now - animationStart);
            int frame = (int) (elapsed / animation.frameMs);
            if (animation.loop) {
                return frame % frames;
            }
            if (frame >= frames) {
                return animation.holdLast || lungeStart >= 0L ? frames - 1 : 0;
            }
            return frame;
        }

        void reset(long now) {
            hp = maxHp;
            targetHp = maxHp;
            energy = BattleUnit.START_ENERGY;
            shield = 0;
            shieldUntil = 0L;
            flashUntil = 0L;
            alive = true;
            statuses.clear();
            animation = SpriteAnimation.IDLE;
            animationStart = now;
            lungeStart = -1L;
            lungeOffset = 0f;
            lungeTo = 0f;
            lungeApproachMs = 0L;
            lungeReturnAt = 0L;
        }
    }
}
