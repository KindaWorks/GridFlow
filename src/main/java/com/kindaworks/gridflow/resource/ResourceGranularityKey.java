package com.kindaworks.gridflow.resource;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

public record ResourceGranularityKey(PlatformResourceKey resourceKey, short sign, Integer granularity) {

}
