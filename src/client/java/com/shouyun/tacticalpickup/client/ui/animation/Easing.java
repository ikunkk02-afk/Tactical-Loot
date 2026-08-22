package com.shouyun.tacticalpickup.client.ui.animation;

public enum Easing {
	LINEAR {
		@Override
		public float apply(float value) {
			return clamp(value);
		}
	},
	OUT_CUBIC {
		@Override
		public float apply(float value) {
			float t = clamp(value);
			float inverse = 1.0F - t;
			return 1.0F - inverse * inverse * inverse;
		}
	},
	IN_CUBIC {
		@Override
		public float apply(float value) {
			float t = clamp(value);
			return t * t * t;
		}
	},
	IN_OUT_CUBIC {
		@Override
		public float apply(float value) {
			float t = clamp(value);
			return t < 0.5F
				? 4.0F * t * t * t
				: 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0D) / 2.0F;
		}
	},
	OUT_QUART {
		@Override
		public float apply(float value) {
			float t = clamp(value);
			float inverse = 1.0F - t;
			return 1.0F - inverse * inverse * inverse * inverse;
		}
	},
	OUT_BACK {
		@Override
		public float apply(float value) {
			float t = clamp(value);
			float shifted = t - 1.0F;
			float back = 0.65F;
			return 1.0F + (back + 1.0F) * shifted * shifted * shifted + back * shifted * shifted;
		}
	};

	public abstract float apply(float value);

	public static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
