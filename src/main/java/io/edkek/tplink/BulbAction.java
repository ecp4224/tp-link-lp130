package io.edkek.tplink;

import com.google.gson.Gson;

import java.util.HashMap;

public class BulbAction {
    public static final Gson GSON = new Gson();

    private HashMap<String, Action> parameters = new HashMap<>();

    public void addParameter(Action action) {
        parameters.put("smartlife.iot.smartbulb.lightingservice." + action.getParameterName(), action);
    }

    public String toJson() {
        return GSON.toJson(parameters, BulbAction.class);
    }

    public static interface Action {
        String getParameterName();
    }

    public static class Transition implements Action {
        protected int ignore_default = 0;
        protected String mode = "normal";
        protected int transition_period = 0;

        public void setIgnoreDefault(int ignore_default) {
            this.ignore_default = ignore_default;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setTransitionPeriod(int transition_period) {
            this.transition_period = transition_period;
        }

        @Override
        public String getParameterName() {
            return "transition_light_state";
        }
    }

    public static class OnOff extends Transition {
        protected boolean on_off = false;

        public OnOff(boolean on_off) {
            this.on_off = on_off;
        }

        public boolean isOn() {
            return on_off;
        }

        public void setOn(boolean on_off) {
            this.on_off = on_off;
        }
    }

    public static class On extends OnOff {
        public On() {
            super(true);
        }
    }

    public static class Off extends OnOff {
        public Off() {
            super(false);
        }
    }

    public static class Brightness extends On {
        public int brightness = 100;

        public Brightness(int brightness) {
            super();
        }

        public int getBrightness() {
            return brightness;
        }

        public void setBrightness(int brightness) {
            this.brightness = brightness;
        }
    }

    public static class Color extends Brightness {
        private int color_temp = 0;
        private int hue = 0;
        private int saturation = 0;


        public Color(int brightness, int hue, int saturation) {
            super(brightness);
            this.ignore_default = 1;
            this.hue = hue;
            this.saturation = saturation;
        }

        public int getColorTemp() {
            return color_temp;
        }

        public void setColorTemp(int color_temp) {
            this.color_temp = color_temp;
        }

        public int getHue() {
            return hue;
        }

        public void setHue(int hue) {
            this.hue = hue;
        }

        public int getSaturation() {
            return saturation;
        }

        public void setSaturation(int saturation) {
            this.saturation = saturation;
        }
    }
}
