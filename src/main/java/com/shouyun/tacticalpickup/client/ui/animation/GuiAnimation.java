package com.shouyun.tacticalpickup.client.ui.animation;

public final class GuiAnimation {
	public static final long NANOS_PER_MILLISECOND = 1_000_000L;

	private GuiAnimation() {
	}

	public static float progress(long nowNanos, long startNanos, int durationMillis) {
		if (durationMillis <= 0) {
			return 1.0F;
		}

		long elapsed = Math.max(0L, nowNanos - startNanos);
		return Easing.clamp((float) (elapsed / (durationMillis * (double) NANOS_PER_MILLISECOND)));
	}

	public static float easedProgress(
			long nowNanos,
			long startNanos,
			int durationMillis,
			Easing easing
	) {
		return easing.apply(progress(nowNanos, startNanos, durationMillis));
	}

	public static float delayedProgress(
			long nowNanos,
			long startNanos,
			int delayMillis,
			int durationMillis,
			Easing easing
	) {
		return easedProgress(
			nowNanos,
			startNanos + delayMillis * NANOS_PER_MILLISECOND,
			durationMillis,
			easing
		);
	}

	public static float lerp(float start, float end, float progress) {
		return start + (end - start) * progress;
	}

	public static int lerpRounded(int start, int end, float progress) {
		return Math.round(lerp(start, end, progress));
	}

	public static int multiplyAlpha(int color, float opacity) {
		int alpha = color >>> 24;
		int adjustedAlpha = Math.round(alpha * Easing.clamp(opacity));
		return color & 0x00FFFFFF | adjustedAlpha << 24;
	}
}
