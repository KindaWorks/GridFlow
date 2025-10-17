package com.kindaworks.rsfactory.client.gui.side_buttons;


import net.minecraft.resources.ResourceLocation;

import java.time.temporal.ChronoUnit;

public enum Granularity {
    //        TICK {
//            public final int tickAmount = 1;
//            @Override public final String str() {return "t";}
//            @Override protected String getSpritePath() {return super.getSpritePath()+"tick";}
//        },
    SECOND {
        public final ChronoUnit getChronoUnit() {
            return ChronoUnit.SECONDS;
        }

        public final int getTickAmount() {
            return 20;
        }

        public final String str() {
            return "s";
        }

        protected String getSpritePath() {
            return super.getSpritePath() + "second";
        }
    },
    MINUTE {
        public final ChronoUnit getChronoUnit() {
            return ChronoUnit.MINUTES;
        }

        public final int getTickAmount() {
            return 20 * 60;
        }

        public final String str() {
            return "m";
        }

        protected String getSpritePath() {
            return super.getSpritePath() + "minute";
        }
    },
    HOUR {
        public final ChronoUnit getChronoUnit() {
            return ChronoUnit.HOURS;
        }

        public final int getTickAmount() {
            return 20 * 60 * 60;
        }

        public final String str() {
            return "h";
        }

        protected String getSpritePath() {
            return super.getSpritePath() + "hour";
        }
    },
    DAY {
        public final ChronoUnit getChronoUnit() {
            return ChronoUnit.DAYS;
        }

        public final int getTickAmount() {
            return 20 * 60 * 60 * 24;
        }

        public final String str() {
            return "d";
        }

        protected String getSpritePath() {
            return super.getSpritePath() + "day";
        }
    };

    public static Granularity next(Granularity current) {
        return Granularity.values()[(current.ordinal() + 1) % Granularity.values().length];
    }

    public static Granularity prev(Granularity current) {
        return Granularity.values()[(current.ordinal() - 1 + Granularity.values().length) % Granularity.values().length];
    }

    public static double getPerTick(Granularity from, double amount) {
        return amount / from.getTickAmount();
    }

    public ChronoUnit getChronoUnit() {
        return ChronoUnit.MICROS;
    }

    public int getTickAmount() {
        return 0;
    }

    protected String str() {
        return "t";
    }

    public String perStr() {
        return "/" + this.str();
    }

    protected String getSpritePath() {
        return "widget/side_button/granularity/";
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("rsfactory", this.getSpritePath());
    }

    public double convertFrom(Granularity from, double amount) {
        return getPerTick(from, amount) * this.getTickAmount();
    }
}
