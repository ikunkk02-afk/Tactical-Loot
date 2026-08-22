package com.shouyun.tacticalpickup.client.ui.animation;

public final class AnimatedFloat {
	private float startValue;
	private float targetValue;
	private long startNanos;
	private int durationMillis;
	private Easing easing = Easing.LINEAR;

	public AnimatedFloat(float initialValue) {
		startValue = initialValue;
		targetValue = initialValue;
	}

	public float value(long nowNanos) {
		float progress = GuiAnimation.easedProgress(nowNanos, startNanos, durationMillis, easing);
		return GuiAnimation.lerp(startValue, targetValue, progress);
	}

	public void setTarget(float target, int duration, Easing nextEasing, long nowNanos) {
		if (Float.compare(target, targetValue) == 0) {
			return;
		}

		startValue = value(nowNanos);
		targetValue = target;
		startNanos = nowNanos;
		durationMillis = duration;
		easing = nextEasing;
	}

	public void snap(float value, long nowNanos) {
		startValue = value;
		targetValue = value;
		startNanos = nowNanos;
		durationMillis = 0;
		easing = Easing.LINEAR;
	}
}
