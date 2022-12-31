package com.snapyr.sdk.notifications;

import android.os.Parcel;
import android.os.Parcelable.Creator;

final class GoogleZzcParcelableCreator implements Creator<GoogleZzaParcelable> {
    GoogleZzcParcelableCreator() {
    }

    /**
     * Create a new instance of the Parcelable class, instantiating it
     * from the given Parcel whose data had previously been written by
     * {@link Parcelable#writeToParcel Parcelable.writeToParcel()}.
     *
     * @param source The Parcel to read the object's data from.
     * @return Returns a new instance of the Parcelable class.
     */
    @Override
    public GoogleZzaParcelable createFromParcel(Parcel source) {
        return null;
    }

    /**
     * Create a new array of the Parcelable class.
     *
     * @param size Size of the array.
     * @return Returns an array of the Parcelable class, with every entry
     * initialized to null.
     */
    @Override
    public GoogleZzaParcelable[] newArray(int size) {
        return new GoogleZzaParcelable[0];
    }
}
