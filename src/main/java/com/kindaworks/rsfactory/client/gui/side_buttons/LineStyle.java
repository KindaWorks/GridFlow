package com.kindaworks.rsfactory.client.gui.side_buttons;


import net.minecraft.resources.ResourceLocation;

public enum LineStyle {
    BLOCKY {
        @Override
        protected String getSpritePath() {
            return super.getSpritePath() + "blocky";
        }
    },
    EXACT {
        @Override
        protected String getSpritePath() {
            return super.getSpritePath() + "exact";
        }
    };

    public static LineStyle next(LineStyle current) {
        return LineStyle.values()[(current.ordinal() + 1) % LineStyle.values().length];
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("rsfactory", this.getSpritePath());
    }

    protected String getSpritePath() {
        return "widget/side_button/line_style/";
    }
}
