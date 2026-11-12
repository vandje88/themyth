package zhaf.media.themyth.core;

import android.view.View;

public final class UiAnim {

    private UiAnim() {
    }

    public static void fadeIn(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(180L).start();
    }

    public static void pop(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(90L)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .start())
                .start();
    }
}
