package com.kindaworks.rsfactory.resource;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

public record ResourceChangeGranularityKey(PlatformResourceKey resourceKey, short sign, Integer granularity) {

}
