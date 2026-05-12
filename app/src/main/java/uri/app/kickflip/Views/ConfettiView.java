package uri.app.kickflip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiView extends View {

    private static final int PARTICLE_COUNT = 40;
    private static final long DURATION_MS = 1500;

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress = 0f; // 0..1

    private static final int[] COLORS = {
            Color.parseColor("#3B82F6"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#F97316"),
            Color.parseColor("#FFFFFF"),
    };

    private static class Particle {
        float startX, startY;
        float velX, velY;
        float size;
        int color;
        float rotation;
        float rotSpeed;
        boolean isCircle;
    }

    public ConfettiView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void initParticles(int w, int h) {
        particles.clear();
        Random rng = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            // Start from center-top area
            p.startX = w * 0.2f + rng.nextFloat() * w * 0.6f;
            p.startY = h * 0.25f + rng.nextFloat() * h * 0.15f;
            // Burst outward + downward
            double angle = rng.nextDouble() * Math.PI * 2;
            float speed = 300f + rng.nextFloat() * 600f;
            p.velX = (float) (Math.cos(angle) * speed);
            p.velY = (float) (Math.sin(angle) * speed * 0.5f) - 200f; // upward bias
            p.size = 8f + rng.nextFloat() * 16f;
            p.color = COLORS[rng.nextInt(COLORS.length)];
            p.rotation = rng.nextFloat() * 360f;
            p.rotSpeed = (rng.nextFloat() - 0.5f) * 720f;
            p.isCircle = rng.nextBoolean();
            particles.add(p);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float t = progress;
        float gravity = 800f; // pixels per sec^2
        float elapsed = t * (DURATION_MS / 1000f); // seconds

        for (Particle p : particles) {
            float x = p.startX + p.velX * elapsed;
            float y = p.startY + p.velY * elapsed + 0.5f * gravity * elapsed * elapsed;
            float rot = p.rotation + p.rotSpeed * elapsed;

            int alpha = (int) (255 * (1f - t * t)); // fade out
            paint.setColor(p.color);
            paint.setAlpha(Math.max(0, alpha));

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rot);
            if (p.isCircle) {
                canvas.drawCircle(0, 0, p.size / 2f, paint);
            } else {
                canvas.drawRect(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f, paint);
            }
            canvas.restore();
        }
    }

    /**
     * Overlay confetti on the given parent ViewGroup, animate for ~1.5s, then call onDone.
     */
    public static void burst(ViewGroup parent, Runnable onDone) {
        Context ctx = parent.getContext();
        ConfettiView view = new ConfettiView(ctx);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        parent.addView(view, lp);

        // Wait until the view is laid out so we know its dimensions
        view.post(() -> {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w == 0) w = parent.getWidth();
            if (h == 0) h = parent.getHeight();
            view.initParticles(w, h);

            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(DURATION_MS);
            animator.setInterpolator(new AccelerateInterpolator(0.5f));
            animator.addUpdateListener(a -> {
                view.progress = (float) a.getAnimatedValue();
                view.invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    parent.removeView(view);
                    if (onDone != null) onDone.run();
                }
            });
            animator.start();
        });
    }
}
