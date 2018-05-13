package com.icmdigerati.wybe;

import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

public class PhotoMetadata {

    public PhotoMetadata(LatLng location, UUID guid) {
        Location = location;
        Guid = guid;
    }

    public LatLng Location;
    public UUID Guid;
}
