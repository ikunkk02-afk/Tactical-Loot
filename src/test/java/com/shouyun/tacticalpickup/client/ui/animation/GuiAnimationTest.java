package com.shouyun.tacticalpickup.client.ui.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuiAnimationTest {
	private static final long START = 1_000_000_000L;

	@Test
	void easingFunctionsClampAndReachExactEndpoints() {
		for (Easing easing : Easing.values()) {
			assertEquals(0.0F, easing.apply(-1.0F), 0.0001F, easing.name());
			assertEquals(1.0F, easing.apply(2.0F), 0.0001F, easing.name());
		}
		assertTrue(Easing.OUT_BACK.apply(0.7F) < 1.08F, "restrained back easing");
	}

	@Test
	void elapsedTimeProducesTheSameValueAtDifferentFrameRates() {
		long sampleTime = START + 75L * GuiAnimation.NANOS_PER_MILLISECOND;
		float direct = GuiAnimation.easedProgress(sampleTime, START, 200, Easing.OUT_CUBIC);

		float atThirtyFps = sampleAtFrameRate(30, sampleTime);
		float atTwoHundredFortyFps = sampleAtFrameRate(240, sampleTime);
		assertEquals(direct, atThirtyFps, 0.0001F);
		assertEquals(direct, atTwoHundredFortyFps, 0.0001F);
	}

	@Test
	void delayedProgressStaysAtZeroUntilItsSectionStarts() {
		assertEquals(0.0F, GuiAnimation.delayedProgress(
			START + 29L * GuiAnimation.NANOS_PER_MILLISECOND,
			START,
			30,
			110,
			Easing.OUT_CUBIC
		));
		assertTrue(GuiAnimation.delayedProgress(
			START + 80L * GuiAnimation.NANOS_PER_MILLISECOND,
			START,
			30,
			110,
			Easing.OUT_CUBIC
		) > 0.0F);
	}

	private static float sampleAtFrameRate(int framesPerSecond, long sampleTime) {
		long frameStep = 1_000_000_000L / framesPerSecond;
		long frame = START;
		while (frame + frameStep < sampleTime) {
			frame += frameStep;
			GuiAnimation.easedProgress(frame, START, 200, Easing.OUT_CUBIC);
		}
		return GuiAnimation.easedProgress(sampleTime, START, 200, Easing.OUT_CUBIC);
	}
}
